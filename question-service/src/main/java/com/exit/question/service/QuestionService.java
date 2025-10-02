package com.exit.question.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.common.grpc.ai.SaveQuestionRequest;
import com.exit.common.grpc.ai.SimilarQuestion;
import com.exit.common.grpc.ai.SimilarResponse;
import com.exit.common.util.file.FileUploadUtil;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.controller.dto.request.*;
import com.exit.question.controller.dto.response.*;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.QuestionCategory;
import com.exit.question.domain.question.QuestionImage;
import com.exit.question.domain.question.QuestionReport;
import com.exit.question.domain.question.repository.QuestionCategoryRepository;
import com.exit.question.domain.question.repository.QuestionImageRepository;
import com.exit.question.domain.question.repository.QuestionReportRepository;
import com.exit.question.domain.question.repository.QuestionRepository;
import com.exit.question.domain.question.repository.FollowUpRoomRepository;
import com.exit.question.domain.response.Response;
import com.exit.question.domain.response.ResponseImage;
import com.exit.question.domain.response.ResponseLike;
import com.exit.question.domain.response.ResponseReport;
import com.exit.question.domain.response.repository.ResponseImageRepository;
import com.exit.question.domain.response.repository.ResponseLikeRepository;
import com.exit.question.domain.response.repository.ResponseReportRepository;
import com.exit.question.domain.response.repository.ResponseRepository;
import com.exit.question.exception.GrpcQuestionErrorCode;
import com.exit.question.exception.GrpcResponseErrorCode;
import com.exit.question.service.client.AiGrpcClient;
import com.exit.question.service.client.NotificationGrpcClient;
import com.exit.question.service.client.UserGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QuestionService {
    private static final String QUESTION_FOLDER = "question";
    private static final String RESPONSE_FOLDER = "response";
    private final QuestionRepository questionRepository;
    private final QuestionCategoryRepository questionCategoryRepository;
    private final QuestionImageRepository questionImageRepository;
    private final QuestionReportRepository questionReportRepository;
    private final ResponseRepository responseRepository;
    private final ResponseImageRepository responseImageRepository;
    private final ResponseLikeRepository responseLikeRepository;
    private final ResponseReportRepository responseReportRepository;
    private final FollowUpRoomRepository followUpRoomRepository;
    private final FileUploadUtil fileUploadUtil;
    private final UserGrpcClient userGrpcClient;
    private final NotificationGrpcClient notificationGrpcClient;
    private final AiGrpcClient aiGrpcClient;
    private final TaskScheduler taskScheduler;

    public QuestionCreateResponseDto createQuestion(QuestionCreateRequestDto request) {
        QuestionCategory questionCategory = questionCategoryRepository.findById(request.questionCategoryId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));
        Question question = Question.createQuestionFromRequest(request, questionCategory);

        Question savedQuestion = questionRepository.save(question);

        List<String> imageUrls = uploadQuestionImages(request, savedQuestion);
        String questionWriterName = userGrpcClient.getUserName(question.getQuestionWriterId());

        aiGrpcClient.saveQuestion(createSaveQuestionToVectorDBRequest(question));
        // AI 답변 자동 생성
        scheduleAiAnswerGeneration(savedQuestion);

        return QuestionCreateResponseDto.from(savedQuestion, imageUrls, questionWriterName);
    }

    private SaveQuestionRequest createSaveQuestionToVectorDBRequest(Question question) {
        return SaveQuestionRequest.newBuilder()
                .setQuestionId(question.getQuestionId())
                .setTitle(question.getQuestionTitle())
                .setContent(question.getQuestionContent())
                .setCategoryId(question.getQuestionCategory().getQuestionCategoryId().intValue())
                .build();
    }

    /**
     * AI 답변 생성 스케줄링
     * 긴급 질문: 즉시 생성
     * 일반 질문: 5분 후 생성
     */
    private void scheduleAiAnswerGeneration(Question question) {
        if (question.getQuestionUrgency()) {
            // 긴급 질문은 즉시 생성
            generateAiAnswerAsync(question);
        } else {
            // 일반 질문은 5분 후 생성
            Instant scheduledTime = Instant.now().plus(Duration.ofMinutes(5));
            taskScheduler.schedule(() -> generateAiAnswerAsync(question), scheduledTime);
        }
    }

    /**
     * 질문 생성 시 AI 답변을 자동으로 생성하여 저장
     * AI 생성 실패 시 최대 3회 재시도 (지수 백오프)
     * 모든 재시도 실패 시에도 질문 생성은 정상 처리됨
     */
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            recover = "recoverGenerateAiAnswer"
    )
    public void generateAiAnswerAsync(Question question) {
        // 질문 제목과 내용을 결합
        String fullQuestion = String.format("제목: %s\n\n내용: %s",
                question.getQuestionTitle(),
                question.getQuestionContent());

        // AI 답변 생성 요청
        String aiAnswer = aiGrpcClient.generateAiAnswer(fullQuestion, question.getQuestionId());

        // AI 답변을 Response로 저장
        Response aiResponse = Response.builder()
                .questionId(question.getQuestionId())
                .responseWriterId(1L) // AI 시스템 계정 ID
                .responseContent(aiAnswer)
                .responseAdopt(false)
                .responseIsAnonymous(false)
                .build();

        responseRepository.save(aiResponse);
        log.info("AI answer generated and saved for question ID: {}", question.getQuestionId());
    }

    /**
     * AI 답변 생성 재시도 실패 시 폴백 메서드
     */
    @Recover
    private void recoverGenerateAiAnswer(Exception e, Question question) {
        log.error("Failed to generate AI answer after all retry attempts for question {}: {}",
                question.getQuestionId(), e.getMessage(), e);
        // TODO: 필요시 사용자에게 알림 전송 또는 재시도 큐에 추가
    }

    private List<String> uploadQuestionImages(QuestionCreateRequestDto questionCreateRequestDto, Question savedQuestion) {
        List<String> imageUrls = null;
        if (questionCreateRequestDto.images() != null) {
            imageUrls = fileUploadUtil.uploadImages(questionCreateRequestDto.images(), QUESTION_FOLDER);
            List<QuestionImage> questionImages = imageUrls.stream()
                    .map(url -> QuestionImage.builder()
                            .questionId(savedQuestion.getQuestionId())
                            .questionImageUrl(url)
                            .build())
                    .toList();

            questionImageRepository.saveAll(questionImages);
        }
        return imageUrls;
    }

    public AnswerAdoptResponseDto answerAdopt(AnswerAdoptRequestDto answerAdoptRequestDto) {
        Response response = responseRepository.findById(answerAdoptRequestDto.responseId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));

        validateQuestionNotAlreadyAdopted(response.getQuestionId());

        Response adoptedResponse = adoptResponse(response);
        Question question = questionRepository.findById(adoptedResponse.getQuestionId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));
        updateResponseAdopt(question);
        String body = truncateContent(adoptedResponse.getResponseContent());
        SendNotificationRequestDto requestDto = SendNotificationRequestDto.builder()
                .body(body)
                .type("ANSWER_ADOPTED")
                .targetId(question.getQuestionId())
                .receiverId(question.getQuestionWriterId())
                .deviceId(answerAdoptRequestDto.deviceId())
                .build();
        notificationGrpcClient.sendNotification(requestDto);
        return AnswerAdoptResponseDto.from(adoptedResponse);
    }

    private void updateResponseAdopt(Question question) {
        question.updateAnswerAdopt();
        questionRepository.save(question);
    }

    public AnswerCreateResponseDto answerCreate(AnswerCreateRequestDto answerCreateRequestDto) {
        Response newResponse = Response.createResponse(answerCreateRequestDto);
        Response savedResponse = responseRepository.save(newResponse);

        List<String> imageUrls = processAnswerImages(answerCreateRequestDto, savedResponse);
        Question question = questionRepository.findById(savedResponse.getQuestionId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        String subBody = truncateContent(savedResponse.getResponseContent());
        SendNotificationRequestDto requestDto = SendNotificationRequestDto.builder()
                .body(subBody)
                .type("NEW_ANSWER_ON_QUESTION")
                .targetId(question.getQuestionId())
                .receiverId(question.getQuestionWriterId())
                .deviceId(answerCreateRequestDto.deviceId())
                .build();
        notificationGrpcClient.sendNotification(requestDto);

        return AnswerCreateResponseDto.from(savedResponse, imageUrls);
    }

    private String truncateContent(String content) {
        String subBody;
        if(content.length() <= 100) {
            subBody = content.substring(0, content.length()-1);
        } else {
            subBody = content.substring(0, 100);
        }
        return subBody;
    }

    public AnswerRecommendResponseDto toggleAnswerLike(AnswerRecommendRequestDto req) {
        Optional<ResponseLike> existingLike =
                responseLikeRepository.findByResponseIdAndUserId(req.responseId(), req.userId());

        boolean isLiked = handleLikeToggle(existingLike, req);
        int likeCount = responseLikeRepository.countByResponseId(req.responseId());

        return new AnswerRecommendResponseDto(likeCount, isLiked);
    }

    public QuestionReportResponseDto questionReport(QuestionReportRequestDto request) {
        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        QuestionReport questionReport = createQuestionReport(request);
        userGrpcClient.increaseReportCount(question.getQuestionWriterId());

        QuestionReport savedQuestionReport = questionReportRepository.save(questionReport);
        return QuestionReportResponseDto.from(savedQuestionReport);
    }

    private QuestionReport createQuestionReport(QuestionReportRequestDto request) {
        return QuestionReport.builder()
                .questionId(request.questionId())
                .questionReportTitle(request.questionReportTitle())
                .questionReportContent(request.questionReportContent())
                .questionReportWriterId(request.questionReportWriterId())
                .build();
    }

    public AnswerReportResponseDto answerReport(AnswerReportRequestDto request) {
        Response response = responseRepository.findById(request.responseId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));
        ResponseReport responseReport = ResponseReport.from(request);

        ResponseReport savedResponseReport = responseReportRepository.save(responseReport);
        userGrpcClient.increaseReportCount(response.getResponseWriterId());

        return AnswerReportResponseDto.from(savedResponseReport);
    }

    @Transactional(readOnly = true)
    public QuestionListResponseDto questionList(QuestionListRequestDto filter) {
        PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize());
        Slice<QuestionListQueryResponseDto> slice = questionRepository.findQuestionsByFilter(filter.getCategoryIds(), filter.getKeyword(), pageRequest);
        return new QuestionListResponseDto(slice.getContent(), slice.hasNext());
    }


    // 카테고리 추천
    @Transactional(readOnly = true)
    public CategoryRecommendationResponse categoryRecommend(String title) {
        Long categoryRecommend = aiGrpcClient.categoryRecommend(title).longValue();
        QuestionCategory questionCategory = questionCategoryRepository.findById(categoryRecommend)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION_CATEGORY));

        return CategoryRecommendationResponse.newBuilder()
                .setCategoryId(questionCategory.getQuestionCategoryId())
                .setCategoryName(questionCategory.getQuestionCategoryName())
                .build();
    }

    // 유사 질문 조회
    @Transactional(readOnly = true)
    public SimilarQuestionResponse similarQuestion(String title, String content) {
        SimilarResponse similarResponse = aiGrpcClient.similarQuestion(title, content);
        if(similarResponse.getQuestionsCount() <= 0) {
            return null;
        }

        List<Long> similarQuestionIds = similarResponse.getQuestionsList().stream().map(SimilarQuestion::getQuestionId).toList();
        List<Question> similarQuestions = questionRepository.findAllById(similarQuestionIds);
        return getSimilarQuestionResponse(similarQuestions);
    }

    @Transactional(readOnly = true)
    public QuestionDetailResponseDto getQuestionDetail(Long questionId) {
        QuestionCreateResponseDto questionDto = buildQuestionDto(questionId);
        List<ResponseDetailDto> responseDetailDtos = buildResponseDetailDtos(questionId);
        boolean hasMore = hasMoreResponses(questionId);

        return new QuestionDetailResponseDto(questionDto, responseDetailDtos, hasMore);
    }

    private QuestionCreateResponseDto buildQuestionDto(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        Optional<List<QuestionImage>> images = questionImageRepository.findAllByQuestionId(questionId);
        List<String> questionUrls = new ArrayList<>();
        images.ifPresent(questionImages ->
                questionImages.forEach(image -> questionUrls.add(image.getQuestionImageUrl())));
        String questionWriterName = userGrpcClient.getUserName(question.getQuestionWriterId());
        QuestionCreateResponseDto questionDto = QuestionCreateResponseDto.from(question, questionUrls, questionWriterName);
        return questionDto;
    }

    private List<ResponseDetailDto> buildResponseDetailDtos(Long questionId) {
        PageRequest pageRequest = PageRequest.of(0, 5);
        Slice<Response> responseSlice = responseRepository.findAllByQuestionId(questionId, pageRequest);

        List<Response> responses = responseSlice.getContent();
        if (responses.isEmpty()) {
            return Collections.emptyList();
        }

        // 배치로 필요한 데이터 미리 조회 (N+1 문제 해결)
        Map<Long, List<String>> responseImageUrlsMap = getResponseImageUrlsMap(responses);
        Map<Long, Integer> likeCountMap = getLikeCountMap(responses);
        Map<Long, String> writerNameMap = getWriterNameMap(responses);

        return responses.stream()
                .map(response -> ResponseDetailDto.from(
                        response,
                        responseImageUrlsMap.getOrDefault(response.getResponseId(), Collections.emptyList()),
                        likeCountMap.getOrDefault(response.getResponseId(), 0),
                        writerNameMap.getOrDefault(response.getResponseWriterId(), "Unknown")
                ))
                .toList();
    }

    private Map<Long, List<String>> getResponseImageUrlsMap(List<Response> responses) {
        List<Long> responseIds = responses.stream()
                .map(Response::getResponseId)
                .toList();

        return responseImageRepository.findAllByResponseIdIn(responseIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ResponseImage::getResponseId,
                        Collectors.mapping(ResponseImage::getResponseImageUrl, Collectors.toList())
                ));
    }

    private Map<Long, Integer> getLikeCountMap(List<Response> responses) {
        List<Long> responseIds = responses.stream()
                .map(Response::getResponseId)
                .toList();

        return responseLikeRepository.countByResponseIdIn(responseIds);
    }

    private Map<Long, String> getWriterNameMap(List<Response> responses) {
        Set<Long> writerIds = responses.stream()
                .map(Response::getResponseWriterId)
                .collect(toSet());

        List<UserIdAndNameInfo> userInfos = userGrpcClient.getUserNames(new ArrayList<>(writerIds));
        return userInfos.stream()
                .collect(toMap(
                        UserIdAndNameInfo::getUserId,
                        UserIdAndNameInfo::getUserName
                ));
    }

    private boolean hasMoreResponses(Long questionId) {
        PageRequest pageRequest = PageRequest.of(0, 5);
        return responseRepository.findAllByQuestionId(questionId, pageRequest).hasNext();
    }

    private void validateQuestionNotAlreadyAdopted(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        if (question.getQuestionAnswerAdopt()) {
            throw new GrpcException(GrpcQuestionErrorCode.EXIST_ADOPTED_RESPONSE);
        }
    }

    private Response adoptResponse(Response response) {
        Response updateResponseAdopt = response.updateResponseAdopt();
        Response savedResponse = responseRepository.save(updateResponseAdopt);
        return savedResponse;
    }

    private List<String> processAnswerImages(AnswerCreateRequestDto answerCreateRequestDto, Response savedResponse) {
        List<String> imageUrls = null;
        if (answerCreateRequestDto.images() != null) {
            imageUrls = fileUploadUtil.uploadImages(answerCreateRequestDto.images(), RESPONSE_FOLDER);
            for (String imageUrl : imageUrls) {
                ResponseImage responseImage = ResponseImage.builder()
                        .responseId(savedResponse.getResponseId())
                        .responseImageUrl(imageUrl)
                        .build();
                responseImageRepository.save(responseImage);
            }
        }
        return imageUrls;
    }

    private boolean handleLikeToggle(Optional<ResponseLike> existingLike, AnswerRecommendRequestDto req) {
        if (existingLike.isPresent()) {
            responseLikeRepository.delete(existingLike.get());
            return false; // 좋아요 취소됨
        }

        ResponseLike newLike = createNewLike(req);
        responseLikeRepository.save(newLike);
        return true; // 새로운 좋아요
    }

    private ResponseLike createNewLike(AnswerRecommendRequestDto req) {
        return ResponseLike.builder()
                .responseId(req.responseId())
                .userId(req.userId())
                .build();
    }

    public UpdateResponseResponse updateResponse(UpdateResponseRequest request) {
        Response response = responseRepository.findById(request.getResponseId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));

        if(Boolean.TRUE.equals(response.getResponseAdopt()))
            throw new GrpcException(GrpcResponseErrorCode.ALREADY_RESPONSE_ADOPTED);

        response.updateContent(request.getContent());
        responseRepository.save(response);

        return UpdateResponseResponse.newBuilder()
                .setResponseId(response.getResponseId())
                .setContent(response.getResponseContent())
                .build();
    }

    public void deleteResponse(DeleteResponseRequest request) {
        Response response = responseRepository.findById(request.getResponseId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));

        if(Boolean.TRUE.equals(response.getResponseAdopt()))
            throw new GrpcException(GrpcResponseErrorCode.ALREADY_RESPONSE_ADOPTED);

        // FollowUpRoom이 있다면 먼저 삭제
        followUpRoomRepository.findByResponse(response)
                .ifPresent(followUpRoomRepository::delete);

        responseRepository.delete(response);
    }

    private SimilarQuestionItem createSimilarQuestion(Question question) {
        return SimilarQuestionItem.newBuilder()
                .setQuestionId(question.getQuestionId())
                .setQuestionTitle(question.getQuestionTitle())
                .setQuestionContent(question.getQuestionContent())
                .setQuestionCategory(question.getQuestionCategory().getQuestionCategoryId())
                .setQuestionUrgency(question.getQuestionUrgency())
                .setQuestionAnswerType(question.getQuestionAnswerType().name())
                .setQuestionAnswerAdopt(question.getQuestionAnswerAdopt())
                .setCreatedAt(TimeStampUtil.toGrpcTimestamp(question.getCreatedAt()))
                .build();
    }

    public SimilarQuestionResponse getSimilarQuestionResponse(List<Question> questions) {
        List<SimilarQuestionItem> similarQuestionItems = questions.stream().map(this::createSimilarQuestion).toList();
        return SimilarQuestionResponse.newBuilder()
                .addAllSimilarQuestions(similarQuestionItems)
                .build();
    }
}
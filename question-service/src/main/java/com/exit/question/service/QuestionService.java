package com.exit.question.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.common.grpc.ai.SaveQuestionRequest;
import com.exit.common.grpc.ai.SimilarQuestion;
import com.exit.common.grpc.ai.SimilarResponse;
import com.exit.common.util.file.FileUploadUtil;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.controller.dto.response.PopularPostDto;
import com.exit.question.controller.dto.response.QuestionListQueryResponseDto;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.QuestionCategory;
import com.exit.question.domain.question.QuestionImage;
import com.exit.question.domain.question.QuestionReport;
import com.exit.question.domain.question.repository.QuestionCategoryRepository;
import com.exit.question.domain.question.repository.QuestionImageRepository;
import com.exit.question.domain.question.repository.QuestionReportRepository;
import com.exit.question.domain.question.repository.QuestionRepository;
import com.exit.question.domain.response.Response;
import com.exit.question.domain.response.ResponseImage;
import com.exit.question.domain.response.repository.ResponseImageRepository;
import com.exit.question.domain.response.repository.ResponseLikeRepository;
import com.exit.question.domain.response.repository.ResponseRepository;
import com.exit.question.exception.GrpcQuestionErrorCode;
import com.exit.question.service.client.AiGrpcClient;
import com.exit.question.service.client.NotificationGrpcClient;
import com.exit.question.service.client.UserGrpcClient;
import com.exit.question.service.util.QuestionGrpcMapper;
import com.google.protobuf.Empty;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QuestionService {
    private static final String QUESTION_FOLDER = "question";
    private final QuestionRepository questionRepository;
    private final QuestionCategoryRepository questionCategoryRepository;
    private final QuestionImageRepository questionImageRepository;
    private final QuestionReportRepository questionReportRepository;
    private final ResponseRepository responseRepository;
    private final ResponseImageRepository responseImageRepository;
    private final ResponseLikeRepository responseLikeRepository;
    private final FileUploadUtil fileUploadUtil;
    private final UserGrpcClient userGrpcClient;
    private final NotificationGrpcClient notificationGrpcClient;
    private final AiGrpcClient aiGrpcClient;
    private final TaskScheduler taskScheduler;
    private final QuestionGrpcMapper questionGrpcMapper;

    public QuestionCreateResponse createQuestion(QuestionCreateRequest request) {
        QuestionCategory questionCategory = questionCategoryRepository.findById(request.getQuestionCategory())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));
        Question question = Question.createQuestionFromRequest(request, questionCategory);
        Question savedQuestion = questionRepository.save(question);

        List<String> imageUrls = uploadQuestionImages(request, savedQuestion);
        String questionWriterName = userGrpcClient.getUserName(question.getQuestionWriterId());

        aiGrpcClient.saveQuestion(createSaveQuestionToVectorDBRequest(question));
        // AI 답변 자동 생성
        scheduleAiAnswerGeneration(savedQuestion);

        return questionGrpcMapper.getQuestionCreateResponse(savedQuestion, imageUrls, questionWriterName);
    }

    public QuestionReportResponse questionReport(QuestionReportRequest request) {
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        QuestionReport questionReport = QuestionReport.from(request);
        userGrpcClient.increaseReportCount(question.getQuestionWriterId());

        QuestionReport savedQuestionReport = questionReportRepository.save(questionReport);
        return questionGrpcMapper.getQuestionReportResponse(savedQuestionReport);
    }

    @Transactional(readOnly = true)
    public QuestionListResponse questionList(QuestionListRequest filter) {
        PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize());
        Slice<QuestionListQueryResponseDto> slice = questionRepository.findQuestionsByFilter(filter.getCategoryIdsList(), filter.getKeyword(), pageRequest);
        return questionGrpcMapper.getQuestionListResponse(slice.getContent(), slice.hasNext());
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
        if (similarResponse.getQuestionsCount() <= 0) {
            return null;
        }

        List<Long> similarQuestionIds = similarResponse.getQuestionsList().stream().map(SimilarQuestion::getQuestionId).toList();
        List<Question> similarQuestions = questionRepository.findAllById(similarQuestionIds);
        return questionGrpcMapper.getSimilarQuestionResponse(similarQuestions);
    }

    @Transactional(readOnly = true)
    public QuestionDetailResponse getQuestionDetail(Long questionId) {
        QuestionCreateResponse questionCreateResponse = buildQuestionCreateResponse(questionId);
        List<ResponseDetail> responseDetails = buildResponseDetail(questionId);
        boolean hasMore = hasMoreResponses(questionId);

        return questionGrpcMapper.getQuestionDetailResponse(questionCreateResponse, responseDetails, hasMore);
    }

    private QuestionCreateResponse buildQuestionCreateResponse(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        Optional<List<QuestionImage>> images = questionImageRepository.findAllByQuestionId(questionId);
        List<String> questionUrls = new ArrayList<>();
        images.ifPresent(questionImages ->
                questionImages.forEach(image -> questionUrls.add(image.getQuestionImageUrl())));
        String questionWriterName = userGrpcClient.getUserName(question.getQuestionWriterId());

        return questionGrpcMapper.getQuestionCreateResponse(question, questionUrls, questionWriterName);
    }

    private List<ResponseDetail> buildResponseDetail(Long questionId) {
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
                .map(response -> questionGrpcMapper.getResponseDetail(
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
        if (Boolean.TRUE.equals(question.getQuestionUrgency())) {
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
    private void generateAiAnswerAsync(Question question) {
        // AI 답변 생성 요청
        String aiAnswer = aiGrpcClient.generateAiAnswer(question.getQuestionId());

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

    private List<String> uploadQuestionImages(QuestionCreateRequest request, Question savedQuestion) {
        List<String> imageUrls = null;
        if (!request.getImagesList().isEmpty()) {
            imageUrls = fileUploadUtil.uploadImages(request.getImagesList(), QUESTION_FOLDER);
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

    @Transactional(readOnly = true)
    public GetPopularPostResponse getPopularPost() {
        List<PopularPostDto> popularPosts = questionRepository.findTop5ByResponseCount();

        if (popularPosts.isEmpty()) {
            return GetPopularPostResponse.newBuilder().build();
        }

        // 모든 작성자 ID 수집
        Set<Long> writerIds = popularPosts.stream()
                .map(PopularPostDto::questionWriterId)
                .collect(toSet());

        // 배치로 사용자 정보 조회
        List<UpdateAdditionalUserInfoResponse> userInfoList = userGrpcClient.getUsersNameAndProfile(new ArrayList<>(writerIds)).getUserInfoList();
        Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap = userInfoList.stream()
                .collect(toMap(UpdateAdditionalUserInfoResponse::getUserId, Function.identity()));

        // PopularPostItem 생성
        List<PopularPostItem> items = popularPosts.stream()
                .map(post -> {
                    UpdateAdditionalUserInfoResponse userInfo = userInfoMap.get(post.questionWriterId());
                    return PopularPostItem.newBuilder()
                            .setCategoryId(post.questionCategoryId())
                            .setProfileUrl(!userInfo.getUserProfile().isEmpty() ? userInfo.getUserProfile() : "")
                            .setNickname(userInfo.getUserName())
                            .setTitle(post.questionTitle())
                            .setContent(post.questionContent())
                            .setAnswerAdopt(post.questionAnswerAdopt())
                            .setAnswerCount(post.answerCount())
                            .setCreatedAt(TimeStampUtil.toGrpcTimestamp(post.createdAt()))
                            .build();
                })
                .toList();

        return GetPopularPostResponse.newBuilder()
                .addAllPost(items)
                .build();
    }
}
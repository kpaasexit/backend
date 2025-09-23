package com.exit.question.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.UserIdAndNameInfo;
import com.exit.common.util.file.FileUploadUtil;
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
import com.exit.question.domain.response.Response;
import com.exit.question.domain.response.ResponseImage;
import com.exit.question.domain.response.ResponseLike;
import com.exit.question.domain.response.ResponseReport;
import com.exit.question.domain.response.repository.ResponseImageRepository;
import com.exit.question.domain.response.repository.ResponseLikeRepository;
import com.exit.question.domain.response.repository.ResponseReportRepository;
import com.exit.question.domain.response.repository.ResponseRepository;
import com.exit.question.exception.GrpcQuestionErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
@Transactional
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
    private final FileUploadUtil fileUploadUtil;
    private final UserGrpcClient userGrpcClient;
//    private final NlpGrpcClient nlpGrpcClient;

    public QuestionCreateResponseDto createQuestion(QuestionCreateRequestDto request) {
        QuestionCategory questionCategory = questionCategoryRepository.findById(request.questionCategoryId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));
        Question question = Question.createQuestionFromRequest(request, questionCategory);

        Question savedQuestion = questionRepository.save(question);

        List<String> imageUrls = uploadQuestionImages(request, savedQuestion);
        String questionWriterName = userGrpcClient.getUserName(question.getQuestionWriterId());

        return QuestionCreateResponseDto.from(savedQuestion, imageUrls, questionWriterName);
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

        return AnswerAdoptResponseDto.from(adoptedResponse);
    }

    public AnswerCreateResponseDto answerCreate(AnswerCreateRequestDto answerCreateRequestDto) {
        Response newResponse = Response.createResponse(answerCreateRequestDto);
        Response savedResponse = responseRepository.save(newResponse);

        List<String> imageUrls = processAnswerImages(answerCreateRequestDto, savedResponse);

        return AnswerCreateResponseDto.from(savedResponse, imageUrls);
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
    public CategoryRecommendationResponseDto categoryRecommend(String title) {

        return null;
    }

    // 유사 질문 조회
    @Transactional(readOnly = true)
    public SimilarQuestionResponseDto similarQuestion(String title) {
        List<Question> questions = questionRepository.findAll();
        if (questions.isEmpty()) {
            return null;
        }
        Question similarQuestion = questions.get(0);

        return new SimilarQuestionResponseDto(
                similarQuestion.getQuestionId(),
                similarQuestion.getQuestionTitle(),
                similarQuestion.getQuestionContent(),
                similarQuestion.getQuestionCategory(),
                similarQuestion.getQuestionUrgency(),
                similarQuestion.getQuestionAnswerType(),
                similarQuestion.getQuestionAnswerAdopt(),
                similarQuestion.getCreatedAt()
        );
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
}
package com.exit.question.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.file.util.FileUploadUtil;
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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionService {
    private static final String QUESTION_FOLDER = "/question";
    private static final String RESPONSE_FOLDER = "/response";
    private final QuestionRepository questionRepository;
    private final QuestionCategoryRepository questionCategoryRepository;
    private final QuestionImageRepository questionImageRepository;
    private final QuestionReportRepository questionReportRepository;
    private final ResponseRepository responseRepository;
    private final ResponseImageRepository responseImageRepository;
    private final ResponseLikeRepository responseLikeRepository;
    private final ResponseReportRepository responseReportRepository;
    private final FileUploadUtil fileUploadUtil;

    public QuestionCreateResponse questionCreate(QuestionCreateRequest questionCreateRequest) {
        Question question = Question.createQuestionFromRequest(questionCreateRequest);

        Question savedQuestion = questionRepository.save(question);

        List<String> imageUrls = null;
        if (questionCreateRequest.images() != null) {
            imageUrls = fileUploadUtil.uploadImages(questionCreateRequest.images(), QUESTION_FOLDER);
            for (String imageUrl : imageUrls) {
                QuestionImage questionImage = QuestionImage.builder()
                        .questionId(savedQuestion.getQuestionId())
                        .questionImageUrl(imageUrl)
                        .build();
                questionImageRepository.save(questionImage);
            }
        }

        return QuestionCreateResponse.from(savedQuestion, imageUrls);
    }

    public AnswerAdoptResponse answerAdopt(AnswerAdoptRequest answerAdoptRequest) {
        Response response = responseRepository.findById(answerAdoptRequest.responseId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));

        Response updateResponseAdopt = response.updateResponseAdopt();
        Response savedResponse = responseRepository.save(updateResponseAdopt);

        return AnswerAdoptResponse.from(savedResponse);
    }

    public AnswerCreateResponse answerCreate(AnswerCreateRequest answerCreateRequest) {
        Response newResponse = Response.createResponse(answerCreateRequest);

        Response savedResponse = responseRepository.save(newResponse);

        List<String> imageUrls = null;
        if (answerCreateRequest.images() != null) {
            imageUrls = fileUploadUtil.uploadImages(answerCreateRequest.images(), RESPONSE_FOLDER);
            for (String imageUrl : imageUrls) {
                ResponseImage responseImage = ResponseImage.builder()
                        .responseId(savedResponse.getResponseId())
                        .responseImageUrl(imageUrl)
                        .build();
                responseImageRepository.save(responseImage);
            }
        }

        return AnswerCreateResponse.from(savedResponse, imageUrls);
    }

    public AnswerRecommendResponse answerRecommend(AnswerRecommendRequest req) {
        Optional<ResponseLike> existing =
                responseLikeRepository.findByResponseIdAndUserId(req.responseId(), req.userId());

        if (existing.isPresent()) {
            // 이미 좋아요 → 취소
            responseLikeRepository.delete(existing.get());
            int count = responseLikeRepository.countByResponseId(req.responseId());
            return new AnswerRecommendResponse(count, false);
        }

        // 새로 좋아요
        ResponseLike like = ResponseLike.builder()
                .responseId(req.responseId())
                .userId(req.userId())
                .build();
        responseLikeRepository.save(like);

        int count = responseLikeRepository.countByResponseId(req.responseId());
        return new AnswerRecommendResponse(count, true);
    }

    public QuestionReportResponse questionReport(QuestionReportRequest questionReportRequest) {
        QuestionReport questionReport = QuestionReport.builder()
                .questionId(questionReportRequest.questionId())
                .questionReportTitle(questionReportRequest.questionReportTitle())
                .questionReportContent(questionReportRequest.questionReportContent())
                .questionReportWriterId(1L)
                .build();

        QuestionReport savedQuestionReport = questionReportRepository.save(questionReport);

        return new QuestionReportResponse(
                savedQuestionReport.getQuestionReportId(),
                savedQuestionReport.getQuestionId(),
                savedQuestionReport.getQuestionReportTitle(),
                savedQuestionReport.getQuestionReportContent(),
                savedQuestionReport.getQuestionReportWriterId(),
                savedQuestionReport.getCreatedAt(),
                savedQuestionReport.getUpdatedAt()
        );
    }

    public AnswerReportResponse answerReport(AnswerReportRequest answerReportRequest) {
        ResponseReport responseReport = ResponseReport.builder()
                .responseId(answerReportRequest.responseId())
                .responseReportTitle(answerReportRequest.responseReportTitle())
                .responseReportContent(answerReportRequest.responseReportContent())
                .responseReportWriterId(1L)
                .build();

        ResponseReport savedResponseReport = responseReportRepository.save(responseReport);

        return new AnswerReportResponse(
                savedResponseReport.getResponseReportId(),
                savedResponseReport.getResponseId(),
                savedResponseReport.getResponseReportTitle(),
                savedResponseReport.getResponseReportContent(),
                savedResponseReport.getResponseReportWriterId(),
                savedResponseReport.getCreatedAt(),
                savedResponseReport.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public QuestionListResponse questionList(QuestionListRequest filter) {
        PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize());
        Slice<QuestionListQueryResponse> slice = questionRepository.findQuestionsByFilter(filter.getCategoryIds(), filter.getKeyword(), pageRequest);
        return new QuestionListResponse(slice.getContent(), slice.hasNext());
    }

    // 카테고리 추천
    public CategoryRecommendationResponse categoryRecommend(String title) {
        // 1. 과거 질문 데이터를 기반으로 유사한 제목의 질문에서 가장 많이 사용된 카테고리 찾기
        QuestionCategory mostUsedCategory = questionCategoryRepository.findMostUsedCategoryByTitlePattern(title);
        if (mostUsedCategory != null) {
            return CategoryRecommendationResponse.from(mostUsedCategory);
        }

        // 2. 제목에서 키워드를 추출하여 카테고리 이름과 매칭
        String[] keywords = title.toLowerCase().split("\\s+");
        for (String keyword : keywords) {
            List<QuestionCategory> matchingCategories = questionCategoryRepository.findCategoriesByKeyword(keyword);
            if (!matchingCategories.isEmpty()) {
                QuestionCategory matchedCategory = matchingCategories.get(0);
                return CategoryRecommendationResponse.from(matchedCategory);
            }
        }

        // 3. 기본값: 첫 번째 카테고리 반환
        // TODO: 더 정교한 카테고리 추천을 위해서는 AI/ML 기술이 필요
        // - 자연어 처리(NLP)를 통한 의미적 유사도 계산
        // - 머신러닝 모델을 활용한 카테고리 분류
        // - 벡터 임베딩 기반 유사도 매칭
        List<QuestionCategory> allCategories = questionCategoryRepository.findAll();
        if (allCategories.isEmpty()) {
            return null;
        }

        QuestionCategory defaultCategory = allCategories.get(0);
        return CategoryRecommendationResponse.from(defaultCategory);
    }

    // 유사 질문 조회
    public SimilarQuestionResponse similarQuestion(String title) {
        List<Question> questions = questionRepository.findAll();
        if (questions.isEmpty()) {
            return null;
        }
        Question similarQuestion = questions.get(0);

        return new SimilarQuestionResponse(
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
}


package com.exit.question.service;

import com.exit.question.controller.dto.request.*;
import com.exit.question.controller.dto.response.*;
import com.exit.question.domain.question.*;
import com.exit.question.domain.question.repository.*;
import com.exit.question.domain.response.*;
import com.exit.question.domain.response.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final QuestionCategoryRepository questionCategoryRepository;
    private final HashTagRepository hashTagRepository;
    private final QuestionImageRepository questionImageRepository;
    private final QuestionReportRepository questionReportRepository;
    private final ResponseRepository responseRepository;
    private final ResponseImageRepository responseImageRepository;
    private final ResponseReferenceRepository responseReferenceRepository;
    private final ResponseLikeRepository responseLikeRepository;
    private final ResponseReportRepository responseReportRepository;

//    public CategoryRecommendationResponse categoryRecommend(String title) {
//        // 1. 과거 질문 데이터를 기반으로 유사한 제목의 질문에서 가장 많이 사용된 카테고리 찾기
//        QuestionCategory mostUsedCategory = questionCategoryRepository.findMostUsedCategoryByTitlePattern(title);
//        if (mostUsedCategory != null) {
//            return CategoryRecommendationResponse.from(mostUsedCategory);
//
//        }
//
//        // 2. 제목에서 키워드를 추출하여 카테고리 이름과 매칭
//        String[] keywords = title.toLowerCase().split("\\s+");
//        for (String keyword : keywords) {
//            List<QuestionCategory> matchingCategories = questionCategoryRepository.findCategoriesByKeyword(keyword);
//            if (!matchingCategories.isEmpty()) {
//                QuestionCategory matchedCategory = matchingCategories.get(0);
//                return CategoryRecommendationResponse.from(matchedCategory);
//            }
//        }
//
//        // 3. 기본값: 첫 번째 카테고리 반환
//        // TODO: 더 정교한 카테고리 추천을 위해서는 AI/ML 기술이 필요
//        // - 자연어 처리(NLP)를 통한 의미적 유사도 계산
//        // - 머신러닝 모델을 활용한 카테고리 분류
//        // - 벡터 임베딩 기반 유사도 매칭
//        List<QuestionCategory> allCategories = questionCategoryRepository.findAll();
//        if (allCategories.isEmpty()) {
//            return null;
//        }
//
//        QuestionCategory defaultCategory = allCategories.get(0);
//        return CategoryRecommendationResponse.from(defaultCategory);
//    }
//
//    public HashtagSuggestionResponse hashtagSuggest(HashtagSuggestionRequest hashtagSuggestionRequest) {
//
//        // 1. 단어 기반 해시태그 검색
//
//        // 2. 기본값: null 반환 (추천할 해시태그가 없음)
//        // TODO: 더 정교한 해시태그 추천을 위해서는 AI/ML 기술이 필요
//        // - 자연어 처리(NLP)를 통한 키워드 추출 및 의미 분석
//        // - TF-IDF 또는 Word2Vec을 활용한 텍스트 유사도 계산
//        // - 협업 필터링을 통한 사용자 기반 해시태그 추천
//        // - 딥러닝 모델을 활용한 자동 태깅 시스템
//        return null;
//    }
//
//    public SimilarQuestionResponse similarQuestion(String title) {
//        List<Question> questions = questionRepository.findAll();
//        if (questions.isEmpty()) {
//            return null;
//        }
//        Question similarQuestion = questions.get(0);
//        List<HashTag> hashTags = hashTagRepository.findAll().stream()
//                .filter(tag -> tag.getQuestionId().equals(similarQuestion.getQuestionId()))
//                .collect(Collectors.toList());
//        List<String> hashTagTitles = hashTags.stream()
//                .map(HashTag::getHashTagTitle)
//                .collect(Collectors.toList());
//
//        return new SimilarQuestionResponse(
//                similarQuestion.getQuestionId(),
//                similarQuestion.getQuestionTitle(),
//                similarQuestion.getQuestionContent(),
//                similarQuestion.getQuestionCategory(),
//                similarQuestion.getQuestionUrgency(),
//                similarQuestion.getQuestionAnswerType(),
//                similarQuestion.getQuestionAnswerAdopt(),
//                hashTagTitles,
//                0.85,
//                similarQuestion.getCreatedAt()
//        );
//    }

    public QuestionCreateResponse questionCreate(QuestionCreateRequest questionCreateRequest) {
        Question question = Question.createQuestionFromRequest(questionCreateRequest);

        Question savedQuestion = questionRepository.save(question);

        if (questionCreateRequest.hashTags() != null) {
            for (String hashTagTitle : questionCreateRequest.hashTags()) {
                HashTag hashTag = HashTag.builder()
                        .questionId(savedQuestion.getQuestionId())
                        .hashTagTitle(hashTagTitle)
                        .build();
                hashTagRepository.save(hashTag);
            }
        }

        if (questionCreateRequest.images() != null) {
            // 이미지 저장 작업
            for (String imageUrl : questionCreateRequest.images()) {
                QuestionImage questionImage = QuestionImage.builder()
                        .questionId(savedQuestion.getQuestionId())
                        .questionImageUrl(imageUrl)
                        .build();
                questionImageRepository.save(questionImage);
            }
        }

        return new QuestionCreateResponse(
                savedQuestion.getQuestionId(),
                savedQuestion.getQuestionCategoryId(),
                savedQuestion.getQuestionWriterId(),
                savedQuestion.getQuestionTitle(),
                savedQuestion.getQuestionContent(),
                savedQuestion.getQuestionCategory(),
                savedQuestion.getQuestionUrgency(),
                savedQuestion.getQuestionAnswerType(),
                savedQuestion.getQuestionAnswerAdopt(),
                questionCreateRequest.hashTags(),
                questionCreateRequest.imageUrls(),
                savedQuestion.getCreatedAt(),
                savedQuestion.getUpdatedAt()
        );
    }

    public AnswerAdoptResponse answerAdopt(AnswerAdoptRequest answerAdoptRequest) {
        Optional<Response> responseOpt = responseRepository.findById(answerAdoptRequest.responseId());
        if (responseOpt.isEmpty()) {
            throw new RuntimeException("Response not found");
        }

        Response response = responseOpt.get();
        Response updatedResponse = Response.builder()
                .responseId(response.getResponseId())
                .questionId(response.getQuestionId())
                .responseWriterId(response.getResponseWriterId())
                .responseTitle(response.getResponseTitle())
                .responseContent(response.getResponseContent())
                .responseDisclosure(response.getResponseDisclosure())
                .responseAdopt(true)
                .build();

        Response savedResponse = responseRepository.save(updatedResponse);

        return new AnswerAdoptResponse(
                savedResponse.getResponseId(),
                savedResponse.getResponseAdopt(),
                savedResponse.getUpdatedAt()
        );
    }

    public AnswerCreateResponse answerCreate(AnswerCreateRequest answerCreateRequest) {
        Response response = Response.builder()
                .questionId(answerCreateRequest.questionId())
                .responseTitle(answerCreateRequest.responseTitle())
                .responseContent(answerCreateRequest.responseContent())
                .responseDisclosure(answerCreateRequest.responseDisclosure())
                .responseAdopt(false)
                .build();

        Response savedResponse = responseRepository.save(response);

        if (answerCreateRequest.imageUrls() != null) {
            for (String imageUrl : answerCreateRequest.imageUrls()) {
                ResponseImage responseImage = ResponseImage.builder()
                        .responseId(savedResponse.getResponseId())
                        .responseImageUrl(imageUrl)
                        .build();
                responseImageRepository.save(responseImage);
            }
        }

        if (answerCreateRequest.referenceUrls() != null) {
            for (String referenceUrl : answerCreateRequest.referenceUrls()) {
                ResponseReference responseReference = ResponseReference.builder()
                        .responseId(savedResponse.getResponseId())
                        .responseReferenceUrl(referenceUrl)
                        .build();
                responseReferenceRepository.save(responseReference);
            }
        }

        int likeCount = responseLikeRepository.findAll().stream()
                .filter(like -> like.getResponseId().equals(savedResponse.getResponseId()))
                .size();

        return new AnswerCreateResponse(
                savedResponse.getResponseId(),
                savedResponse.getQuestionId(),
                savedResponse.getResponseWriterId(),
                savedResponse.getResponseTitle(),
                savedResponse.getResponseContent(),
                savedResponse.getResponseDisclosure(),
                savedResponse.getResponseAdopt(),
                answerCreateRequest.imageUrls(),
                answerCreateRequest.referenceUrls(),
                likeCount,
                savedResponse.getCreatedAt(),
                savedResponse.getUpdatedAt()
        );
    }

    public AnswerRecommendResponse answerRecommend(AnswerRecommendRequest answerRecommendRequest) {
        ResponseLike responseLike = ResponseLike.builder()
                .responseId(answerRecommendRequest.responseId())
                .userId(1L)
                .build();

        ResponseLike savedResponseLike = responseLikeRepository.save(responseLike);

        return new AnswerRecommendResponse(
                savedResponseLike.getResponseLikeId(),
                savedResponseLike.getResponseId(),
                savedResponseLike.getUserId(),
                savedResponseLike.getCreatedAt()
        );
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

    public QuestionListResponse questionList(String title) {
        List<Question> questions = questionRepository.findAll();
        if (questions.isEmpty()) {
            return null;
        }

        Question question = questions.get(0);
        List<HashTag> hashTags = hashTagRepository.findAll().stream()
                .filter(tag -> tag.getQuestionId().equals(question.getQuestionId()))
                .collect(Collectors.toList());
        List<String> hashTagTitles = hashTags.stream()
                .map(HashTag::getHashTagTitle)
                .collect(Collectors.toList());

        List<QuestionImage> questionImages = questionImageRepository.findAll().stream()
                .filter(img -> img.getQuestionId().equals(question.getQuestionId()))
                .collect(Collectors.toList());
        List<String> imageUrls = questionImages.stream()
                .map(QuestionImage::getQuestionImageUrl)
                .collect(Collectors.toList());

        int answerCount = responseRepository.findAll().stream()
                .filter(response -> response.getQuestionId().equals(question.getQuestionId()))
                .size();

        return new QuestionListResponse(
                question.getQuestionId(),
                question.getQuestionCategoryId(),
                question.getQuestionWriterId(),
                question.getQuestionTitle(),
                question.getQuestionContent(),
                question.getQuestionCategory(),
                question.getQuestionUrgency(),
                question.getQuestionAnswerType(),
                question.getQuestionAnswerAdopt(),
                hashTagTitles,
                imageUrls,
                answerCount,
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

}


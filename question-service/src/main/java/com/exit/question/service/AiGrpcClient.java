package com.exit.question.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.ai.*;
import com.exit.question.exception.GrpcAiErrorCode;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * AI 서비스와 통신하는 gRPC 클라이언트
 * - AI 답변 생성
 * - 카테고리 분류
 * - 유사 질문 검색
 */
@Service
@Slf4j
public class AiGrpcClient {

    @GrpcClient("ai-service")
    private AIQuestionServiceGrpc.AIQuestionServiceBlockingStub aiQuestionServiceBlockingStub;

    /**
     * AI 답변 생성 요청
     * @param question 질문 내용 (제목 + 본문)
     * @return AI가 생성한 답변 텍스트
     */
    public String generateAiAnswer(String question, Long questionId) {
        try {
            log.info("Requesting AI answer generation for question: {}",
                    question.substring(0, Math.min(50, question.length())));

            AnswerRequest request = AnswerRequest.newBuilder()
                    .setQuestion(question)
                    .setQuestionId(questionId)
                    .build();

            AnswerResponse response = aiQuestionServiceBlockingStub.generateAIAnswer(request);

            log.info("AI answer generated successfully.");

            return response.getAnswer();

        } catch (StatusRuntimeException e) {
            log.error("gRPC error while generating AI answer: {}", e.getStatus(), e);
            throw new GrpcException(GrpcAiErrorCode.AI_ANSWER_FAIL, "AI 답변 생성 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("Unexpected error while generating AI answer", e);
            throw new GrpcException(GrpcAiErrorCode.AI_ANSWER_FAIL, "AI 답변 생성 중 예상치 못한 오류가 발생했습니다");
        }
    }

    public Integer categoryRecommend(String title) {
        try {
            log.info("Requesting AI Category Recommend for title: {}", title);

            ClassifyRequest request = ClassifyRequest.newBuilder()
                    .setTitle(title)
                    .build();

            CategoryResponse response = aiQuestionServiceBlockingStub.classifyCategory(request);

            log.info("AI category recommend successfully");

            return response.getCategoryId();

        } catch (StatusRuntimeException e) {
            log.error("gRPC error while recommend AI Category: {}", e.getStatus(), e);
            throw new GrpcException(GrpcAiErrorCode.AI_CATEGORY_RECOMMEND_FAIL, "AI 카테고리 추천 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("Unexpected error while recommend AI Category", e);
            throw new GrpcException(GrpcAiErrorCode.AI_CATEGORY_RECOMMEND_FAIL, "AI 카테고리 추천 중 예상치 못한 오류가 발생했습니다");
        }
    }

    public SimilarResponse similarQuestion(String title, String content) {
        try {
            log.info("Requesting find similarQuestion for title : {}", title);

            SimilarRequest request = SimilarRequest.newBuilder()
                    .setTitle(title)
                    .setContent(content)
                    .build();

            log.info("AI category recommend successfully");

            return aiQuestionServiceBlockingStub.findSimilarQuestions(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error while find similarQuestion: {}", e.getStatus(), e);
            throw new GrpcException(GrpcAiErrorCode.AI_SIMILAR_QUESTION_FAIL, "AI 유사 질문 찾기 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("Unexpected error while find similarQuestion", e);
            throw new GrpcException(GrpcAiErrorCode.AI_SIMILAR_QUESTION_FAIL, "AI 유사 질문 찾기 중 예상치 못한 오류가 발생했습니다");
        }
    }

    public SaveQuestionResponse saveQuestion(SaveQuestionRequest request) {
        try {
            return aiQuestionServiceBlockingStub.saveQuestion(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error while save question vector DB: {}", e.getStatus(), e);
            throw new GrpcException(GrpcAiErrorCode.AI_SAVE_QUESTION_FAIL, "백터 디비 질문 저장 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("Unexpected error while save question vector DB", e);
            throw new GrpcException(GrpcAiErrorCode.AI_SAVE_QUESTION_FAIL, "백터 디비 질문 저장 중 예상치 못한 오류가 발생했습니다");
        }
    }
}

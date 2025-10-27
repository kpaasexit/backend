package com.exit.question.service;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.Authority;
import com.exit.common.grpc.CategoryRecommendationResponse;
import com.exit.common.grpc.DeleteQuestionRequest;
import com.exit.common.grpc.GetMyQuestionRequest;
import com.exit.common.grpc.GetMyQuestionResponse;
import com.exit.common.grpc.GetPopularPostResponse;
import com.exit.common.grpc.GetUsersNameAndProfileResponse;
import com.exit.common.grpc.ImageObject;
import com.exit.common.grpc.PopularPostItem;
import com.exit.common.grpc.QuestionCreateRequest;
import com.exit.common.grpc.QuestionCreateResponse;
import com.exit.common.grpc.QuestionDetailRequest;
import com.exit.common.grpc.QuestionDetailResponse;
import com.exit.common.grpc.QuestionListRequest;
import com.exit.common.grpc.QuestionListResponse;
import com.exit.common.grpc.QuestionReportRequest;
import com.exit.common.grpc.QuestionReportResponse;
import com.exit.common.grpc.SendNotificationRequest;
import com.exit.common.grpc.SimilarQuestionResponse;
import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import com.exit.common.grpc.UpdateQuestionRequest;
import com.exit.common.grpc.UploadBytesRequest;
import com.exit.common.grpc.ai.SaveQuestionRequest;
import com.exit.common.grpc.ai.SimilarQuestion;
import com.exit.common.grpc.ai.SimilarResponse;
import com.exit.common.util.file.FileUploadUtil;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.controller.dto.response.CommentAndAdditionalQuestionNum;
import com.exit.question.controller.dto.response.PopularPostDto;
import com.exit.question.controller.dto.response.QuestionListQueryResponseDto;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.QuestionCategory;
import com.exit.question.domain.question.QuestionImage;
import com.exit.question.domain.question.QuestionReport;
import com.exit.question.domain.question.repository.QuestionCategoryRepository;
import com.exit.question.domain.question.repository.QuestionCommentRepository;
import com.exit.question.domain.question.repository.QuestionImageRepository;
import com.exit.question.domain.question.repository.QuestionReportRepository;
import com.exit.question.domain.question.repository.QuestionRepository;
import com.exit.question.domain.response.Response;
import com.exit.question.domain.response.repository.ResponseRepository;
import com.exit.question.exception.GrpcQuestionErrorCode;
import com.exit.question.service.client.AiGrpcClient;
import com.exit.question.service.client.NotificationGrpcClient;
import com.exit.question.service.client.UserGrpcClient;
import com.exit.question.service.util.NotificationGrpcMapper;
import com.exit.question.service.util.QuestionGrpcMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final QuestionCommentRepository questionCommentRepository;
    private final ResponseRepository responseRepository;
    private final FileUploadUtil fileUploadUtil;
    private final UserGrpcClient userGrpcClient;
    private final AiGrpcClient aiGrpcClient;
    private final TaskScheduler taskScheduler;
    private final QuestionGrpcMapper questionGrpcMapper;
    private final NotificationGrpcClient notificationGrpcClient;
    private final NotificationGrpcMapper notificationGrpcMapper;

    public QuestionCreateResponse createQuestion(QuestionCreateRequest request) {
        try {
            QuestionCategory questionCategory = questionCategoryRepository.findById(request.getQuestionCategory())
                    .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.UNAVAILABLE_QUESTION_CATEGORY));
            Question question = Question.createQuestionFromRequest(request, questionCategory);
            Question savedQuestion = questionRepository.save(question);

            List<ImageObject> imageObjects = uploadQuestionImages(request, savedQuestion);
            UpdateAdditionalUserInfoResponse userNameAndProfile = userGrpcClient.getUserNameAndProfile(
                    question.getQuestionWriterId());

            aiGrpcClient.saveQuestion(createSaveQuestionToVectorDBRequest(question));
            // AI 답변 자동 생성
            scheduleAiAnswerGeneration(savedQuestion, request.getImagesList());
            CommentAndAdditionalQuestionNum commentAndAdditionalQuestionNum = questionRepository.findCommentAndAdditionalQuestionNumByQuestionId(
                    question.getQuestionId());
            return questionGrpcMapper.getQuestionCreateResponse(savedQuestion, imageObjects, userNameAndProfile, commentAndAdditionalQuestionNum);
        } catch (GrpcException e) {
            throw new GrpcException(GrpcQuestionErrorCode.CREATE_QUESTION_FAILED,
                    e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Create question failed for userId: {}", request.getQuestionWriterId(), e);
            throw new GrpcException(GrpcQuestionErrorCode.CREATE_QUESTION_FAILED, e.getMessage());
        }
    }

    public QuestionReportResponse questionReport(QuestionReportRequest request) {
        try {
            Question question = questionRepository.findById(request.getQuestionId())
                    .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));

            QuestionReport questionReport = QuestionReport.from(request);
            userGrpcClient.increaseReportCount(question.getQuestionWriterId());

            QuestionReport savedQuestionReport = questionReportRepository.save(questionReport);
            return questionGrpcMapper.getQuestionReportResponse(savedQuestionReport);
        } catch (GrpcException e) {
            throw new GrpcException(GrpcQuestionErrorCode.QUESTION_REPORT_FAILED,
                    e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Question report failed for questionId: {}", request.getQuestionId(), e);
            throw new GrpcException(GrpcQuestionErrorCode.QUESTION_REPORT_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public QuestionListResponse questionList(QuestionListRequest filter) {
        try {
            PageRequest pageRequest = PageRequest.of(filter.getPageNum(), filter.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<QuestionListQueryResponseDto> page = questionRepository.findQuestionsByFilter(
                    filter.getCategoryIdsList(),
                    filter.getKeyword().isEmpty() ? null : filter.getKeyword(),
                    filter.getIsAnswered(), pageRequest);

            Set<Long> writerIds = page.getContent().stream().map(QuestionListQueryResponseDto::questionWriterId)
                    .collect(toSet());
            GetUsersNameAndProfileResponse usersNameAndProfile = userGrpcClient.getUsersNameAndProfile(writerIds);
            Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap = usersNameAndProfile.getUserInfoList().stream()
                    .collect(toMap(UpdateAdditionalUserInfoResponse::getUserId, Function.identity()));

            return questionGrpcMapper.getQuestionListResponse(page, userInfoMap);
        } catch (Exception e) {
            log.error("Get question list failed", e);
            throw new GrpcException(GrpcQuestionErrorCode.GET_QUESTION_LIST_FAILED, e.getMessage());
        }
    }

    // 카테고리 추천
    @Transactional(readOnly = true)
    public CategoryRecommendationResponse categoryRecommend(String title) {
        try {
            Long categoryRecommend = aiGrpcClient.categoryRecommend(title).longValue();
            QuestionCategory questionCategory = questionCategoryRepository.findById(categoryRecommend)
                    .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.UNAVAILABLE_QUESTION_CATEGORY));

            return CategoryRecommendationResponse.newBuilder()
                    .setCategoryId(questionCategory.getQuestionCategoryId())
                    .setCategoryName(questionCategory.getQuestionCategoryName())
                    .build();
        } catch (GrpcException e) {
            throw new GrpcException(GrpcQuestionErrorCode.CATEGORY_RECOMMEND_FAILED,
                    e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Category recommend failed for title: {}", title, e);
            throw new GrpcException(GrpcQuestionErrorCode.CATEGORY_RECOMMEND_FAILED, e.getMessage());
        }
    }

    // 유사 질문 조회
    @Transactional(readOnly = true)
    public SimilarQuestionResponse similarQuestion(String title, String content) {
        try {
            SimilarResponse similarResponse = aiGrpcClient.similarQuestion(title, content);
            if (similarResponse.getQuestionsCount() <= 0) {
                return null;
            }

            List<Long> similarQuestionIds = similarResponse.getQuestionsList().stream()
                    .map(SimilarQuestion::getQuestionId).toList();
            List<Question> similarQuestions = questionRepository.findAllById(similarQuestionIds);
            return questionGrpcMapper.getSimilarQuestionResponse(similarQuestions);
        } catch (Exception e) {
            log.error("Similar question search failed for title: {}", title, e);
            throw new GrpcException(GrpcQuestionErrorCode.SIMILAR_QUESTION_FAILED, e.getMessage());
        }
    }

    // 질문 게시글 상세 보기
    @Transactional(readOnly = true)
    public QuestionDetailResponse getQuestionDetail(QuestionDetailRequest request) {
        try {
            QuestionCreateResponse questionCreateResponse = buildQuestionCreateResponse(request.getQuestionId(),
                    request.getUserId());

            Authority authority = getAuthority(request);
            return questionGrpcMapper.getQuestionDetailResponse(questionCreateResponse, authority);
        } catch (GrpcException e) {
            throw new GrpcException(GrpcQuestionErrorCode.GET_QUESTION_DETAIL_FAILED,
                    e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Get question detail failed for questionId: {}", request.getQuestionId(), e);
            throw new GrpcException(GrpcQuestionErrorCode.GET_QUESTION_DETAIL_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetPopularPostResponse getPopularPost() {
        try {
            List<PopularPostDto> popularPosts = questionRepository.findTop5By();

            if (popularPosts.isEmpty()) {
                return GetPopularPostResponse.newBuilder().build();
            }

            // 모든 작성자 ID 수집
            Set<Long> writerIds = popularPosts.stream()
                    .map(PopularPostDto::questionWriterId)
                    .collect(toSet());

            // 배치로 사용자 정보 조회
            Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap = getUserNicknameAndProfileByWriterIds(writerIds);

            // PopularPostItem 생성
            List<PopularPostItem> items = popularPosts.stream()
                    .map(post -> {
                        UpdateAdditionalUserInfoResponse userInfo = userInfoMap.get(post.questionWriterId());
                        return PopularPostItem.newBuilder()
                                .setQuestionId(post.questionId())
                                .setCategoryId(post.questionCategoryId())
                                .setProfileUrl(!userInfo.getUserProfile().isEmpty() ? userInfo.getUserProfile() : "")
                                .setNickname(userInfo.getUserName())
                                .setTitle(post.questionTitle())
                                .setContent(post.questionContent())
                                .setIsAnswered(post.isAnswered())
                                .setAnswerCount(post.answerCount())
                                .setCreatedAt(TimeStampUtil.toGrpcTimestamp(post.createdAt()))
                                .build();
                    })
                    .toList();

            return GetPopularPostResponse.newBuilder()
                    .addAllPost(items)
                    .build();
        } catch (Exception e) {
            log.error("Get popular post failed", e);
            throw new GrpcException(GrpcQuestionErrorCode.GET_POPULAR_POST_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetMyQuestionResponse getMyQuestion(GetMyQuestionRequest request) {
        try {
            PageRequest pageRequest = PageRequest.of(request.getPageNum(), request.getSize());
            Page<PopularPostDto> myQuestionDtos = questionRepository.findByQuestionWriterId(request.getUserId(),
                    pageRequest);

            Set<Long> writerIds = myQuestionDtos.getContent().stream()
                    .map(PopularPostDto::questionWriterId)
                    .collect(toSet());

            Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap = getUserNicknameAndProfileByWriterIds(writerIds);

            List<PopularPostItem> popularPostItemList = getPopularPostItemList(myQuestionDtos, userInfoMap);

            return GetMyQuestionResponse.newBuilder()
                    .addAllPost(popularPostItemList)
                    .setCurrentPage(myQuestionDtos.getNumber() + 1)
                    .setHasNext(myQuestionDtos.hasNext())
                    .setTotalPageNum(myQuestionDtos.getTotalPages())
                    .build();
        } catch (Exception e) {
            log.error("Get my question failed for userId: {}", request.getUserId(), e);
            throw new GrpcException(GrpcQuestionErrorCode.GET_MY_QUESTION_FAILED, e.getMessage());
        }
    }

    public QuestionCreateResponse updateQuestion(UpdateQuestionRequest request) {
        try {
            Question question = questionRepository.notExistsResponseByQuestionId(request.getQuestionId())
                    .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.ALREADY_EXISTS_RESPONSE));

            if (!request.getContent().isEmpty()) {
                question.updateQuestionContent(request.getContent());
            }

            if (!request.getDeletedImageIdList().isEmpty()) {
                List<QuestionImage> questionImages = questionImageRepository.findAllByQuestionId(
                                request.getQuestionId())
                        .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_EXIST_QUESTION_IMAGE));

                List<String> imageUrls = questionImages.stream().map(QuestionImage::getQuestionImageUrl).toList();

                fileUploadUtil.deleteFiles(imageUrls);
                questionImageRepository.deleteAllById(request.getDeletedImageIdList());
                questionImageRepository.flush();
            }

            if (!request.getImagesList().isEmpty()) {
                List<String> imageUrls = fileUploadUtil.uploadImages(request.getImagesList(), QUESTION_FOLDER);

                imageUrls.forEach(imageUrl -> {
                            QuestionImage questionImage = QuestionImage.builder()
                                    .questionId(request.getQuestionId())
                                    .questionImageUrl(imageUrl)
                                    .build();
                            questionImageRepository.saveAndFlush(questionImage);
                        }
                );
            }

            if(!request.getTitle().isEmpty()) {
                question.updateQuestionTitle(request.getTitle());
            }

            if(request.getQuestionCategoryId() != 0) {
                QuestionCategory questionCategory = questionCategoryRepository.findById(request.getQuestionCategoryId())
                        .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.UNAVAILABLE_QUESTION_CATEGORY));
                question.updateQuestionCategory(questionCategory);
            }

            return buildQuestionCreateResponse(request.getQuestionId(), request.getUserId());
        } catch (GrpcException e) {
            throw new GrpcException(GrpcQuestionErrorCode.GET_QUESTION_DETAIL_FAILED,
                    e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Get question detail failed for questionId: {}", request.getQuestionId(), e);
            throw new GrpcException(GrpcQuestionErrorCode.GET_QUESTION_DETAIL_FAILED, e.getMessage());
        }
    }

    public void deleteQuestion(DeleteQuestionRequest request) {
        log.info("Deleting question with id {}", request.getQuestionId());
        Optional<List<QuestionImage>> allByQuestionId = questionImageRepository.findAllByQuestionId(request.getQuestionId());
        if(allByQuestionId.isPresent()) {
            List<String> imageUrls = allByQuestionId.get().stream().map(QuestionImage::getQuestionImageUrl).toList();
            fileUploadUtil.deleteFiles(imageUrls);
        }
        questionImageRepository.deleteByQuestionId(request.getQuestionId());

        questionCommentRepository.deleteAllByQuestion_QuestionId(request.getQuestionId());
        questionRepository.deleteById(request.getQuestionId());
        log.info("Deleted question with id {}", request.getQuestionId());
    }

    private List<PopularPostItem> getPopularPostItemList(Page<PopularPostDto> myQuestionDtos,
                                                         Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap) {
        return myQuestionDtos.getContent().stream()
                .map(post -> {
                    UpdateAdditionalUserInfoResponse userInfo = userInfoMap.get(post.questionWriterId());
                    return PopularPostItem.newBuilder()
                            .setQuestionId(post.questionId())
                            .setCategoryId(post.questionCategoryId())
                            .setProfileUrl(!userInfo.getUserProfile().isEmpty() ? userInfo.getUserProfile() : "")
                            .setNickname(userInfo.getUserName())
                            .setTitle(post.questionTitle())
                            .setContent(post.questionContent())
                            .setIsAnswered(post.isAnswered())
                            .setAnswerCount(post.answerCount())
                            .setCreatedAt(TimeStampUtil.toGrpcTimestamp(post.createdAt()))
                            .build();
                })
                .toList();
    }

    private QuestionCreateResponse buildQuestionCreateResponse(Long questionId, Long userId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));

        Optional<List<QuestionImage>> images = questionImageRepository.findAllByQuestionId(questionId);
        List<ImageObject> imageObjectDtos = new ArrayList<>();
        images.ifPresent(questionImages -> {
                    questionImages.sort(Comparator.comparing(QuestionImage::getCreatedAt));
                    questionImages.forEach(image ->
                            imageObjectDtos.add(
                                    ImageObject.newBuilder()
                                            .setImageId(image.getQuestionImageId())
                                            .setImageUrl(image.getQuestionImageUrl())
                                            .build()
                            ));
                }
        );
        UpdateAdditionalUserInfoResponse userNameAndProfile = userGrpcClient.getUserNameAndProfile(
                question.getQuestionWriterId());

        CommentAndAdditionalQuestionNum commentAndAdditionalQuestionNum = questionRepository.findCommentAndAdditionalQuestionNumByQuestionId(
                question.getQuestionId());

        return questionGrpcMapper.getQuestionCreateResponse(question, imageObjectDtos, userNameAndProfile, commentAndAdditionalQuestionNum);
    }

    private Authority getAuthority(QuestionDetailRequest request) {
        if(request.getUserId() == -1) {
            return Authority.newBuilder()
                    .setCanDelete(false)
                    .setCanModify(false)
                    .setCanWrite(false)
                    .build();
        }
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));
        boolean isSameUser = Objects.equals(question.getQuestionWriterId(), request.getUserId());
        boolean existResponseByUserId = responseRepository.existsByQuestionIdAndResponseWriterId(question.getQuestionId(),  request.getUserId());
        boolean existResponseByQuestionId = questionRepository.existResponseByQuestionId(question.getQuestionId());
        return Authority.newBuilder()
                .setCanDelete(isSameUser && !existResponseByQuestionId)
                .setCanModify(isSameUser && !existResponseByQuestionId)
                .setCanWrite(!isSameUser && !existResponseByUserId)
                .build();
    }

    private Map<Long, UpdateAdditionalUserInfoResponse> getUserNicknameAndProfileByWriterIds(Set<Long> writerIds) {
        List<UpdateAdditionalUserInfoResponse> userInfoList = userGrpcClient.getUsersNameAndProfile(
                new HashSet<>(writerIds)).getUserInfoList();
        Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap = userInfoList.stream()
                .collect(toMap(UpdateAdditionalUserInfoResponse::getUserId, Function.identity()));
        return userInfoMap;
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
     * AI 답변 생성 스케줄링 긴급 질문: 즉시 생성 일반 질문: 5분 후 생성
     */
    private void scheduleAiAnswerGeneration(Question question, List<UploadBytesRequest> uploadBytesRequests) {
        if (Boolean.TRUE.equals(question.getQuestionUrgency())) {
            log.info("Question urgency has been scheduled");
            // 긴급 질문은 즉시 생성
            generateAiAnswerAsync(question, uploadBytesRequests);
        } else {
            // 일반 질문은 5분 후 생성
            log.info("Question urgency has been unscheduled");
            Instant scheduledTime = Instant.now().plus(Duration.ofMinutes(5));
            taskScheduler.schedule(() -> generateAiAnswerAsync(question, uploadBytesRequests), scheduledTime);
        }
    }

    /**
     * 질문 생성 시 AI 답변을 자동으로 생성하여 저장 AI 생성 실패 시 최대 3회 재시도 (지수 백오프) 모든 재시도 실패 시에도 질문 생성은 정상 처리됨
     */
    @Async("aiAnswerTaskExecutor")
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            recover = "recoverGenerateAiAnswer"
    )
    protected void generateAiAnswerAsync(Question question, List<UploadBytesRequest> uploadBytesRequests) {
        // AI 답변 생성 요청
        String aiAnswer = aiGrpcClient.generateAiAnswer(question.getQuestionId(), uploadBytesRequests);
        log.info("Ai answer has been generated: {}", aiAnswer);
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
        SendNotificationRequest sendNotificationRequest = notificationGrpcMapper.getSendNotificationRequest(
                truncateContent(question.getQuestionContent()), "NEW_ANSWER_ON_QUESTION", question);
        notificationGrpcClient.sendNotification(sendNotificationRequest);
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

    private List<ImageObject> uploadQuestionImages(QuestionCreateRequest request, Question savedQuestion) {
        if (!request.getImagesList().isEmpty()) {
            List<String> imageUrls = fileUploadUtil.uploadImages(request.getImagesList(), QUESTION_FOLDER);
            List<QuestionImage> savedQuestionImages = imageUrls.stream()
                    .map(url -> {
                        QuestionImage questionImage = QuestionImage.builder()
                                .questionId(savedQuestion.getQuestionId())
                                .questionImageUrl(url)
                                .build();
                        return questionImageRepository.saveAndFlush(questionImage);
                    })
                    .toList();

            return savedQuestionImages.stream()
                    .sorted(Comparator.comparing(QuestionImage::getCreatedAt))
                    .map(image -> {
                                return ImageObject.newBuilder()
                                        .setImageId(image.getQuestionImageId())
                                        .setImageUrl(image.getQuestionImageUrl())
                                        .build();
                            }
                    ).toList();
        }

        return null;
    }

    private String truncateContent(String content) {
        String subBody;
        if (content.length() <= 100) {
            subBody = content.substring(0, content.length() - 1);
        } else {
            subBody = content.substring(0, 100);
        }
        return subBody;
    }
}
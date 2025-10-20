package com.exit.question.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.common.util.file.FileUploadUtil;
import com.exit.question.domain.question.FollowUpImage;
import com.exit.question.domain.question.FollowUpMessage;
import com.exit.question.domain.question.FollowUpRoom;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.repository.FollowUpImageRepository;
import com.exit.question.domain.question.repository.FollowUpMessageRepository;
import com.exit.question.domain.question.repository.FollowUpRoomRepository;
import com.exit.question.domain.question.repository.QuestionRepository;
import com.exit.question.domain.response.Response;
import com.exit.question.domain.response.repository.ResponseRepository;
import com.exit.question.exception.GrpcQuestionErrorCode;
import com.exit.question.exception.GrpcResponseErrorCode;
import com.exit.question.service.client.AiGrpcClient;
import com.exit.question.service.client.NotificationGrpcClient;
import com.exit.question.service.util.AiGrpcMapper;
import com.exit.question.service.util.NotificationGrpcMapper;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.exit.common.util.time.TimeStampUtil.toGrpcTimestamp;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdditionalQuestionService {
    private static final String ADDITIONAL_QUESTION_PATH = "ADDITIONAL";
    private final ResponseRepository responseRepository;
    private final FollowUpImageRepository followUpImageRepository;
    private final FollowUpMessageRepository followUpMessageRepository;
    private final FollowUpRoomRepository followUpRoomRepository;
    private final FileUploadUtil fileUploadUtil;
    private final QuestionRepository questionRepository;
    private final NotificationGrpcClient notificationGrpcClient;
    private final AiGrpcClient aiGrpcClient;
    private final NotificationGrpcMapper notificationGrpcMapper;
    private final AiGrpcMapper aiGrpcMapper;

    public CreateAdditionalQuestionMessageResponse createAdditionalQuestionMessage(CreateAdditionalQuestionMessageRequest request) {
        try {
            Response response = findResponseById(request.getResponseId());
            FollowUpRoom followUpRoom = findOrCreateFollowUpRoom(response);
            FollowUpMessage savedMessage = createAndSaveMessage(request, followUpRoom);
            List<String> imageUrls = saveUploadedImages(request.getImagesList(), savedMessage);

            Question question = getQuestion(request.getQuestionId());
            boolean isQuestioner = isUserQuestioner(question.getQuestionWriterId(), request.getUserId());
            if (isQuestioner && response.getResponseWriterId() == 1L) {
                generateAiAnswerAsync(savedMessage.getFollowUpMessageContent(), question);
            }

            MessageItem messageItem = buildMessageItem(savedMessage, imageUrls, isQuestioner);

            if (response.getResponseWriterId() != 1L) {
                SendNotificationRequest sendNotificationRequest = createSendNotificationRequest(
                        messageItem, response.getResponseWriterId(), question.getQuestionWriterId());
                notificationGrpcClient.sendNotification(sendNotificationRequest);
            }

            return CreateAdditionalQuestionMessageResponse.newBuilder()
                    .setFollowUpRoomId(followUpRoom.getFollowUpRoomId())
                    .setMessage(messageItem)
                    .build();
        } catch (GrpcException e) {
            throw new GrpcException(com.exit.question.exception.GrpcAdditionalQuestionErrorCode.CREATE_ADDITIONAL_QUESTION_MESSAGE_FAILED,
                    e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Create additional question message failed for responseId: {}", request.getResponseId(), e);
            throw new GrpcException(com.exit.question.exception.GrpcAdditionalQuestionErrorCode.CREATE_ADDITIONAL_QUESTION_MESSAGE_FAILED,
                    e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetAdditionalQuestionResponse getAdditionalQuestion(GetAdditionalQuestionRequest request) {
        try {
            FollowUpRoom followUpRoom = followUpRoomRepository.findById(request.getFollowUpRoomId())
                    .orElseThrow(() -> new GrpcException(com.exit.question.exception.GrpcAdditionalQuestionErrorCode.NULL_FOLLOW_UP_ROOM));

            Question question = getQuestion(request.getQuestionId());

            List<MessageItem> messageList = followUpRoom.getFollowUpMessages().stream()
                    .map(message -> buildMessageItem(message, message.getImageUrlList(),
                            isUserQuestioner(question.getQuestionWriterId(), message.getFollowUpMessageWriterId())))
                    .toList();

            Authority authority = getAuthority(question, followUpRoom.getResponse(), request.getUserId(), followUpRoom.getFollowUpMessages());

            return GetAdditionalQuestionResponse.newBuilder()
                    .setFollowUpRoomId(request.getFollowUpRoomId())
                    .addAllMessage(messageList)
                    .setAuthority(authority)
                    .build();
        } catch (GrpcException e) {
            throw new GrpcException(com.exit.question.exception.GrpcAdditionalQuestionErrorCode.GET_ADDITIONAL_QUESTION_FAILED,
                    e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Get additional question failed for followUpRoomId: {}", request.getFollowUpRoomId(), e);
            throw new GrpcException(com.exit.question.exception.GrpcAdditionalQuestionErrorCode.GET_ADDITIONAL_QUESTION_FAILED,
                    e.getMessage());
        }
    }

    private Authority getAuthority(Question question, Response response, Long userId, List<FollowUpMessage> messageList) {
        boolean isThirdParty = isThirdParty(question, response, userId);
        boolean canWrite = false;

        if (!messageList.isEmpty() && !isThirdParty) {
            FollowUpMessage messageItem = messageList.get(messageList.size() - 1);
            canWrite = !Objects.equals(messageItem.getFollowUpMessageWriterId(), userId);
        }

        return Authority.newBuilder()
                .setIsThirdParty(isThirdParty)
                .setCanWrite(canWrite)
                .build();
    }

    private boolean isThirdParty(Question question, Response response, Long userId) {
        return !(Objects.equals(question.getQuestionWriterId(), userId) || Objects.equals(response.getResponseWriterId(), userId));
    }

    private Response findResponseById(Long responseId) {
        return responseRepository.findById(responseId)
                .orElseThrow(() -> new GrpcException(GrpcResponseErrorCode.NOT_FOUND_RESPONSE));
    }

    private FollowUpRoom findOrCreateFollowUpRoom(Response response) {
        return followUpRoomRepository.findByResponse(response)
                .orElseGet(() -> followUpRoomRepository.save(new FollowUpRoom(response)));
    }

    private FollowUpMessage createAndSaveMessage(CreateAdditionalQuestionMessageRequest request, FollowUpRoom followUpRoom) {
        FollowUpMessage message = FollowUpMessage.builder()
                .followUpRoom(followUpRoom)
                .followUpMessageWriterId(request.getUserId())
                .followUpMessageContent(request.getContent())
                .build();

        return followUpMessageRepository.save(message);
    }

    private List<String> saveUploadedImages(List<UploadBytesRequest> imageList, FollowUpMessage savedMessage) {
        List<String> uploadedImages = fileUploadUtil.uploadImages(imageList, ADDITIONAL_QUESTION_PATH);
        List<FollowUpImage> followUpImages = FollowUpImage.generateFollowUpImages(savedMessage, uploadedImages);
        followUpImageRepository.saveAll(followUpImages);
        return uploadedImages;
    }

    private boolean isUserQuestioner(Long questionWriterId, Long userId) {
        return questionWriterId.equals(userId);
    }

    private Question getQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));
    }


    private MessageItem buildMessageItem(FollowUpMessage savedMessage, List<String> imageUrls, boolean isQuestioner) {
        return MessageItem.newBuilder()
                .setIsQuestioner(isQuestioner)
                .setMessageId(savedMessage.getFollowUpMessageId())
                .setContent(savedMessage.getFollowUpMessageContent())
                .addAllImages(imageUrls)
                .setCreatedAt(toGrpcTimestamp(savedMessage.getCreatedAt()))
                .build();
    }

    private SendNotificationRequest createSendNotificationRequest(MessageItem messageItem, Long responseWriterId, Long questionWriterId) {
        boolean isQuestioner = messageItem.getIsQuestioner();
        String notificationType = isQuestioner ? "NEW_ADDITIONAL_QUESTION_ON_ANSWER" : "NEW_ANSWER_ON_ADDITIONAL_QUESTION";
        Long receiverId = isQuestioner ? responseWriterId : questionWriterId;
        String body = truncateContent(messageItem.getContent());

        return notificationGrpcMapper.getSendNotificationRequest(
                body, notificationType, messageItem.getMessageId(), receiverId);
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

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            recover = "recoverGenerateAiAnswer"
    )
    private void generateAiAnswerAsync(String content, Question question) {
        aiGrpcClient.saveQuestion(aiGrpcMapper.getSaveQuestionRequest(content, question));
        String aiAnswer = aiGrpcClient.generateAiAnswer(question.getQuestionId());

        FollowUpMessage aiFollowUpMessage = FollowUpMessage.builder()
                .followUpMessageContent(aiAnswer)
                .followUpMessageWriterId(1L)
                .build();

        FollowUpMessage saved = followUpMessageRepository.save(aiFollowUpMessage);
        log.info("AI Additional answer generated and saved for additional question ID: {}", saved.getFollowUpMessageId());
    }

    /**
     * AI 답변 생성 재시도 실패 시 폴백 메서드
     */
    @Recover
    private void recoverGenerateAiAnswer(Exception e, Long questionId) {
        log.error("Failed to generate AI answer after all retry attempts for question {}: {}",
                questionId, e.getMessage(), e);
        // TODO: 필요시 사용자에게 알림 전송 또는 재시도 큐에 추가
    }
}

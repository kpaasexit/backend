package com.exit.question.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.common.util.file.FileUploadUtil;
import com.exit.question.controller.dto.response.AiBestResponseDto;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.repository.FollowUpRoomRepository;
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
import com.exit.question.exception.GrpcResponseErrorCode;
import com.exit.question.service.client.NotificationGrpcClient;
import com.exit.question.service.client.UserGrpcClient;
import com.exit.question.service.util.NotificationGrpcMapper;
import com.exit.question.service.util.ResponseGrpcMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ResponseService {
    private static final String RESPONSE_FOLDER = "response";

    private final QuestionRepository questionRepository;
    private final ResponseRepository responseRepository;
    private final ResponseImageRepository responseImageRepository;
    private final ResponseLikeRepository responseLikeRepository;
    private final ResponseReportRepository responseReportRepository;
    private final FollowUpRoomRepository followUpRoomRepository;
    private final FileUploadUtil fileUploadUtil;
    private final UserGrpcClient userGrpcClient;
    private final NotificationGrpcClient notificationGrpcClient;
    private final ResponseGrpcMapper responseGrpcMapper;
    private final NotificationGrpcMapper notificationGrpcMapper;


    public AnswerCreateResponse answerCreate(AnswerCreateRequest request) {
        try {
            Response newResponse = Response.createResponse(request);
            Response savedResponse = responseRepository.save(newResponse);

            List<String> imageUrls = processAnswerImages(request, savedResponse);
            Question question = questionRepository.findById(savedResponse.getQuestionId())
                    .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));

            String subBody = truncateContent(savedResponse.getResponseContent());
            SendNotificationRequest sendNotificationRequest = notificationGrpcMapper.getSendNotificationRequest(
                    subBody, "NEW_ANSWER_ON_QUESTION", question);
            notificationGrpcClient.sendNotification(sendNotificationRequest);

            return responseGrpcMapper.getAnswerCreateResponse(savedResponse, imageUrls);
        } catch (GrpcException e) {
            throw new GrpcException(GrpcResponseErrorCode.CREATE_ANSWER_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Create answer failed for questionId: {}", request.getQuestionId(), e);
            throw new GrpcException(GrpcResponseErrorCode.CREATE_ANSWER_FAILED, e.getMessage());
        }
    }

    public AnswerRecommendResponse toggleAnswerLike(AnswerRecommendRequest request) {
        try {
            Optional<ResponseLike> existingLike =
                    responseLikeRepository.findByResponseIdAndUserId(request.getResponseId(), request.getUserId());

            boolean isLiked = handleLikeToggle(existingLike, request);
            int likeCount = responseLikeRepository.countByResponseId(request.getResponseId());

            return responseGrpcMapper.getAnswerRecommendResponse(request.getResponseId(), likeCount, isLiked);
        } catch (Exception e) {
            log.error("Toggle answer like failed for responseId: {}", request.getResponseId(), e);
            throw new GrpcException(GrpcResponseErrorCode.TOGGLE_ANSWER_LIKE_FAILED, e.getMessage());
        }
    }

    public UpdateResponseResponse updateResponse(UpdateResponseRequest request) {
        try {
            Response response = responseRepository.findById(request.getResponseId())
                    .orElseThrow(() -> new GrpcException(GrpcResponseErrorCode.NOT_FOUND_RESPONSE));

            if (Boolean.TRUE.equals(response.getResponseAdopt()))
                throw new GrpcException(GrpcResponseErrorCode.ALREADY_RESPONSE_ADOPTED);

            response.updateContent(request.getContent());
            responseRepository.save(response);

            return UpdateResponseResponse.newBuilder()
                    .setResponseId(response.getResponseId())
                    .setContent(response.getResponseContent())
                    .build();
        } catch (GrpcException e) {
            throw new GrpcException(GrpcResponseErrorCode.UPDATE_RESPONSE_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Update response failed for responseId: {}", request.getResponseId(), e);
            throw new GrpcException(GrpcResponseErrorCode.UPDATE_RESPONSE_FAILED, e.getMessage());
        }
    }

    public AnswerAdoptResponse answerAdopt(AnswerAdoptRequest request) {
        try {
            Response adoptedResponse = adoptResponse(request.getResponseId());
            markQuestionAsAdopted(adoptedResponse);
            tryToSendAdoptionNotification(adoptedResponse);

            return responseGrpcMapper.getAnswerAdoptResponse(adoptedResponse);
        } catch (GrpcException e) {
            throw new GrpcException(GrpcResponseErrorCode.ANSWER_ADOPT_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Answer adopt failed for responseId: {}", request.getResponseId(), e);
            throw new GrpcException(GrpcResponseErrorCode.ANSWER_ADOPT_FAILED, e.getMessage());
        }
    }

    public void deleteResponse(DeleteResponseRequest request) {
        try {
            Response response = responseRepository.findById(request.getResponseId())
                    .orElseThrow(() -> new GrpcException(GrpcResponseErrorCode.NOT_FOUND_RESPONSE));

            if (Boolean.TRUE.equals(response.getResponseAdopt()))
                throw new GrpcException(GrpcResponseErrorCode.ALREADY_RESPONSE_ADOPTED);

            // FollowUpRoom이 있다면 먼저 삭제
            followUpRoomRepository.findByResponse(response)
                    .ifPresent(followUpRoomRepository::delete);

            responseRepository.delete(response);
        } catch (GrpcException e) {
            throw new GrpcException(GrpcResponseErrorCode.DELETE_RESPONSE_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Delete response failed for responseId: {}", request.getResponseId(), e);
            throw new GrpcException(GrpcResponseErrorCode.DELETE_RESPONSE_FAILED, e.getMessage());
        }
    }

    public AnswerReportResponse answerReport(AnswerReportRequest request) {
        try {
            Response response = responseRepository.findById(request.getResponseId())
                    .orElseThrow(() -> new GrpcException(GrpcResponseErrorCode.NOT_FOUND_RESPONSE));
            ResponseReport responseReport = ResponseReport.from(request);

            ResponseReport savedResponseReport = responseReportRepository.save(responseReport);
            userGrpcClient.increaseReportCount(response.getResponseWriterId());
            return responseGrpcMapper.getAnswerReportResponse(savedResponseReport);
        } catch (GrpcException e) {
            throw new GrpcException(GrpcResponseErrorCode.ANSWER_REPORT_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Answer report failed for responseId: {}", request.getResponseId(), e);
            throw new GrpcException(GrpcResponseErrorCode.ANSWER_REPORT_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetDetailResponseResponse getDetailResponse(GetDetailResponseRequest request) {
        try {
            List<ResponseDetail> responseDetails = buildResponseDetail(request);
            PageRequest pageRequest = PageRequest.of(request.getPageNum(), request.getSize());
            Page<Response> questions = responseRepository.findAllByQuestionId(request.getQuestionId(), pageRequest);

            return GetDetailResponseResponse.newBuilder()
                    .addAllResponses(responseDetails)
                    .setHasNext(questions.hasNext())
                    .setCurrentPage(questions.getNumber() + 1)
                    .setTotalPageNum(questions.getTotalPages())
                    .build();
        } catch (Exception e) {
            log.error("Get detail response failed for questionId: {}", request.getQuestionId(), e);
            throw new GrpcException(GrpcResponseErrorCode.GET_DETAIL_RESPONSE_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetAiBestResponseResponse getAiBestResponse() {
        List<AiBestResponseDto> aiBestResponseTop5 = responseRepository.findAiBestResponseTop5();
        List<AiBestResponse> itemList = aiBestResponseTop5.stream()
                .map(AiBestResponseDto::toGrpc)
                .toList();

        return GetAiBestResponseResponse.newBuilder()
                .addAllAiBestResponse(itemList)
                .build();
    }

    private Authority getResponseAuthority(Response response, GetDetailResponseRequest request) {
        boolean isSameUser = Objects.equals(response.getResponseId(), request.getUserId());
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));
        return Authority.newBuilder()
                .setCanAdopt(!question.getQuestionAnswerAdopt())
                .setCanDelete(isSameUser)
                .setCanModify(isSameUser)
                .build();
    }

    private Response adoptResponse(Long responseId) {
        Response response = responseRepository.findById(responseId)
                .orElseThrow(() -> new GrpcException(GrpcResponseErrorCode.NOT_FOUND_RESPONSE));

        validateQuestionNotAlreadyAdopted(response.getQuestionId());
        response.updateResponseAdopt();
        return responseRepository.save(response);
    }

    private void validateQuestionNotAlreadyAdopted(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));

        if (Boolean.TRUE.equals(question.getQuestionAnswerAdopt())) {
            throw new GrpcException(GrpcQuestionErrorCode.EXIST_ADOPTED_RESPONSE);
        }
    }

    private void tryToSendAdoptionNotification(Response response) {
        try {
            Question question = questionRepository.findById(response.getQuestionId())
                    .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));

            String body = truncateContent(response.getResponseContent());
            SendNotificationRequest notificationRequest =
                    notificationGrpcMapper.getSendNotificationRequest(body, "ANSWER_ADOPTED", question);
            notificationGrpcClient.sendNotification(notificationRequest);
        } catch (Exception e) {
            log.error("Failed to send adoption notification for response {}: {}",
                    response.getResponseId(), e.getMessage(), e);
        }
    }

    private void markQuestionAsAdopted(Response response) {
        Question question = questionRepository.findById(response.getQuestionId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));

        question.updateAnswerAdopt();
        questionRepository.save(question);
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

    private boolean handleLikeToggle(Optional<ResponseLike> existingLike, AnswerRecommendRequest request) {
        if (existingLike.isPresent()) {
            responseLikeRepository.delete(existingLike.get());
            return false; // 좋아요 취소됨
        }

        ResponseLike newLike = ResponseLike.from(request);
        responseLikeRepository.save(newLike);
        return true; // 새로운 좋아요
    }

    private List<String> processAnswerImages(AnswerCreateRequest answerCreateRequest, Response savedResponse) {
        List<String> imageUrls = null;
        if (!answerCreateRequest.getImagesList().isEmpty()) {
            imageUrls = fileUploadUtil.uploadImages(answerCreateRequest.getImagesList(), RESPONSE_FOLDER);
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

    private List<ResponseDetail> buildResponseDetail(GetDetailResponseRequest request) {
        List<Response> responses = new ArrayList<>();

        // 1. 채택된 답변 조회 (첫 페이지에만)
        if (request.getPageNum() == 0) {
            Optional<Response> adoptedResponse = responseRepository.findByQuestionIdAndResponseAdoptTrue(request.getQuestionId());
            if (adoptedResponse.isPresent()) {
                responses.add(adoptedResponse.get());
            }
        }

        // 2. 일반 답변 조회 (채택된 답변 제외)
        int size = request.getPageNum() == 0 ? 4 : 5;
        PageRequest pageRequest = PageRequest.of(request.getPageNum(), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<Response> responseSlice = responseRepository.findAllByQuestionIdAndResponseAdoptFalse(request.getQuestionId(), pageRequest);

        // 일반 답변을 리스트에 추가
        responses.addAll(responseSlice.getContent());

        if (responses.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 배치로 필요한 데이터 미리 조회 (N+1 문제 해결)
        Map<Long, List<String>> responseImageUrlsMap = getResponseImageUrlsMap(responses);
        Map<Long, Integer> likeCountMap = getLikeCountMap(responses);
        Map<Long, UpdateAdditionalUserInfoResponse> writerNameProfileMap = getWriterNameAndProfileMap(responses);

        // 4. ResponseDetail 생성
        return responses.stream()
                .map(response -> responseGrpcMapper.getResponseDetail(
                        response,
                        responseImageUrlsMap.getOrDefault(response.getResponseId(), Collections.emptyList()),
                        likeCountMap.getOrDefault(response.getResponseId(), 0),
                        writerNameProfileMap.getOrDefault(response.getResponseWriterId(), UpdateAdditionalUserInfoResponse.newBuilder()
                                .setUserName("UNDEFINED")
                                .build()),
                        getResponseAuthority(response, request)

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

    private Map<Long, UpdateAdditionalUserInfoResponse> getWriterNameAndProfileMap(List<Response> responses) {
        Set<Long> writerIds = responses.stream()
                .map(Response::getResponseWriterId)
                .collect(toSet());

        GetUsersNameAndProfileResponse usersNameAndProfile = userGrpcClient.getUsersNameAndProfile(new HashSet<>(writerIds));
        return usersNameAndProfile.getUserInfoList().stream()
                .collect(toMap(
                        UpdateAdditionalUserInfoResponse::getUserId,
                        Function.identity()
                ));
    }
}

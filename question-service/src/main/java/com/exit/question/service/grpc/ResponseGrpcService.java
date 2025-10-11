package com.exit.question.service.grpc;

import com.exit.common.grpc.AnswerAdoptResponse;
import com.exit.common.grpc.AnswerReportResponse;
import com.exit.common.grpc.ResponseServiceGrpc;
import com.exit.question.service.ResponseService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ResponseGrpcService extends ResponseServiceGrpc.ResponseServiceImplBase {
    private final ResponseService responseService;

    @Override
    public void answerReport(com.exit.common.grpc.AnswerReportRequest request,
                             StreamObserver<AnswerReportResponse> responseObserver) {
        try {
            log.info("Answer report request received for response ID: {}", request.getResponseId());

            AnswerReportResponse response = responseService.answerReport(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Answer report failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("답변 신고 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void answerAdopt(com.exit.common.grpc.AnswerAdoptRequest request,
                            StreamObserver<com.exit.common.grpc.AnswerAdoptResponse> responseObserver) {
        try {
            log.info("Answer adopt request received for response ID: {}", request.getResponseId());
            AnswerAdoptResponse response = responseService.answerAdopt(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Answer adopt failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("답변 채택 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }
}
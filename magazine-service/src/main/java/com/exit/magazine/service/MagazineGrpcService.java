package com.exit.magazine.service;

import com.exit.common.grpc.GetMagazineResponse;
import com.exit.common.grpc.GetMagazinesByCategoryResponse;
import com.exit.common.grpc.MagazineServiceGrpc;
import com.exit.common.grpc.QuestionServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class MagazineGrpcService extends MagazineServiceGrpc.MagazineServiceImplBase {

    private final MagazineService magazineService;

    @Override
    public void getMagazinesByCategory(com.exit.common.grpc.GetMagazinesByCategoryRequest request,
                                       StreamObserver<GetMagazinesByCategoryResponse> responseObserver) {
        try {
            log.info("Get Magazine List request received: {}", request.getCategoryId());
            GetMagazinesByCategoryResponse response = magazineService.getMagazinesByCategory(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Get Magazine List failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("매거진 카테고리별 조회 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void getMagazine(com.exit.common.grpc.GetMagazineRequest request,
                            StreamObserver<GetMagazineResponse> responseObserver) {
        try {
            log.info("Get Magazine request received: {}", request.getMagazineId());
            GetMagazineResponse response = magazineService.getMagazine(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Get Magazine failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("매거진 상세 조회 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }
}

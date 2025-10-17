package com.exit.magazine.service;

import com.exit.common.grpc.*;
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
            log.info("Get Magazine List request received: {}", request.getCategoryId());
            GetMagazinesByCategoryResponse response = magazineService.getMagazinesByCategory(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }

    @Override
    public void getMagazine(com.exit.common.grpc.GetMagazineRequest request,
                            StreamObserver<GetMagazineResponse> responseObserver) {
            log.info("Get Magazine request received: {}", request.getMagazineId());
            GetMagazineResponse response = magazineService.getMagazine(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }

    @Override
    public void scrapMagazine(com.exit.common.grpc.ScrapMagazineRequest request,
                            StreamObserver<ScrapMagazineResponse> responseObserver) {
            log.info("Scrap Magazine request received: {}", request.getUserId());
            ScrapMagazineResponse response = magazineService.scrapMagazine(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }

    @Override
    public void getScrapBox(com.exit.common.grpc.GetScrapBoxRequest request,
                            StreamObserver<GetScrapBoxResponse> responseObserver) {
            log.info("Get Magazine Scrap Box request received: {}", request.getUserId());
            GetScrapBoxResponse response = magazineService.getScrapBox(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }

    @Override
    public void getRecommendedMagazine(com.exit.common.grpc.GetRecommendedMagazineRequest request,
                            StreamObserver<GetRecommendedMagazineResponse> responseObserver) {
            log.info("Get Recommended Magazine request received: {}", request.getUserId());
            GetRecommendedMagazineResponse response = magazineService.getRecommendedMagazine(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }
}

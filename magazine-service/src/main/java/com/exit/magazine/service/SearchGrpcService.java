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
public class SearchGrpcService extends SearchServiceGrpc.SearchServiceImplBase {

    private final SearchService searchService;

    @Override
    public void searchMagazines(SearchMagazinesRequest request,
                                StreamObserver<SearchMagazinesResponse> responseObserver) {
        try {
            log.info("Search magazines request received for keyword: {}", request.getKeyword());
            SearchMagazinesResponse response = searchService.searchMagazines(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Search magazines failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("매거진 검색 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void searchQuestions(SearchQuestionsRequest request,
                                StreamObserver<SearchQuestionsResponse> responseObserver) {
        // This method is not implemented in magazine-service
        // Question search should be handled by question-service
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("This service does not handle question search")
                .asRuntimeException());
    }
}

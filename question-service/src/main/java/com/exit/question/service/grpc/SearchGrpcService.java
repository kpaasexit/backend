package com.exit.question.service.grpc;

import com.exit.common.grpc.*;
import com.exit.question.service.SearchService;
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
    public void searchQuestions(SearchQuestionsRequest request,
                                StreamObserver<SearchQuestionsResponse> responseObserver) {
        try {
            log.info("Search questions request received for keyword: {}", request.getKeyword());
            SearchQuestionsResponse response = searchService.searchQuestions(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Search questions failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("질문 검색 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void searchMagazines(SearchMagazinesRequest request,
                                StreamObserver<SearchMagazinesResponse> responseObserver) {
        // This method is not implemented in question-service
        // Magazine search should be handled by magazine-service
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("This service does not handle magazine search")
                .asRuntimeException());
    }
}

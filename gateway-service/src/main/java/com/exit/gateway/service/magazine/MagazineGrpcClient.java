package com.exit.gateway.service.magazine;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.magazine.dto.response.MagazineItemDto;
import com.exit.gateway.controller.magazine.dto.response.MagazineItemListDto;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MagazineGrpcClient {
    @GrpcClient("magazine-service")
    private MagazineServiceGrpc.MagazineServiceBlockingStub magazineServiceStub;

    public MagazineItemListDto getMagazinesByCategory(GetMagazinesByCategoryRequest request) {
        try {
            log.debug("Sending getMagazinesByCategory request via gRPC: categoryId={}, pageNum={}",
                    request.getCategoryId(), request.getPageNum());
            GetMagazinesByCategoryResponse response = magazineServiceStub.getMagazinesByCategory(request);
            log.debug("Received getMagazinesByCategory response via gRPC: {} magazines found",
                    response.getMagazineItemCount());

            return MagazineItemListDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC getMagazinesByCategory failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public MagazineItemDto getMagazine(GetMagazineRequest request) {
        try {
            log.debug("Sending getMagazine request via gRPC: magazineId={}", request.getMagazineId());
            GetMagazineResponse response = magazineServiceStub.getMagazine(request);
            log.debug("Received getMagazine response via gRPC: magazineId={}",
                    response.getMagazineItem().getMagazineId());

            return MagazineItemDto.from(response.getMagazineItem());
        } catch (StatusRuntimeException e) {
            log.error("gRPC getMagazine failed: {}", e.getStatus(), e);
            throw e;
        }
    }
}

package com.exit.magazine.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.magazine.domain.Magazines;
import com.exit.magazine.domain.Repository.MagazineRepository;
import com.exit.magazine.exception.GrpcMagazineErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.exit.common.util.time.TimeStampUtil.toGrpcTimestamp;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MagazineService {
    private final MagazineRepository magazineRepository;
    private final UserGrpcClient  userGrpcClient;

    public GetMagazinesByCategoryResponse getMagazinesByCategory(GetMagazinesByCategoryRequest request) {
        PageRequest pageRequest = PageRequest.of(request.getPageNum(), 5);
        List<MagazineItem> magazineItems = magazineRepository.findAllByMagazineCategoryMagazineCategoryId(request.getCategoryId(), pageRequest)
                .stream()
                .map(magazine -> {
                    UpdateAdditionalUserInfoResponse userInfo = userGrpcClient.getUserNameAndProfile(magazine.getMagazineAuthorId());
                    return createMagazineItem(magazine, userInfo);
                })
                .toList();
        return GetMagazinesByCategoryResponse.newBuilder()
                .addAllMagazineItem(magazineItems)
                .build();
    }

    public GetMagazineResponse getMagazine(GetMagazineRequest request) {
        Magazines magazine = magazineRepository.findById(request.getMagazineId())
                .orElseThrow(() -> new GrpcException(GrpcMagazineErrorCode.NULL_MAGAZINE));
        UpdateAdditionalUserInfoResponse userInfo = userGrpcClient.getUserNameAndProfile(magazine.getMagazineAuthorId());
        MagazineItem magazineItem = createMagazineItem(magazine, userInfo);

        return GetMagazineResponse.newBuilder()
                .setMagazineItem(magazineItem)
                .build();
    }

    private MagazineItem createMagazineItem(Magazines magazine, UpdateAdditionalUserInfoResponse userInfo) {
        return MagazineItem.newBuilder()
                .setMagazineId(magazine.getMagazineId())
                .setMagazineCategoryId(magazine.getMagazineCategory().getMagazineCategoryId())
                .setMagazineTitle(magazine.getMagazineTitle())
                .setMagazineSubtitle(magazine.getMagazineSubtitle())
                .setMagazineContent(magazine.getMagazineContent())
                .setMagazineAuthor(userInfo.getUserName())
                .setAuthorProfileUrl(userInfo.getUserProfile())
                .setMagazineThumbnailUrl(magazine.getMagazineThumbnailUrl())
                .setCreatedAt(toGrpcTimestamp(magazine.getCreatedAt()))
                .build();
    }
}

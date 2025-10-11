package com.exit.magazine.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.magazine.domain.MagazineScraps;
import com.exit.magazine.domain.Magazines;
import com.exit.magazine.domain.Repository.MagazineRepository;
import com.exit.magazine.domain.Repository.MagazineScrapRepository;
import com.exit.magazine.exception.GrpcMagazineErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.exit.common.util.time.TimeStampUtil.toGrpcTimestamp;

@Service
@RequiredArgsConstructor
@Transactional
public class MagazineService {
    private final MagazineRepository magazineRepository;
    private final MagazineScrapRepository magazineScrapRepository;
    private final UserGrpcClient  userGrpcClient;

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public GetMagazineResponse getMagazine(GetMagazineRequest request) {
        Magazines magazine = magazineRepository.findById(request.getMagazineId())
                .orElseThrow(() -> new GrpcException(GrpcMagazineErrorCode.NULL_MAGAZINE));
        UpdateAdditionalUserInfoResponse userInfo = userGrpcClient.getUserNameAndProfile(magazine.getMagazineAuthorId());
        MagazineItem magazineItem = createMagazineItem(magazine, userInfo);

        return GetMagazineResponse.newBuilder()
                .setMagazineItem(magazineItem)
                .build();
    }

    public ScrapMagazineResponse scrapMagazine(ScrapMagazineRequest request) {

        Optional<MagazineScraps> existingScrap = magazineScrapRepository.findByMagazine_MagazineIdAndUserId(request.getMagazineId(), request.getUserId());
        boolean isScrapped = handleScrapToggle(existingScrap, request);

        return ScrapMagazineResponse.newBuilder()
                .setMagazineId(request.getMagazineId())
                .setIsScrapped(isScrapped)
                .build();
    }

    @Transactional(readOnly = true)
    public GetScrapBoxResponse getScrapBox(GetScrapBoxRequest request) {
        PageRequest pageRequest = PageRequest.of(request.getPageNum(), 5);
        Slice<MagazineScraps> slice = magazineScrapRepository.findByUserId(request.getUserId(), pageRequest);
        List<Magazines> magazines = slice.getContent().stream()
                .map(MagazineScraps::getMagazine)
                .toList();

        List<MagazineScrapBoxItem> magazineScrapBoxItems = magazines.stream().map(magazine -> MagazineScrapBoxItem.newBuilder()
                .setMagazineId(magazine.getMagazineId())
                .setMagazineTitle(magazine.getMagazineTitle())
                .setMagazineSubtitle(magazine.getMagazineSubtitle())
                .setMagazineThumbnailUrl(magazine.getMagazineThumbnailUrl())
                .setCreatedAt(toGrpcTimestamp(magazine.getCreatedAt()))
                .build()).toList();

        return GetScrapBoxResponse.newBuilder()
                .addAllMagazineScrapBoxItem(magazineScrapBoxItems)
                .setHasNext(slice.hasNext())
                .build();
    }

    private boolean handleScrapToggle(Optional<MagazineScraps> existingLike, ScrapMagazineRequest request) {
        if (existingLike.isPresent()) {
            magazineScrapRepository.delete(existingLike.get());
            return false; // 스크랩 취소됨
        }

        Magazines magazine = magazineRepository.findById(request.getMagazineId())
                .orElseThrow(() -> new GrpcException(GrpcMagazineErrorCode.NULL_MAGAZINE));
        MagazineScraps magazineScrap = MagazineScraps.from(magazine, request.getUserId());
        magazineScrapRepository.save(magazineScrap);
        return true; // 새로운 스크랩
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

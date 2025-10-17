package com.exit.magazine.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.magazine.domain.MagazineScraps;
import com.exit.magazine.domain.Magazines;
import com.exit.magazine.domain.Repository.MagazineRepository;
import com.exit.magazine.domain.Repository.MagazineScrapRepository;
import com.exit.magazine.exception.GrpcMagazineErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.exit.common.util.time.TimeStampUtil.toGrpcTimestamp;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MagazineService {
    private final MagazineRepository magazineRepository;
    private final MagazineScrapRepository magazineScrapRepository;
    private final UserGrpcClient userGrpcClient;

    @Transactional(readOnly = true)
    public GetMagazinesByCategoryResponse getMagazinesByCategory(GetMagazinesByCategoryRequest request) {
        try {
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
        } catch (Exception e) {
            log.error("Get magazines by category failed for categoryId: {}", request.getCategoryId(), e);
            throw new GrpcException(GrpcMagazineErrorCode.GET_MAGAZINES_BY_CATEGORY_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetMagazineResponse getMagazine(GetMagazineRequest request) {
        try {
            Magazines magazine = magazineRepository.findById(request.getMagazineId())
                    .orElseThrow(() -> new GrpcException(GrpcMagazineErrorCode.MAGAZINE_NOT_FOUND));
            UpdateAdditionalUserInfoResponse userInfo = userGrpcClient.getUserNameAndProfile(magazine.getMagazineAuthorId());
            MagazineItem magazineItem = createMagazineItem(magazine, userInfo);

            return GetMagazineResponse.newBuilder()
                    .setMagazineItem(magazineItem)
                    .build();
        } catch (GrpcException e) {
            throw new GrpcException(GrpcMagazineErrorCode.GET_MAGAZINE_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Get magazine failed for magazineId: {}", request.getMagazineId(), e);
            throw new GrpcException(GrpcMagazineErrorCode.GET_MAGAZINE_FAILED, e.getMessage());
        }
    }

    public ScrapMagazineResponse scrapMagazine(ScrapMagazineRequest request) {
        try {
            Optional<MagazineScraps> existingScrap = magazineScrapRepository.findByMagazine_MagazineIdAndUserId(request.getMagazineId(), request.getUserId());
            boolean isScrapped = handleScrapToggle(existingScrap, request);

            return ScrapMagazineResponse.newBuilder()
                    .setMagazineId(request.getMagazineId())
                    .setIsScrapped(isScrapped)
                    .build();
        } catch (GrpcException e) {
            throw new GrpcException(GrpcMagazineErrorCode.SCRAP_MAGAZINE_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Scrap magazine failed for magazineId: {}, userId: {}", request.getMagazineId(), request.getUserId(), e);
            throw new GrpcException(GrpcMagazineErrorCode.SCRAP_MAGAZINE_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetScrapBoxResponse getScrapBox(GetScrapBoxRequest request) {
        try {
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
        } catch (Exception e) {
            log.error("Get scrap box failed for userId: {}", request.getUserId(), e);
            throw new GrpcException(GrpcMagazineErrorCode.GET_SCRAP_BOX_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetRecommendedMagazineResponse getRecommendedMagazine(GetRecommendedMagazineRequest request) {
        try {
            List<Magazines> recommendedMagazine = magazineRepository.findAllByMagazineIdIn(List.of(1L, 2L, 3L, 4L, 5L));

            List<MagazineScrapBoxItem> recommendedMagazineItems = recommendedMagazine.stream().map(magazine -> MagazineScrapBoxItem.newBuilder()
                    .setMagazineId(magazine.getMagazineId())
                    .setMagazineTitle(magazine.getMagazineTitle())
                    .setMagazineSubtitle(magazine.getMagazineSubtitle())
                    .setMagazineThumbnailUrl(magazine.getMagazineThumbnailUrl())
                    .setCreatedAt(toGrpcTimestamp(magazine.getCreatedAt()))
                    .build()).toList();

            return GetRecommendedMagazineResponse.newBuilder()
                    .addAllRecommendMagazine(recommendedMagazineItems)
                    .build();
        } catch (Exception e) {
            log.error("Get recommended magazine failed", e);
            throw new GrpcException(GrpcMagazineErrorCode.GET_RECOMMENDED_MAGAZINE_FAILED, e.getMessage());
        }
    }

    private boolean handleScrapToggle(Optional<MagazineScraps> existingLike, ScrapMagazineRequest request) {
        if (existingLike.isPresent()) {
            magazineScrapRepository.delete(existingLike.get());
            return false; // 스크랩 취소됨
        }

        Magazines magazine = magazineRepository.findById(request.getMagazineId())
                .orElseThrow(() -> new GrpcException(GrpcMagazineErrorCode.MAGAZINE_NOT_FOUND));
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

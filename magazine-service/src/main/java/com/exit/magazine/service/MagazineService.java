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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            PageRequest pageRequest = PageRequest.of(request.getPageNum(), request.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
            Slice<Magazines> magazines = magazineRepository.findAllByMagazineCategoryMagazineCategoryId(request.getCategoryId(), pageRequest);
            Set<Long> authorIds = magazines.getContent().stream().map(Magazines::getMagazineAuthorId).collect(Collectors.toSet());

            GetUsersNameAndProfileResponse usersNameAndProfile = userGrpcClient.getUsersNameAndProfile(authorIds);
            Map<Long, UpdateAdditionalUserInfoResponse> authorInfoMap = usersNameAndProfile.getUserInfoList().stream()
                    .collect(Collectors.toMap(userInfo -> userInfo.getUserId(), userInfo -> userInfo));

            List<MagazineListItem> magazineListItems = magazines.getContent().stream().map(magazine -> {
                        UpdateAdditionalUserInfoResponse authorUserInfo = authorInfoMap.get(magazine.getMagazineAuthorId());
                        return createMagazineListItem(magazine, authorUserInfo);
                    }
            ).toList();
            return GetMagazinesByCategoryResponse.newBuilder()
                    .addAllMagazineItem(magazineListItems)
                    .setCurrentPage(magazines.getNumber() + 1)
                    .setHasNext(magazines.hasNext())
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
            boolean isScrap = magazineScrapRepository.findByUserIdAndMagazine_magazineId(request.getUserId(), request.getMagazineId()).isPresent();
            MagazineItem magazineItem = createMagazineItem(magazine, userInfo, isScrap);

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
            PageRequest pageRequest = PageRequest.of(request.getPageNum(), request.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
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
                    .setCurrentPage(slice.getNumber() + 1)
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

    @Transactional(readOnly = true)
    public SearchMagazinesResponse searchMagazines(SearchMagazinesRequest request) {
        try {

            log.info("Searching magazines with keyword: {}, page: {}, size: {}",
                    request.getKeyword(), request.getPage(), request.getSize());

            PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

            // keyword로 매거진 검색 (제목 + 부제목 + 내용)
            // 키워드가 비어있으면 모든 매거진 조회
            Slice<Magazines> slice;
            if (request.getKeyword().trim().isEmpty()) {
                slice = magazineRepository.findAll(pageRequest);
            } else {
                slice = magazineRepository.searchByKeyword(request.getKeyword(), pageRequest);
            }

            // 작성자 정보 조회
            Set<Long> authorIds = slice.getContent().stream()
                    .map(Magazines::getMagazineAuthorId)
                    .collect(Collectors.toSet());

            Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap = getUserInfoMap(authorIds);

            List<MagazineListItem> searchItems = slice.getContent().stream().map(
                    magazine -> createMagazineListItem(magazine, userInfoMap.get(magazine.getMagazineAuthorId()))
                    ).toList();

            return SearchMagazinesResponse.newBuilder()
                    .addAllMagazines(searchItems)
                    .setHasNext(slice.hasNext())
                    .setCurrentPage(slice.getNumber() + 1)
                    .build();
        } catch (Exception e) {
            throw new GrpcException(GrpcMagazineErrorCode.SEARCH_INTEGRATED_FAILED, e.getMessage());
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

    private MagazineItem createMagazineItem(Magazines magazine, UpdateAdditionalUserInfoResponse userInfo, Boolean isScrap) {
        return MagazineItem.newBuilder()
                .setMagazineId(magazine.getMagazineId())
                .setMagazineCategoryId(magazine.getMagazineCategory().getMagazineCategoryId())
                .setMagazineTitle(magazine.getMagazineTitle())
                .setMagazineSubtitle(magazine.getMagazineSubtitle())
                .setMagazineContent(magazine.getMagazineContent())
                .setMagazineAuthor(userInfo != null ? userInfo.getUserName() : "Unknown")
                .setAuthorProfileUrl(userInfo != null && !userInfo.getUserProfile().isEmpty() ? userInfo.getUserProfile() : "")
                .setMagazineThumbnailUrl(magazine.getMagazineThumbnailUrl() != null ? magazine.getMagazineThumbnailUrl() : "")
                .setCreatedAt(toGrpcTimestamp(magazine.getCreatedAt()))
                .setIsScrap(isScrap)
                .build();
    }

    private Map<Long, UpdateAdditionalUserInfoResponse> getUserInfoMap(Set<Long> authorIds) {
        if (authorIds.isEmpty()) {
            return Map.of();
        }

        // Batch로 사용자 정보 조회
        return authorIds.stream()
                .map(userGrpcClient::getUserNameAndProfile)
                .collect(Collectors.toMap(
                        UpdateAdditionalUserInfoResponse::getUserId,
                        Function.identity()
                ));
    }

    private MagazineListItem createMagazineListItem(Magazines magazine, UpdateAdditionalUserInfoResponse authorInfo) {
        return MagazineListItem.newBuilder()
                .setMagazineId(magazine.getMagazineId())
                .setMagazineCategoryId(magazine.getMagazineCategory().getMagazineCategoryId())
                .setMagazineTitle(magazine.getMagazineTitle())
                .setMagazineSubtitle(magazine.getMagazineSubtitle())
                .setMagazineAuthor(authorInfo != null ? authorInfo.getUserName() : "Unknown")
                .setAuthorProfileUrl(authorInfo != null && !authorInfo.getUserProfile().isEmpty() ? authorInfo.getUserProfile() : "")
                .setMagazineThumbnailUrl(magazine.getMagazineThumbnailUrl() != null ? magazine.getMagazineThumbnailUrl() : "")
                .setCreatedAt(toGrpcTimestamp(magazine.getCreatedAt()))
                .build();
    }
}

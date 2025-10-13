package com.exit.magazine.service;

import com.exit.common.grpc.*;
import com.exit.magazine.domain.Magazines;
import com.exit.magazine.domain.Repository.MagazineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.exit.common.util.time.TimeStampUtil.toGrpcTimestamp;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SearchService {

    private final MagazineRepository magazineRepository;
    private final UserGrpcClient userGrpcClient;

    public SearchMagazinesResponse searchMagazines(SearchMagazinesRequest request) {
        log.info("Searching magazines with keyword: {}, page: {}, size: {}",
                request.getKeyword(), request.getPage(), request.getSize());

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());

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

        // MagazineSearchItem 생성
        List<MagazineSearchItem> searchItems = slice.getContent().stream()
                .map(magazine -> {
                    UpdateAdditionalUserInfoResponse userInfo = userInfoMap.get(magazine.getMagazineAuthorId());
                    return MagazineSearchItem.newBuilder()
                            .setMagazineId(magazine.getMagazineId())
                            .setMagazineCategoryId(magazine.getMagazineCategory().getMagazineCategoryId())
                            .setMagazineTitle(magazine.getMagazineTitle())
                            .setMagazineSubtitle(magazine.getMagazineSubtitle())
                            .setMagazineContent(magazine.getMagazineContent())
                            .setMagazineAuthor(userInfo != null ? userInfo.getUserName() : "Unknown")
                            .setAuthorProfileUrl(userInfo != null ? userInfo.getUserProfile() : "")
                            .setMagazineThumbnailUrl(magazine.getMagazineThumbnailUrl())
                            .setCreatedAt(toGrpcTimestamp(magazine.getCreatedAt()))
                            .build();
                })
                .toList();

        return SearchMagazinesResponse.newBuilder()
                .addAllMagazines(searchItems)
                .setTotalCount(searchItems.size())
                .setHasNext(slice.hasNext())
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
}

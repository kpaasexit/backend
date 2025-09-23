package com.exit.common.file.util;

import com.exit.common.grpc.UploadBytesRequest;
import com.exit.common.properties.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadUtil {

    private final FileStorageProperties properties;

    /**
     * 단일 이미지 파일을 마운트된 NAS에 업로드
     *
     * @param request    업로드할 파일 정보
     * @param folderPath 저장할 폴더 경로 (예: "questions/images/")
     * @return 업로드된 파일의 공개 URL
     */
    public String uploadImage(UploadBytesRequest request, String folderPath) {
        validateFile(request);

        try {
            // 날짜별 폴더 구조 생성 (예: 2024/01/15/)
            String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fullFolderPath = folderPath + "/" + dateFolder;

            // 고유한 파일명 생성
            String fileName = generateUniqueFileName(request.getMeta().getFilename());

            // 전체 파일 경로 생성
            Path uploadDir = Paths.get(properties.getUploadPath(), fullFolderPath);
            Path filePath = uploadDir.resolve(fileName);

            // 디렉토리가 없으면 생성
            if (properties.getCreateDirectories()) {
                Files.createDirectories(uploadDir);
            }

            // 파일 저장
            Files.write(filePath, request.getData().toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            // 공개 URL 생성
            String relativePath = fullFolderPath + "/" + fileName;
            String fileUrl = generateFileUrl(relativePath);

            log.info("File uploaded successfully: {} -> {}", request.getMeta().getFilename(), fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload file: {}", request.getMeta().getFilename(), e);
            throw new FileUploadException.FileUploadFailedException("파일 업로드 실패", e);
        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", request.getMeta().getFilename(), e);
            throw new FileUploadException.FileUploadFailedException("파일 업로드 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * 여러 이미지 파일을 마운트된 NAS에 업로드
     *
     * @param files      업로드할 파일 목록
     * @param folderPath 저장할 폴더 경로
     * @return 업로드된 파일들의 공개 URL 목록
     */
    public List<String> uploadImages(List<UploadBytesRequest> files, String folderPath) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (UploadBytesRequest file : files) {
            if (file != null) {
                String uploadedUrl = uploadImage(file, folderPath);
                uploadedUrls.add(uploadedUrl);
            }
        }

        return uploadedUrls;
    }

    /**
     * 파일 삭제
     *
     * @param fileUrl 삭제할 파일의 URL
     */
    public void deleteFile(String fileUrl) {
        try {
            String relativePath = extractRelativePathFromUrl(fileUrl);
            Path filePath = Paths.get(properties.getUploadPath(), relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted successfully: {}", fileUrl);
            } else {
                log.warn("File not found for deletion: {}", fileUrl);
            }

        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileUrl, e);
            throw new FileUploadException.FileUploadFailedException("파일 삭제 실패", e);
        } catch (Exception e) {
            log.error("Unexpected error during file deletion: {}", fileUrl, e);
            throw new FileUploadException.FileUploadFailedException("파일 삭제 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * 여러 파일 삭제
     *
     * @param fileUrls 삭제할 파일 URL 목록
     */
    public void deleteFiles(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return;
        }

        for (String fileUrl : fileUrls) {
            try {
                deleteFile(fileUrl);
            } catch (Exception e) {
                log.warn("Failed to delete file: {}, continuing with other files", fileUrl, e);
            }
        }
    }

    /**
     * 파일 존재 여부 확인
     *
     * @param fileUrl 확인할 파일의 URL
     * @return 파일 존재 여부
     */
    public boolean fileExists(String fileUrl) {
        try {
            String relativePath = extractRelativePathFromUrl(fileUrl);
            Path filePath = Paths.get(properties.getUploadPath(), relativePath);
            return Files.exists(filePath);

        } catch (Exception e) {
            log.error("Error checking file existence: {}", fileUrl, e);
            return false;
        }
    }

    /**
     * 파일 크기 조회
     *
     * @param fileUrl 조회할 파일의 URL
     * @return 파일 크기 (바이트)
     */
    public long getFileSize(String fileUrl) {
        try {
            String relativePath = extractRelativePathFromUrl(fileUrl);
            Path filePath = Paths.get(properties.getUploadPath(), relativePath);

            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }
            return 0;

        } catch (IOException e) {
            log.error("Error getting file size: {}", fileUrl, e);
            return 0;
        }
    }

    private void validateFile(UploadBytesRequest request) {
        if (request.getData().isEmpty()) {
            throw new FileUploadException("업로드할 파일이 없습니다.");
        }

        if (!FileValidationUtils.isValidImageFile(request)) {
            throw new FileUploadException.InvalidFileTypeException("지원하지 않는 파일 형식입니다. (지원 형식: JPG, PNG, GIF, BMP, WEBP)");
        }
    }

    private String generateUniqueFileName(String originalFilename) {
        return FileValidationUtils.generateUniqueFileName(originalFilename);
    }

    private String generateFileUrl(String relativePath) {
        return properties.getBaseUrl() + "/" + relativePath;
    }

    private String extractRelativePathFromUrl(String fileUrl) {
        if (fileUrl.startsWith(properties.getBaseUrl())) {
            return fileUrl.substring(properties.getBaseUrl().length() + 1);
        }
        throw new IllegalArgumentException("Invalid file URL format: " + fileUrl);
    }
}
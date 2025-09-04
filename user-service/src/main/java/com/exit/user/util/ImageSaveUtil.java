package com.exit.user.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class ImageSaveUtil {

    @Value("${app.nsa.mount-path:/mnt/nsa-storage}")
    private String nsaMountPath;

    @Value("${app.image.user-profile-path:/private/users/profiles}")
    private String userProfilePath;

    public String processProfileImage(byte[] imageData, String originalFileName, String userEmail) {
        try {
            // 이미지 데이터 검증
            validateImageData(imageData);

            // 파일 확장자 추출 및 검증
            String extension = getFileExtension(originalFileName);
            validateImageExtension(extension);

            // 이미지 형식 검증 (Magic Number로 실제 이미지인지 확인)
            validateImageFormat(imageData, extension);

            // 고유한 파일명 생성
            String fileName = generateUniqueFileName(userEmail, extension);

            // NSA 마운트 경로에 파일 저장
            String relativePath = userProfilePath + "/" + fileName;
            String fullPath = nsaMountPath + relativePath;

            // 디렉토리 생성
            createDirectoryIfNotExists(fullPath);

            // byte[] 배열을 파일로 저장
            Files.write(Paths.get(fullPath), imageData);

            return relativePath; // DB에는 상대 경로만 저장

        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 저장 실패", e);
        }
    }

    private void validateImageData(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("이미지 데이터가 비어있습니다.");
        }

        // 파일 크기 제한 (5MB)
        if (imageData.length > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("이미지 크기가 5MB를 초과할 수 없습니다.");
        }
    }

    private void validateImageFormat(byte[] imageData, String extension) {
        // Magic Number로 실제 이미지 형식 검증
        if (imageData.length < 4) {
            throw new IllegalArgumentException("올바르지 않은 이미지 데이터입니다.");
        }

        // JPEG 검증: FF D8 FF
        if (extension.equals("jpg") || extension.equals("jpeg")) {
            if (!(imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8 && imageData[2] == (byte) 0xFF)) {
                throw new IllegalArgumentException("올바르지 않은 JPEG 이미지입니다.");
            }
        }
        // PNG 검증: 89 50 4E 47
        else if (extension.equals("png")) {
            if (!(imageData[0] == (byte) 0x89 && imageData[1] == 0x50 &&
                    imageData[2] == 0x4E && imageData[3] == 0x47)) {
                throw new IllegalArgumentException("올바르지 않은 PNG 이미지입니다.");
            }
        }
        // GIF 검증: 47 49 46
        else if (extension.equals("gif")) {
            if (!(imageData[0] == 0x47 && imageData[1] == 0x49 && imageData[2] == 0x46)) {
                throw new IllegalArgumentException("올바르지 않은 GIF 이미지입니다.");
            }
        }
    }

    private String generateUniqueFileName(String userEmail, String extension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String emailHash = Integer.toHexString(userEmail.hashCode());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("profile_%s_%s_%s.%s", emailHash, timestamp, uuid, extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private void validateImageExtension(String extension) {
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다: " + extension);
        }
    }

    private void createDirectoryIfNotExists(String filePath) throws IOException {
        Path directory = Paths.get(filePath).getParent();
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }
}

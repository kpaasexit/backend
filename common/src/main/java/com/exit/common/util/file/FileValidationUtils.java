package com.exit.common.util.file;

import com.exit.common.grpc.UploadBytesRequest;
import com.google.protobuf.ByteString;

import java.util.Arrays;
import java.util.List;

public class FileValidationUtils {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    public static boolean isValidImageFile(UploadBytesRequest request) {
        ByteString data = request.getData();
        String fileName = request.getMeta().getFilename();
        String contentType = request.getMeta().getContentType();
        if (data.isEmpty()) {
            return false;
        }

        // 파일 크기 검증
        if (data.size() > MAX_FILE_SIZE) {
            return false;
        }

        // MIME 타입 검증
        if (contentType.isEmpty() || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            return false;
        }

        // 파일 확장자 검증
        if (fileName.isEmpty()) {
            return false;
        }

        return isImageFile(fileName);
    }

    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public static String generateUniqueFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomSuffix = String.valueOf((int) (Math.random() * 10000));

        return timestamp + "_" + randomSuffix + "." + extension;
    }

    public static boolean isImageFile(String filename) {
        String extension = getFileExtension(filename);
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }
}
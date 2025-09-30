package com.exit.common.util.file;

public class FileUploadException extends RuntimeException {

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class InvalidFileTypeException extends FileUploadException {
        public InvalidFileTypeException(String message) {
            super(message);
        }
    }

    public static class FileSizeExceededException extends FileUploadException {
        public FileSizeExceededException(String message) {
            super(message);
        }
    }

    public static class FileUploadFailedException extends FileUploadException {
        public FileUploadFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
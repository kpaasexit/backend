package com.exit.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file.storage")
@Getter
@Setter
public class FileStorageProperties {
    @Value("${app.file-dir}")
    private String uploadPath;
    private String baseUrl = "https://file.house-it.210-178-1-144.nip.io/files";
    private Boolean createDirectories = true;
}

// 包声明：存储服务所在包
package com.lifereview.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** 本地文件存储：将文件保存到 app.upload.local-path 目录，返回 public-prefix + 文件名 */
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.upload.local-path:./uploads}")
    private String localPath;

    @Value("${app.upload.public-prefix:/uploads}")
    private String publicPrefix;

    @Override
    public String store(MultipartFile file) {
        try {
            Path folder = Path.of(localPath).toAbsolutePath().normalize();
            Files.createDirectories(folder);
            String filename = buildFilename(file.getOriginalFilename());
            Path target = folder.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return publicPrefix + "/" + filename;
        } catch (IOException e) {
            throw new IllegalArgumentException("文件上传失败");
        }
    }

    private String buildFilename(String originalFilename) {
        String ext = "";
        if (originalFilename != null) {
            int idx = originalFilename.lastIndexOf('.');
            if (idx >= 0) {
                ext = originalFilename.substring(idx);
            }
        }
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }
}

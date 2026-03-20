// 包声明：存储服务所在包
package com.lifereview.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** OSS 模式存储：示例实现，返回 base-url + 文件名。可接入阿里云 OSS/MinIO SDK 实现真实上传 */
@Service
public class OssFileStorageService implements FileStorageService {

    @Value("${app.oss.base-url}")
    private String baseUrl;

    @Override
    public String store(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        return baseUrl.endsWith("/") ? baseUrl + filename : baseUrl + "/" + filename;
    }
}

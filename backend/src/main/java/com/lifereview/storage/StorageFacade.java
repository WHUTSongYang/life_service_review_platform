// 包声明：存储服务所在包
package com.lifereview.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 文件上传门面。根据 app.upload.mode 选择 local 或 oss 实现 */
@Service
@RequiredArgsConstructor
public class StorageFacade {
    private final LocalFileStorageService localFileStorageService;
    private final OssFileStorageService ossFileStorageService;

    @Value("${app.upload.mode:local}")
    private String mode;

    public String upload(MultipartFile file) {
        if ("oss".equalsIgnoreCase(mode)) {
            return ossFileStorageService.store(file);
        }
        return localFileStorageService.store(file);
    }
}

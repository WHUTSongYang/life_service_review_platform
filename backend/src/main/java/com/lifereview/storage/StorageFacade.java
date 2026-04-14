package com.lifereview.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传门面服务。
 * <p>
 * 根据 {@code app.upload.mode} 在本地存储与 OSS 示例实现之间路由，对外统一 {@link #upload} 入口。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class StorageFacade {

    /** 本地磁盘存储实现 */
    private final LocalFileStorageService localFileStorageService;

    /** 对象存储（示例）实现 */
    private final OssFileStorageService ossFileStorageService;

    /** 存储模式：{@code local} 或 {@code oss}（大小写不敏感） */
    @Value("${app.upload.mode:local}")
    private String mode;

    /**
     * 按配置选择具体存储实现并保存文件。
     *
     * @param file 上传文件
     * @return 由具体实现返回的可访问路径或 URL
     */
    public String upload(MultipartFile file) {
        if ("oss".equalsIgnoreCase(mode)) {
            return ossFileStorageService.store(file); // 走 OSS 分支
        }
        return localFileStorageService.store(file); // 默认本地
    }
}

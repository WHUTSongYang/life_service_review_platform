package com.lifereview.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 对象存储模式的 {@link FileStorageService} 占位实现。
 * <p>
 * 当前仅生成唯一文件名并拼接 {@code app.oss.base-url}，未调用真实 OSS SDK；
 * 可在此基础上接入阿里云 OSS、MinIO 等完成实际上传。
 * </p>
 */
@Service
public class OssFileStorageService implements FileStorageService {

    /** OSS 对外访问基地址，末尾可有或可无 {@code /} */
    @Value("${app.oss.base-url}")
    private String baseUrl;

    /**
     * 生成 UUID 文件名并拼接基地址作为返回 URL（不执行网络上载）。
     *
     * @param file 上传文件（当前实现仅读取原始文件名以保留扩展名）
     * @return 拼接后的访问 URL 字符串
     */
    @Override
    public String store(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        return baseUrl.endsWith("/") ? baseUrl + filename : baseUrl + "/" + filename; // 避免双斜杠或缺斜杠
    }
}

package com.lifereview.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 基于本地文件系统的 {@link FileStorageService} 实现。
 * <p>
 * 将文件写入 {@code app.upload.local-path}，对外访问路径为 {@code app.upload.public-prefix} 与文件名的组合。
 * </p>
 */
@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    /** 本地存储根目录 */
    @Value("${app.upload.local-path:./uploads}")
    private String localPath;

    /** 对外 URL 路径前缀，需与 {@link com.lifereview.config.WebMvcConfig} 静态映射一致 */
    @Value("${app.upload.public-prefix:/uploads}")
    private String publicPrefix;

    /**
     * 将 {@code MultipartFile} 拷贝到本地目录，文件名使用 UUID 保留扩展名。
     *
     * @param file 上传文件
     * @return {@code publicPrefix/文件名} 形式的访问路径
     * @throws IllegalArgumentException 磁盘 IO 失败时包装为业务异常抛出
     */
    @Override
    public String store(MultipartFile file) {
        try {
            Path folder = Path.of(localPath).toAbsolutePath().normalize();
            Files.createDirectories(folder); // 确保目录存在
            String filename = buildFilename(file.getOriginalFilename());
            Path target = folder.resolve(filename);
            //2026.4.14sy新增 输出保存图片的路径
            log.info(target.toString());
            //保存图片到指定路径
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING); // 覆盖同名
            return publicPrefix + "/" + filename;
        } catch (IOException e) {
            throw new IllegalArgumentException("文件上传失败");
        }
    }

    /**
     * 根据原始文件名提取扩展名，并与无横线的 UUID 拼接生成存储文件名。
     *
     * @param originalFilename 客户端提供的原始文件名，可为 {@code null}
     * @return 存储用唯一文件名
     */
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

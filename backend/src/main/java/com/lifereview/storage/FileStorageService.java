package com.lifereview.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务契约。
 * <p>
 * 将上传的多部分文件持久化到具体介质（本地磁盘、对象存储等），并返回客户端可访问的 URL 或路径。
 * </p>
 */
public interface FileStorageService {

    /**
     * 保存上传文件并返回可访问地址（相对路径或绝对 URL，由实现决定）。
     *
     * @param file 浏览器或客户端上传的 {@link MultipartFile}
     * @return 可用于前端展示或下载的资源路径/URL
     */
    String store(MultipartFile file);

}

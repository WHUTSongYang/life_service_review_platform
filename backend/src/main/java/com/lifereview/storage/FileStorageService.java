// 包声明：存储服务所在包
package com.lifereview.storage;

import org.springframework.web.multipart.MultipartFile;

/** 文件存储服务接口。store 方法将上传文件持久化并返回可访问 URL */
public interface FileStorageService {

    String store(MultipartFile file);  // 存储文件，返回可访问 URL

}

package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.storage.StorageFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传控制器。
 * 支持图片上传，根据配置使用本地存储或 OSS。
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageFacade storageFacade;

    // 上传文件，返回访问 URL
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        String url = storageFacade.upload(file);
        return ApiResponse.ok(Map.of("url", url));
    }
}

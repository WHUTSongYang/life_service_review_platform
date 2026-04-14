package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.storage.StorageFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传控制器。
 * <p>URL 前缀：{@code /api/files}。根据配置经 {@link StorageFacade} 写入本地或 OSS，访问策略以存储实现为准。</p>
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    /** 统一文件存储门面（本地/OSS 等） */
    private final StorageFacade storageFacade;

    /**
     * 上传单个文件并返回可访问 URL。
     *
     * @param file 表单字段名为 {@code file} 的二进制内容
     * @return 包含 {@code url} 的 Map
     * @throws IllegalArgumentException 未选择文件或上传失败
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        String url = storageFacade.upload(file);
        return ApiResponse.ok(Map.of("url", url));
    }
}

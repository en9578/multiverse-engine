package com.minbao.multiverse.controller;

import com.minbao.multiverse.common.BusinessException;
import com.minbao.multiverse.common.Result;
import com.minbao.multiverse.enums.ErrorCodeEnum;
import com.minbao.multiverse.manager.OssManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {
    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    @Resource
    private OssManager ossManager;

    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAM, "文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCodeEnum.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCodeEnum.FILE_TYPE_UNSUPPORTED);
        }
        try {
            String url = ossManager.upload(file.getBytes(), file.getOriginalFilename());
            log.info("文件上传成功 url={}", url);
            return Result.ok(Map.of("url", url));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "文件上传失败");
        }
    }
}
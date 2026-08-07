package com.minbao.multiverse.manager.impl;

import com.minbao.multiverse.manager.OssManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OssManagerImpl implements OssManager {
    private static final Logger log = LoggerFactory.getLogger(OssManagerImpl.class);

    @Override
    public String upload(byte[] data, String fileName) {
        log.info("OSS上传 fileName={} size={}bytes", fileName, data.length);
        // 骨架：实际应调用阿里云OSS SDK上传，返回公网URL
        String ext = fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf("."))
                : "";
        return "https://oss.example.com/multiverse/" + UUID.randomUUID() + ext;
    }

    @Override
    public byte[] download(String url) {
        log.info("OSS下载 url={}", url);
        // 骨架：实际应调用阿里云OSS SDK下载
        return new byte[0];
    }
}
package com.minbao.multiverse.manager.impl;

import com.minbao.multiverse.manager.BailianManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BailianManagerImpl implements BailianManager {
    private static final Logger log = LoggerFactory.getLogger(BailianManagerImpl.class);

    @Override
    public String generateText(String prompt, String model) {
        log.info("百炼文本生成 model={}", model);
        return "{\"result\": \"骨架返回\"}";
    }

    @Override
    public String generateImage(String prompt) {
        log.info("百炼生图 prompt={}", prompt);
        return "";
    }

    @Override
    public String detectCompliance(String imageUrl, String prompt) {
        log.info("百炼合规检测");
        return "{\"compliance\": true}";
    }

    @Override
    public String callR1(String prompt) {
        log.info("百炼R1推理");
        return "{\"reasoning\": \"骨架返回\"}";
    }

    @Override
    public String generateVideo(String prompt) {
        log.info("百炼视频生成");
        return "";
    }

    @Override
    public String tts(String text) {
        log.info("百炼TTS");
        return "";
    }
}

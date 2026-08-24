package com.minbao.multiverse.manager;

import com.minbao.multiverse.enums.StageEnum;

/**
 * 百炼 DashScope 统一接入层。
 * 文本生成按 StageEnum 自动路由模型，其他模态（生图/VL/视频/TTS）独立方法。
 */
public interface BailianManager {
    /**
     * 统一文本生成，按 StageEnum 自动路由模型：
     * COLLECTING→qwen-plus, EXPLORING→deepseek-r1, SETTLING→qwen-plus
     */
    String generateText(StageEnum stage, String systemPrompt, String userPrompt);

    /** 图片生成（wanx2.1-t2i-turbo），返回图片 URL */
    String generateImage(String prompt);

    /** VL 合规检测（qwen-vl-plus），返回 JSON 检测结果 */
    String detectCompliance(String imageUrl, String prompt);

    /** 视频生成（wanx2.1-t2v），返回视频 URL */
    String generateVideo(String prompt);

    /** 语音合成（qwen-tts），返回音频 URL */
    String tts(String text);
}

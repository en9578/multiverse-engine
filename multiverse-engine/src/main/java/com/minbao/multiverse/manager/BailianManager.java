package com.minbao.multiverse.manager;

import com.minbao.multiverse.enums.StageEnum;

/**
 * 百炼 DashScope 统一接入层。
 * 文本生成按 StageEnum 自动路由模型，其他模态（生图/VL/视频/TTS）独立方法。
 */
public interface BailianManager {
    /**
     * 统一文本生成，按 StageEnum 自动路由模型：
     * COLLECTING→qwen3.7-plus, GENERATING/EXPLORING→deepseek-v4-pro, SETTLING→qwen3.8-max。
     * 无 taskId 版本不做任务级预算记账（测试/无任务上下文调用用）。
     */
    String generateText(StageEnum stage, String systemPrompt, String userPrompt);

    /**
     * 带任务级 token 预算的文本生成（编排链路统一走此重载）：
     * 调用前查 TokenBudgetManager 预算，超限抛 LLM_DEGRADED（调用方沿既有降级路径处理）；
     * 成功后按 usage 归账。stage 的 maxOutputTokens 作为单次输出上限。
     */
    String generateText(StageEnum stage, String systemPrompt, String userPrompt, Long taskId);

    /** 图片生成（wanx2.1-t2i-turbo），返回图片 URL */
    String generateImage(String prompt);

    /** VL 合规检测（qwen-vl-plus），返回 JSON 检测结果 */
    String detectCompliance(String imageUrl, String prompt);

    /** 视频生成（wanx2.1-t2v），返回视频 URL */
    String generateVideo(String prompt);

    /** 语音合成（qwen-tts），返回音频 URL */
    String tts(String text);
}

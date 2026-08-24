package com.minbao.multiverse.manager.impl;

import com.minbao.multiverse.common.BusinessException;
import com.minbao.multiverse.dao.BailianCallLogDAO;
import com.minbao.multiverse.domain.entity.BailianCallLogDO;
import com.minbao.multiverse.enums.ErrorCodeEnum;
import com.minbao.multiverse.enums.StageEnum;
import com.minbao.multiverse.manager.BailianManager;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import jakarta.annotation.Resource;
import java.net.URI;
import java.util.List;

/**
 * 百炼统一接入实现（设计 §3.2 / §14.3）-- OpenAI 兼容协议版。
 * token-plan 团队 key 仅支持 OpenAI 兼容协议，基地址见 application.yml `spring.ai.openai.base-url`；
 * 文本生成按 StageEnum 路由模型；生图 / 视觉 / 向量化走独立模型常量。
 * 每个方法含：幂等检查 → 熔断包装 → 重试(3次指数退避) → 落库记录（bailian_call_log）。
 */
@Service
public class BailianManagerImpl implements BailianManager {
    private static final Logger log = LoggerFactory.getLogger(BailianManagerImpl.class);
    private static final int MAX_RETRY = 3;

    /** 非文本模型常量（设计 §14.3）：文生图 / 视觉理解 */
    private static final String IMAGE_MODEL = "wan2.7-image-pro";
    private static final String VL_MODEL = "qwen-vl-plus";

    @Resource
    private BailianCallLogDAO callLogDAO;

    @Resource
    private CircuitBreaker bailianBreaker;

    /** Spring AI OpenAI starter 自动装配（指向 token-plan 兼容基地址） */
    @Resource
    private ChatModel chatModel;

    /** Spring AI OpenAI starter 自动装配的 OpenAiImageModel（ImageModel 接口） */
    @Resource
    private ImageModel imageModel;

    // ==================== 文本生成（统一入口，StageEnum 路由模型） ====================

    @Override
    public String generateText(StageEnum stage, String systemPrompt, String userPrompt) {
        String model = stage.getModelName();
        String requestId = generateRequestId(stage);
        String fullPrompt = buildPrompt(systemPrompt, userPrompt);

        BailianCallLogDO cached = callLogDAO.selectByRequestId(requestId);
        if (cached != null && Boolean.TRUE.equals(cached.getSuccess())) {
            log.info("百炼幂等命中 requestId={} stage={}", requestId, stage);
            return cached.getOutputText();
        }

        for (int i = 1; i <= MAX_RETRY; i++) {
            try {
                String result = bailianBreaker.executeSupplier(
                        () -> doGenerateText(model, systemPrompt, userPrompt));
                callLogDAO.insert(BailianCallLogDO.success(requestId, "text", model,
                        truncate(fullPrompt, 500), result, null, null));
                log.info("百炼文本生成成功 requestId={} stage={} model={} retry={}", requestId, stage, model, i);
                return result;
            } catch (CallNotPermittedException e) {
                log.warn("百炼熔断开启 requestId={}", requestId);
                throw new BusinessException(ErrorCodeEnum.CIRCUIT_OPEN, e);
            } catch (Exception e) {
                log.warn("百炼调用异常 requestId={} retry={}/{} model={}", requestId, i, MAX_RETRY, model, e);
                if (i == MAX_RETRY) {
                    callLogDAO.insert(BailianCallLogDO.fail(requestId, "text", model,
                            truncate(fullPrompt, 500), e.getMessage()));
                    throw new BusinessException(ErrorCodeEnum.BAILIAN_CALL_TIMEOUT, e);
                }
                sleepBackoff(i);
            }
        }
        throw new BusinessException(ErrorCodeEnum.BAILIAN_CALL_TIMEOUT);
    }

    private String doGenerateText(String model, String systemPrompt, String userPrompt) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .build();

        List<Message> messages;
        if (systemPrompt == null || systemPrompt.isBlank()) {
            messages = List.of(new UserMessage(userPrompt));
        } else {
            messages = List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt));
        }

        ChatResponse response = chatModel.call(new Prompt(messages, options));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new BusinessException(ErrorCodeEnum.BAILIAN_API_ERROR);
        }
        return response.getResult().getOutput().getText();
    }

    // ==================== 图片生成（wan2.7-image-pro） ====================

    @Override
    public String generateImage(String prompt) {
        String requestId = "img-" + System.currentTimeMillis();

        BailianCallLogDO cached = callLogDAO.selectByRequestId(requestId);
        if (cached != null && Boolean.TRUE.equals(cached.getSuccess())) {
            log.info("百炼生图幂等命中 requestId={}", requestId);
            return cached.getOutputText();
        }

        for (int i = 1; i <= MAX_RETRY; i++) {
            try {
                String result = bailianBreaker.executeSupplier(() -> doGenerateImage(prompt));
                callLogDAO.insert(BailianCallLogDO.success(requestId, "image", IMAGE_MODEL,
                        truncate(prompt, 500), result, null, null));
                log.info("百炼生图成功 requestId={} model={} retry={}", requestId, IMAGE_MODEL, i);
                return result;
            } catch (CallNotPermittedException e) {
                log.warn("百炼熔断开启 requestId={}", requestId);
                throw new BusinessException(ErrorCodeEnum.CIRCUIT_OPEN, e);
            } catch (Exception e) {
                log.warn("百炼生图异常 requestId={} retry={}/{}", requestId, i, MAX_RETRY, e);
                if (i == MAX_RETRY) {
                    callLogDAO.insert(BailianCallLogDO.fail(requestId, "image", IMAGE_MODEL,
                            truncate(prompt, 500), e.getMessage()));
                    throw new BusinessException(ErrorCodeEnum.BAILIAN_CALL_TIMEOUT, e);
                }
                sleepBackoff(i);
            }
        }
        throw new BusinessException(ErrorCodeEnum.BAILIAN_CALL_TIMEOUT);
    }

    private String doGenerateImage(String prompt) {
        ImagePrompt imagePrompt = new ImagePrompt(prompt, OpenAiImageOptions.builder()
                .model(IMAGE_MODEL)
                .width(1024)
                .height(1024)
                .N(1)
                .build());
        var response = imageModel.call(imagePrompt);
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new BusinessException(ErrorCodeEnum.BAILIAN_API_ERROR);
        }
        String url = response.getResult().getOutput().getUrl();
        return url != null ? url : "";
    }

    // ==================== VL 合规检测（qwen-vl-plus） ====================

    @Override
    public String detectCompliance(String imageUrl, String prompt) {
        String requestId = "vl-" + System.currentTimeMillis();

        BailianCallLogDO cached = callLogDAO.selectByRequestId(requestId);
        if (cached != null && Boolean.TRUE.equals(cached.getSuccess())) {
            log.info("百炼 VL 幂等命中 requestId={}", requestId);
            return cached.getOutputText();
        }

        for (int i = 1; i <= MAX_RETRY; i++) {
            try {
                String result = bailianBreaker.executeSupplier(() -> doDetectCompliance(imageUrl, prompt));
                callLogDAO.insert(BailianCallLogDO.success(requestId, "vl", VL_MODEL,
                        truncate(prompt, 500), result, null, null));
                log.info("百炼 VL 检测成功 requestId={} model={} retry={}", requestId, VL_MODEL, i);
                return result;
            } catch (CallNotPermittedException e) {
                log.warn("百炼熔断开启 requestId={}", requestId);
                throw new BusinessException(ErrorCodeEnum.CIRCUIT_OPEN, e);
            } catch (Exception e) {
                log.warn("百炼 VL 异常 requestId={} retry={}/{}", requestId, i, MAX_RETRY, e);
                if (i == MAX_RETRY) {
                    callLogDAO.insert(BailianCallLogDO.fail(requestId, "vl", VL_MODEL,
                            truncate(prompt, 500), e.getMessage()));
                    throw new BusinessException(ErrorCodeEnum.BAILIAN_CALL_TIMEOUT, e);
                }
                sleepBackoff(i);
            }
        }
        throw new BusinessException(ErrorCodeEnum.BAILIAN_CALL_TIMEOUT);
    }

    private String doDetectCompliance(String imageUrl, String prompt) {
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, URI.create(imageUrl));
        UserMessage userMessage = UserMessage.builder()
                .text(prompt)
                .media(media)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(VL_MODEL)
                .build();
        List<Message> messages = List.of(userMessage);
        ChatResponse response = chatModel.call(new Prompt(messages, options));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new BusinessException(ErrorCodeEnum.BAILIAN_API_ERROR);
        }
        return response.getResult().getOutput().getText();
    }

    // ==================== 视频生成（异步任务，MVP 暂不实现） ====================

    @Override
    public String generateVideo(String prompt) {
        log.info("百炼视频生成 prompt={} (骨架：T2V 异步任务，MVP 暂不实现)", truncate(prompt, 100));
        return "{\"url\":\"\",\"status\":\"not_implemented\"}";
    }

    // ==================== 语音合成（MVP 暂不实现） ====================

    @Override
    public String tts(String text) {
        log.info("百炼 TTS text={} (骨架：qwen-tts，MVP 暂不实现)", truncate(text, 100));
        return "{\"url\":\"\",\"status\":\"not_implemented\"}";
    }

    // ==================== 内部工具方法 ====================

    private String generateRequestId(StageEnum stage) {
        return stage.name() + "-" + System.currentTimeMillis();
    }

    private String buildPrompt(String systemPrompt, String userPrompt) {
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            return systemPrompt + "\n\n" + userPrompt;
        }
        return userPrompt;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private void sleepBackoff(int attempt) {
        try {
            long ms = (long) Math.pow(2, attempt) * 500L; // 1s, 2s, 4s
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}

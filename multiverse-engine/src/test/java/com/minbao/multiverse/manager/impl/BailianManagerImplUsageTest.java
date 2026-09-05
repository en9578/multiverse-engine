package com.minbao.multiverse.manager.impl;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P4 usage→token_count 解析（包内静态助手）：total 优先，total 缺失回退 prompt+completion，
 * usage 缺失/为空置 null。
 */
class BailianManagerImplUsageTest {

    private Usage usage(Integer total, Integer prompt, Integer completion) {
        Usage u = mock(Usage.class);
        when(u.getTotalTokens()).thenReturn(total);
        when(u.getPromptTokens()).thenReturn(prompt);
        when(u.getCompletionTokens()).thenReturn(completion);
        return u;
    }

    private ChatResponse responseWith(Usage usage) {
        ChatResponse resp = mock(ChatResponse.class);
        if (usage == null) {
            when(resp.getMetadata()).thenReturn(null);
        } else {
            ChatResponseMetadata meta = mock(ChatResponseMetadata.class);
            when(meta.getUsage()).thenReturn(usage);
            when(resp.getMetadata()).thenReturn(meta);
        }
        return resp;
    }

    @Test
    void prefersTotalTokens() {
        assertEquals(123, BailianManagerImpl.usageTokens(responseWith(usage(123, 50, 70))));
    }

    @Test
    void fallsBackToPromptPlusCompletion() {
        assertEquals(15, BailianManagerImpl.usageTokens(responseWith(usage(null, 10, 5))));
    }

    @Test
    void returnsNullWhenUsageAbsent() {
        assertNull(BailianManagerImpl.usageTokens(responseWith(null)));
        assertNull(BailianManagerImpl.usageTokens(null));
    }

    @Test
    void returnsNullWhenUsageEmpty() {
        assertNull(BailianManagerImpl.usageTokens(responseWith(usage(null, 0, 0))));
    }
}

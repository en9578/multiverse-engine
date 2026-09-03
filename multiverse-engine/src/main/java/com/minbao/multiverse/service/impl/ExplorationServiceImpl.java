package com.minbao.multiverse.service.impl;

import com.minbao.multiverse.common.BusinessException;
import com.minbao.multiverse.common.JsonUtil;
import com.minbao.multiverse.dao.CompetitorReactionDAO;
import com.minbao.multiverse.dao.ConversationDAO;
import com.minbao.multiverse.dao.GeneDefectDAO;
import com.minbao.multiverse.dao.StressTestDAO;
import com.minbao.multiverse.dao.UniverseDAO;
import com.minbao.multiverse.dao.UniverseWeatherDAO;
import com.minbao.multiverse.domain.dto.ExploreDTO;
import com.minbao.multiverse.domain.entity.CompetitorReactionDO;
import com.minbao.multiverse.domain.entity.ConversationDO;
import com.minbao.multiverse.domain.entity.GeneDefectDO;
import com.minbao.multiverse.domain.entity.StressTestDO;
import com.minbao.multiverse.domain.entity.UniverseDO;
import com.minbao.multiverse.domain.entity.UniverseWeatherDO;
import com.minbao.multiverse.domain.vo.CompetitorReactionVO;
import com.minbao.multiverse.domain.vo.StressTestVO;
import com.minbao.multiverse.domain.vo.UniverseVO;
import com.minbao.multiverse.domain.vo.UniverseWeatherVO;
import com.minbao.multiverse.enums.ErrorCodeEnum;
import com.minbao.multiverse.enums.StageEnum;
import com.minbao.multiverse.manager.BailianManager;
import com.minbao.multiverse.service.ExplorationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 穿梭体验：单宇宙详情 + 对话式探索。
 * 详情返回 90 天演化数据、基因缺陷、策略包；探索为多轮对话，持久化对话历史并返回向导回复。
 */
@Service
public class ExplorationServiceImpl implements ExplorationService {
    private static final Logger log = LoggerFactory.getLogger(ExplorationServiceImpl.class);

    private static final String SYSTEM_PROMPT = """
            你是多元宇宙穿越向导。卖家正在探索一个平行宇宙，你需要基于该宇宙的「策略包」与「90 天演化结果」，
            用通俗易懂的中文回答卖家对该宇宙命运的疑问。
            要求：
            1. 只基于给定的宇宙数据推理，数据不足时明确说明"该宇宙数据未覆盖"。
            2. 回答控制在 200 字内，直接给结论，不编造销量等绝对数值。
            3. 引用演化结果中的评分/存活率/证据时，说明其可信来源（知识库规则 vs 模型推断）。""";

    @Resource
    private UniverseDAO universeDAO;
    @Resource
    private GeneDefectDAO geneDefectDAO;
    @Resource
    private StressTestDAO stressTestDAO;
    @Resource
    private UniverseWeatherDAO universeWeatherDAO;
    @Resource
    private CompetitorReactionDAO competitorReactionDAO;
    @Resource
    private ConversationDAO conversationDAO;
    @Resource
    private BailianManager bailianManager;

    @Override
    public UniverseVO explore(Long universeId, ExploreDTO dto) {
        log.info("穿梭体验 universeId={} sessionId={}", universeId, dto.getSessionId());
        UniverseDO universe = requireUniverse(universeId);

        String sessionId = (dto.getSessionId() == null || dto.getSessionId().isBlank())
                ? UUID.randomUUID().toString() : dto.getSessionId();
        String universeKey = String.valueOf(universeId);

        // 1. 持久化用户提问
        insertConversation(universe, universeKey, sessionId, "user", dto.getMessage());

        // 2. 加载历史对话
        List<ConversationDO> history = conversationDAO.selectByTaskAndSession(
                universe.getTaskId(), universeKey, sessionId);

        // 3. 调用向导生成回复
        String reply;
        try {
            reply = bailianManager.generateText(StageEnum.SETTLING, SYSTEM_PROMPT, buildUserPrompt(universe, history));
        } catch (Exception e) {
            log.warn("穿梭体验 LLM 调用失败 universeId={}", universeId, e);
            reply = "穿越向导暂时不可用（" + e.getMessage() + "），请稍后重试。";
        }
        insertConversation(universe, universeKey, sessionId, "assistant", reply);

        // 4. 返回富化详情 + 回复
        UniverseVO vo = toVO(universe);
        vo.setReply(reply);
        vo.setSessionId(sessionId);
        return vo;
    }

    @Override
    public UniverseVO getUniverseDetail(Long universeId) {
        UniverseDO universe = requireUniverse(universeId);
        return toVO(universe);
    }

    private UniverseVO toVO(UniverseDO universe) {
        UniverseVO vo = new UniverseVO();
        vo.setId(universe.getId());
        vo.setTaskId(universe.getTaskId());
        vo.setUniverseIndex(universe.getUniverseIndex());
        vo.setRating(universe.getRating());
        vo.setSubState(universe.getSubState());
        vo.setSurvivalRate(universe.getSurvivalRate());
        vo.setStrategyPackage(universe.getStrategyPackage());
        vo.setEvolutionData(JsonUtil.parseObject(universe.getEvolutionData()));
        vo.setGeneDefects(loadGeneDefects(universe.getId()));
        vo.setStressTests(toStressVOs(universe.getId()));
        vo.setWeather(toWeatherVO(universe.getId()));
        vo.setCompetitorReactions(toReactionVOs(universe.getId()));
        return vo;
    }

    /** 5 风暴压力测试（仅 STRATEGY 宇宙有行，无则空 list） */
    private List<StressTestVO> toStressVOs(Long universeId) {
        return stressTestDAO.selectByUniverseId(universeId).stream().map(d -> {
            StressTestVO vo = new StressTestVO();
            vo.setStorm(d.getStorm());
            vo.setSurvivalRate(d.getSurvivalRate());
            vo.setWeakestLink(d.getWeakestLink());
            vo.setFixSuggestion(d.getFixSuggestion());
            return vo;
        }).collect(Collectors.toList());
    }

    /** 市场气象（修复落库后 STRATEGY 宇宙一行；无则 null） */
    private UniverseWeatherVO toWeatherVO(Long universeId) {
        UniverseWeatherDO d = universeWeatherDAO.selectByUniverseId(universeId);
        if (d == null) return null;
        UniverseWeatherVO vo = new UniverseWeatherVO();
        vo.setWeather(d.getWeather());
        vo.setSearchSignal(d.getSearchSignal());
        vo.setSentimentSignal(d.getSentimentSignal());
        vo.setPriceSignal(d.getPriceSignal());
        vo.setPolicySignal(d.getPolicySignal());
        vo.setForecast7d(d.getForecast7d());
        vo.setForecast30d(d.getForecast30d());
        vo.setForecast90d(d.getForecast90d());
        return vo;
    }

    /** 竞品关联反应（无 LLM 时为 []） */
    private List<CompetitorReactionVO> toReactionVOs(Long universeId) {
        return competitorReactionDAO.selectByUniverseId(universeId).stream().map(d -> {
            CompetitorReactionVO vo = new CompetitorReactionVO();
            vo.setCompetitorName(d.getCompetitorName());
            vo.setReactionType(d.getReactionType());
            vo.setProbability(d.getProbability());
            vo.setImpact(d.getImpact());
            vo.setSource(d.getSource());
            vo.setEvidence(d.getEvidence());
            return vo;
        }).collect(Collectors.toList());
    }

    private UniverseDO requireUniverse(Long universeId) {
        UniverseDO universe = universeDAO.selectById(universeId);
        if (universe == null) {
            throw new BusinessException(ErrorCodeEnum.UNIVERSE_NOT_FOUND);
        }
        return universe;
    }

    private List<String> loadGeneDefects(Long universeId) {
        return geneDefectDAO.selectByUniverseId(universeId).stream()
                .map(this::formatDefect)
                .collect(Collectors.toList());
    }

    private String formatDefect(GeneDefectDO d) {
        String severity = d.getSeverity() == null ? "" : d.getSeverity();
        String frequency = d.getFrequency() == null ? "" : d.getFrequency();
        String solution = d.getSolution() == null || d.getSolution().isBlank() ? "暂无方案" : d.getSolution();
        return String.format("%s [%s/%s] → %s", d.getDefectName(), frequency, severity, solution);
    }

    /** 构建 LLM 用户 prompt：宇宙策略包 + 演化结果 + 对话历史 + 当前问题 */
    private String buildUserPrompt(UniverseDO universe, List<ConversationDO> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("产品：").append(universe.getProductName())
          .append("（目标市场：").append(universe.getTargetMarket()).append("）\n");
        sb.append("宇宙策略包：").append(universe.getStrategyPackage()).append("\n");
        sb.append("90 天演化结果：").append(universe.getEvolutionData()).append("\n\n");
        sb.append("对话历史：\n");
        for (ConversationDO c : history) {
            sb.append(c.getRole()).append(": ").append(c.getContent()).append("\n");
        }
        return sb.toString();
    }

    private void insertConversation(UniverseDO universe, String universeKey,
                                    String sessionId, String role, String content) {
        ConversationDO conversation = new ConversationDO();
        conversation.setTaskId(universe.getTaskId());
        conversation.setUniverseId(universeKey);
        conversation.setSessionId(sessionId);
        conversation.setRole(role);
        conversation.setContent(content);
        conversation.setTraceId(universe.getTraceId());
        conversation.setIsDeleted(false);
        conversationDAO.insert(conversation);
    }
}

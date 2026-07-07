<script setup lang="ts">
import { computed, ref } from 'vue';
import { analyzeReport, askFollowUp, fetchReportDetail, fetchReportHistory } from './api/advisorApi';
import type { AdvisorReportSummary, FollowUpResponse, ResearchReport } from './types/advisor';

const query = ref('Analyze 600519');
const analysisType = ref('full');
const historyStockCode = ref('600519');
const report = ref<ResearchReport | null>(null);
const currentReportId = ref<number | null>(null);
const history = ref<AdvisorReportSummary[]>([]);
const loading = ref(false);
const historyLoading = ref(false);
const followUpLoading = ref(false);
const errorMessage = ref('');
const followUpError = ref('');
const lastDurationMs = ref<number | null>(null);
const followUpQuestion = ref('');
const followUpAnswer = ref<FollowUpResponse | null>(null);

const rawJson = computed(() => {
  if (!report.value) {
    return '尚未生成报告';
  }
  return JSON.stringify({
    reportId: currentReportId.value,
    report: report.value,
    followUp: followUpAnswer.value
  }, null, 2);
});

const qualityScore = computed(() => report.value?.quality?.qualityScore ?? 0);

const agentSections = computed(() => {
  const current = report.value;
  return [
    { key: 'fundamental', title: '基本面', content: current?.fundamentalView || '' },
    { key: 'technical', title: '技术面', content: current?.technicalView || '' },
    { key: 'valuation', title: '估值', content: current?.valuationView || '' },
    { key: 'news', title: '新闻', content: current?.newsView || '' },
    { key: 'risk', title: '风险', content: current?.riskView || '' }
  ];
});

const llmHint = computed(() => {
  if (!report.value) {
    return '尚未请求后端';
  }
  if (followUpAnswer.value) {
    return followUpAnswer.value.llmEnabled
      ? '追问已通过后端 LangChain4j 链路生成'
      : '追问返回了可解释降级结果；如需真实 LLM，请启用后端 LLM 配置';
  }
  const filledCount = agentSections.value.filter((item) => item.content.trim().length > 0).length;
  if (filledCount >= 4) {
    return '检测到多段 Agent 分析内容，可用于观察真实 LLM 或降级输出效果';
  }
  if (filledCount > 0) {
    return '检测到部分分析内容；如需真实 LLM，请确认后端已配置 ADVISOR_LLM_ENABLED=true';
  }
  return '未检测到 Agent 段落；请检查后端 LLM 配置或降级链路';
});

const hasReport = computed(() => report.value !== null);
const canAskFollowUp = computed(() => currentReportId.value !== null && followUpQuestion.value.trim().length > 0);

async function generateReport() {
  if (!query.value.trim()) {
    errorMessage.value = '请输入股票代码或分析请求';
    return;
  }
  errorMessage.value = '';
  followUpError.value = '';
  followUpAnswer.value = null;
  currentReportId.value = null;
  loading.value = true;
  const startedAt = performance.now();
  try {
    const result = await analyzeReport({
      query: query.value.trim(),
      analysisType: analysisType.value
    });
    report.value = result;
    historyStockCode.value = result.stockCode;
    lastDurationMs.value = Math.round(performance.now() - startedAt);
    const reports = await loadHistory(result.stockCode, true);
    currentReportId.value = reports[0]?.id ?? null;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '生成报告失败';
  } finally {
    loading.value = false;
  }
}

async function loadHistory(stockCode = historyStockCode.value, quiet = false): Promise<AdvisorReportSummary[]> {
  if (!stockCode.trim()) {
    errorMessage.value = '请输入股票代码后再查询历史';
    return [];
  }
  if (!quiet) {
    errorMessage.value = '';
  }
  historyLoading.value = true;
  try {
    const reports = await fetchReportHistory(stockCode.trim());
    history.value = reports;
    return reports;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '查询历史失败';
    return [];
  } finally {
    historyLoading.value = false;
  }
}

async function loadReport(id: number) {
  errorMessage.value = '';
  followUpError.value = '';
  followUpAnswer.value = null;
  loading.value = true;
  try {
    report.value = await fetchReportDetail(id);
    currentReportId.value = id;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载报告详情失败';
  } finally {
    loading.value = false;
  }
}

async function submitFollowUp() {
  if (currentReportId.value === null) {
    followUpError.value = '请先生成报告，或从历史报告中打开一份报告';
    return;
  }
  if (!followUpQuestion.value.trim()) {
    followUpError.value = '请输入追问内容';
    return;
  }
  followUpError.value = '';
  followUpLoading.value = true;
  try {
    followUpAnswer.value = await askFollowUp(currentReportId.value, followUpQuestion.value.trim());
  } catch (error) {
    followUpError.value = error instanceof Error ? error.message : '追问失败';
  } finally {
    followUpLoading.value = false;
  }
}
</script>

<template>
  <main class="shell">
    <aside class="sidebar">
      <div>
        <p class="eyebrow">A-Share Advisor</p>
        <h1>LangChain4j 投研联调工作台</h1>
        <p class="muted">Vue 前端通过 Vite proxy 调用 Spring Boot 后端，适合测试真实 LLM、报告结构和证据链。</p>
      </div>

      <section class="status-panel">
        <span class="status-dot"></span>
        <div>
          <strong>联调状态</strong>
          <p>{{ llmHint }}</p>
        </div>
      </section>

      <section class="mini-panel">
        <span>后端</span>
        <strong>/api/advisor/analyze</strong>
      </section>
      <section class="mini-panel">
        <span>当前报告 ID</span>
        <strong>{{ currentReportId ?? '-' }}</strong>
      </section>
      <section class="mini-panel">
        <span>响应耗时</span>
        <strong>{{ lastDurationMs === null ? '-' : `${lastDurationMs} ms` }}</strong>
      </section>
      <section class="mini-panel">
        <span>质量分</span>
        <strong>{{ hasReport ? qualityScore : '-' }}</strong>
      </section>
    </aside>

    <section class="workspace">
      <section class="query-bar">
        <div class="field grow">
          <label for="query">分析请求</label>
          <input id="query" v-model="query" placeholder="Analyze 600519 或 帮我分析600519" />
        </div>
        <div class="field compact">
          <label for="analysisType">分析类型</label>
          <select id="analysisType" v-model="analysisType">
            <option value="full">完整分析</option>
            <option value="valuation">估值分析</option>
            <option value="risk">风险分析</option>
          </select>
        </div>
        <button class="primary" :disabled="loading" @click="generateReport">
          {{ loading ? '生成中' : '生成报告' }}
        </button>
      </section>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <section v-if="hasReport && report" class="report-head">
        <div>
          <p class="eyebrow">{{ report.stockCode }} · {{ report.analysisTime?.replace('T', ' ').slice(0, 19) }}</p>
          <h2>{{ report.stockName }}</h2>
          <p>{{ report.quoteSummary }}</p>
        </div>
        <div class="quality">
          <span>{{ qualityScore }}</span>
          <strong>质量分</strong>
        </div>
      </section>

      <section v-if="hasReport && report" class="conclusion">
        <h3>核心结论</h3>
        <p>{{ report.conclusion || '暂无结论' }}</p>
      </section>

      <section class="section-title">
        <div>
          <p class="eyebrow">Agent Views</p>
          <h3>五类 Agent 分析</h3>
        </div>
      </section>

      <section class="agent-grid">
        <article v-for="item in agentSections" :key="item.key" class="panel">
          <h4>{{ item.title }}</h4>
          <p>{{ item.content || '暂无内容；请确认后端是否启用真实 LLM 或是否存在降级输出。' }}</p>
        </article>
      </section>

      <section class="two-column">
        <article class="panel">
          <h3>可解释洞察</h3>
          <div v-if="report?.insights?.length" class="stack">
            <details v-for="insight in report.insights" :key="`${insight.type}-${insight.title}`" open>
              <summary>{{ insight.type }} · {{ insight.title }}</summary>
              <p>{{ insight.summary }}</p>
              <div class="tags">
                <span>风险：{{ insight.riskLevel }}</span>
                <span>置信度：{{ insight.confidence }}</span>
              </div>
              <div class="tags">
                <span v-for="evidence in insight.supportingEvidence" :key="evidence">{{ evidence }}</span>
              </div>
            </details>
          </div>
          <p v-else class="muted">生成报告后展示 insights。</p>
        </article>

        <article class="panel">
          <h3>证据链</h3>
          <div v-if="report?.evidences?.length" class="evidence-list">
            <section v-for="evidence in report.evidences" :key="`${evidence.source}-${evidence.title}`" class="evidence">
              <strong>{{ evidence.source }}</strong>
              <span>{{ evidence.title }}</span>
              <p>{{ evidence.value }}</p>
              <small>{{ evidence.fetchedAt?.replace('T', ' ').slice(0, 19) }}</small>
            </section>
          </div>
          <p v-else class="muted">生成报告后展示 evidences。</p>
        </article>
      </section>
    </section>

    <aside class="debugbar">
      <section class="panel">
        <h3>历史报告</h3>
        <div class="history-form">
          <input v-model="historyStockCode" placeholder="600519" />
          <button :disabled="historyLoading" @click="loadHistory()">{{ historyLoading ? '查询中' : '查询' }}</button>
        </div>
        <div v-if="history.length" class="history-list">
          <button v-for="item in history" :key="item.id" @click="loadReport(item.id)">
            <strong>{{ item.stockCode }} · {{ item.stockName }}</strong>
            <span>{{ item.createdAt?.replace('T', ' ').slice(0, 19) }}</span>
          </button>
        </div>
        <p v-else class="muted">暂无历史报告。</p>
      </section>

      <section class="panel follow-up">
        <h3>基于当前报告追问</h3>
        <textarea v-model="followUpQuestion" placeholder="例如：为什么估值偏高？最大的风险是什么？"></textarea>
        <button
          data-test="follow-up-submit"
          :disabled="followUpLoading || !canAskFollowUp"
          @click="submitFollowUp"
        >
          {{ followUpLoading ? '追问中' : '提交追问' }}
        </button>
        <p v-if="followUpError" class="error">{{ followUpError }}</p>
        <article v-if="followUpAnswer" class="follow-up-answer">
          <strong>{{ followUpAnswer.llmEnabled ? 'LLM 回答' : '降级回答' }}</strong>
          <p>{{ followUpAnswer.answer }}</p>
          <div class="tags">
            <span v-for="evidence in followUpAnswer.citedEvidence" :key="evidence">{{ evidence }}</span>
          </div>
          <div class="tags">
            <span v-for="source in followUpAnswer.contextSources" :key="source">{{ source }}</span>
          </div>
        </article>
        <p class="muted">追问会调用 `/api/advisor/reports/{id}/follow-up`，基于当前报告分析段落与证据链回答。</p>
      </section>

      <section class="panel json-panel">
        <h3>原始 JSON</h3>
        <pre>{{ rawJson }}</pre>
      </section>
    </aside>
  </main>
</template>

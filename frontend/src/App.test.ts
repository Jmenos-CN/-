import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import App from './App.vue';

const fixtures = vi.hoisted(() => ({
  sampleReport: {
    stockCode: '600519',
    stockName: '贵州茅台',
    analysisTime: '2026-07-07T12:00:00',
    quoteSummary: '最新价 1510.00',
    fundamentalView: '基本面稳定',
    technicalView: '技术面波动',
    valuationView: '估值偏高',
    newsView: '新闻热度较高',
    riskView: '关注估值回撤',
    conclusion: '仅供研究参考',
    evidences: [{
      source: 'Sina Finance',
      title: '贵州茅台实时行情',
      value: '最新价 1510.00',
      fetchedAt: '2026-07-07T12:00:00'
    }],
    insights: [],
    quality: { qualityScore: 90, missingEvidenceTypes: [], warnings: [] }
  }
}));

vi.mock('./api/advisorApi', () => ({
  analyzeReport: vi.fn().mockResolvedValue(fixtures.sampleReport),
  fetchReportHistory: vi.fn().mockResolvedValue([{
    id: 7,
    stockCode: '600519',
    stockName: '贵州茅台',
    quoteSummary: '最新价 1510.00',
    createdAt: '2026-07-07T12:00:00'
  }]),
  fetchReportDetail: vi.fn().mockResolvedValue(fixtures.sampleReport),
  askFollowUp: vi.fn().mockResolvedValue({
    reportId: 7,
    question: '最大的风险是什么？',
    answer: '主要风险是估值回撤。',
    citedEvidence: ['贵州茅台实时行情'],
    contextSources: ['report:7'],
    llmEnabled: true,
    answeredAt: '2026-07-07T12:01:00'
  })
}));

describe('App', () => {
  it('renders the advisor integration workbench shell', () => {
    const wrapper = mount(App);

    expect(wrapper.text()).toContain('A-Share Advisor');
    expect(wrapper.text()).toContain('生成报告');
    expect(wrapper.text()).toContain('五类 Agent 分析');
    expect(wrapper.text()).toContain('质量分');
    expect(wrapper.text()).toContain('证据链');
    expect(wrapper.text()).toContain('原始 JSON');
    expect(wrapper.text()).toContain('基于当前报告追问');
  });

  it('answers a follow-up question after report generation resolves the latest report id', async () => {
    const wrapper = mount(App);

    await wrapper.find('button.primary').trigger('click');
    await new Promise((resolve) => setTimeout(resolve, 0));
    await wrapper.find('textarea').setValue('最大的风险是什么？');
    await wrapper.find('[data-test="follow-up-submit"]').trigger('click');
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(wrapper.text()).toContain('主要风险是估值回撤。');
    expect(wrapper.text()).toContain('贵州茅台实时行情');
    expect(wrapper.text()).toContain('report:7');
  });
});

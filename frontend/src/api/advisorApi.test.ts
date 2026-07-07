import { afterEach, describe, expect, it, vi } from 'vitest';
import { analyzeReport, askFollowUp, fetchReportDetail, fetchReportHistory } from './advisorApi';

function okJson(data: unknown) {
  return {
    ok: true,
    json: () => Promise.resolve({ success: true, code: 200, message: 'success', data })
  } as Response;
}

describe('advisorApi', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('posts analyze request and unwraps backend Result payload', async () => {
    const report = { stockCode: '600519', stockName: '贵州茅台' };
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(okJson(report));

    const result = await analyzeReport({ query: 'Analyze 600519', analysisType: 'full' });

    expect(result).toEqual(report);
    expect(fetchMock).toHaveBeenCalledWith('/api/advisor/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json; charset=utf-8' },
      body: JSON.stringify({ query: 'Analyze 600519', analysisType: 'full' })
    });
  });

  it('loads report history by stock code', async () => {
    const history = [{ id: 1, stockCode: '600519' }];
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(okJson(history));

    const result = await fetchReportHistory('600519');

    expect(result).toEqual(history);
    expect(fetchMock).toHaveBeenCalledWith('/api/advisor/reports?stockCode=600519', undefined);
  });

  it('loads report detail by id', async () => {
    const report = { stockCode: '600519', quality: { qualityScore: 100 } };
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(okJson(report));

    const result = await fetchReportDetail(7);

    expect(result).toEqual(report);
    expect(fetchMock).toHaveBeenCalledWith('/api/advisor/reports/7', undefined);
  });

  it('posts follow-up question against an existing report', async () => {
    const answer = {
      reportId: 7,
      question: '最大的风险是什么？',
      answer: '主要风险是估值回撤。',
      citedEvidence: ['贵州茅台实时行情'],
      contextSources: ['report:7'],
      llmEnabled: true,
      answeredAt: '2026-07-07T12:00:00'
    };
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(okJson(answer));

    const result = await askFollowUp(7, '最大的风险是什么？');

    expect(result).toEqual(answer);
    expect(fetchMock).toHaveBeenCalledWith('/api/advisor/reports/7/follow-up', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json; charset=utf-8' },
      body: JSON.stringify({ question: '最大的风险是什么？' })
    });
  });
});

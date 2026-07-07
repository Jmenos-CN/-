import { afterEach, describe, expect, it, vi } from 'vitest';
import { analyzeReport, fetchReportDetail, fetchReportHistory } from './advisorApi';

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
});

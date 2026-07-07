import type {
  AdvisorReportSummary,
  AdvisorRequest,
  FollowUpResponse,
  ResearchReport,
  Result
} from '../types/advisor';

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  const payload = (await response.json()) as Result<T>;
  if (!payload.success) {
    throw new Error(payload.message || '请求失败');
  }
  return payload.data;
}

export function analyzeReport(request: AdvisorRequest): Promise<ResearchReport> {
  return requestJson<ResearchReport>('/api/advisor/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify(request)
  });
}

export function fetchReportHistory(stockCode: string): Promise<AdvisorReportSummary[]> {
  return requestJson<AdvisorReportSummary[]>(
    `/api/advisor/reports?stockCode=${encodeURIComponent(stockCode)}`,
    undefined
  );
}

export function fetchReportDetail(id: number): Promise<ResearchReport> {
  return requestJson<ResearchReport>(`/api/advisor/reports/${encodeURIComponent(id)}`, undefined);
}

export function askFollowUp(reportId: number, question: string): Promise<FollowUpResponse> {
  return requestJson<FollowUpResponse>(`/api/advisor/reports/${encodeURIComponent(reportId)}/follow-up`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify({ question })
  });
}

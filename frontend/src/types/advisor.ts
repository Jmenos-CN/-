export interface Result<T> {
  success: boolean;
  code: number;
  message: string;
  data: T;
}

export interface AdvisorRequest {
  query: string;
  analysisType: string;
}

export interface DataEvidence {
  source: string;
  title: string;
  value: string;
  fetchedAt?: string;
}

export interface ReportInsight {
  type: string;
  title: string;
  summary: string;
  supportingEvidence: string[];
  riskLevel: string;
  confidence: number;
}

export interface ReportQuality {
  qualityScore: number;
  missingEvidenceTypes: string[];
  warnings: string[];
}

export interface ResearchReport {
  stockCode: string;
  stockName: string;
  analysisTime: string;
  quoteSummary: string;
  fundamentalView: string;
  technicalView: string;
  valuationView: string;
  newsView: string;
  riskView: string;
  conclusion: string;
  evidences: DataEvidence[];
  insights: ReportInsight[];
  quality: ReportQuality;
}

export interface AdvisorReportSummary {
  id: number;
  stockCode: string;
  stockName: string;
  quoteSummary: string;
  createdAt: string;
}

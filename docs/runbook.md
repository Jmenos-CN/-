# Runbook

## Test

```powershell
.\gradlew.bat :app:test --console=plain
```

## Compile

```powershell
.\gradlew.bat :app:compileJava --console=plain
```

## Start Backend

```powershell
.\gradlew.bat :app:bootRun --console=plain
```

The first milestone starts with an H2 in-memory datasource by default. PostgreSQL environment variables are only required after persistence is implemented.

## Analyze API

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/advisor/analyze" `
  -Method Post `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"query":"帮我分析600519","analysisType":"full"}'
```

Expected response shape:

```json
{
  "code": 200,
  "success": true,
  "message": "success",
  "data": {
    "stockCode": "600519",
    "stockName": "贵州茅台",
    "quoteSummary": "最新价 ...",
    "fundamentalView": "",
    "technicalView": "",
    "valuationView": "",
    "newsView": "",
    "riskView": "",
    "conclusion": "...不构成投资建议...",
    "evidences": []
  }
}
```

The endpoint calls Sina Finance for realtime quote data. If the public Sina endpoint is unavailable, the API returns an error until a fallback data source is added.

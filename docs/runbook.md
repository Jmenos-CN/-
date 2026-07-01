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

## Analyze API

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/advisor/analyze" `
  -Method Post `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"query":"帮我分析600519","analysisType":"full"}'
```

Expected first-milestone response shape:

```json
{
  "code": 200,
  "success": true,
  "message": "success",
  "data": {
    "query": "帮我分析600519",
    "analysisType": "full"
  }
}
```

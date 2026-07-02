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

Local startup uses an H2 in-memory datasource by default and creates advisor tables with `ddl-auto=update`.

## Start With Remote PostgreSQL

Use this only when you want the Java advisor project to persist reports into the existing Ubuntu PostgreSQL instance:

```powershell
$env:SERVER_PORT="18080"
$env:POSTGRES_URL="jdbc:postgresql://192.168.150.101:5432/jmenos_interview_guide"
$env:POSTGRES_USER="postgres"
$env:POSTGRES_PASSWORD="<postgres-password>"
$env:POSTGRES_DRIVER="org.postgresql.Driver"
$env:SPRING_JPA_HIBERNATE_DDL_AUTO="update"

.\gradlew.bat :app:bootRun --console=plain
```

The project writes to `stock_advisor_report`. Do not use `create` against this shared database because it already contains AI interview platform tables.

## Enable Redis Cache

Redis cache is optional. Enable it when the Ubuntu Redis instance is reachable:

```powershell
$env:APP_CACHE_REDIS_ENABLED="true"
$env:REDIS_HOST="192.168.150.101"
$env:REDIS_PORT="6379"
$env:REDIS_DATABASE="0"
$env:APP_CACHE_TTL_QUOTE="15s"
$env:APP_CACHE_TTL_REPORT="10m"

.\gradlew.bat :app:bootRun --console=plain
```

Cache keys:

- quote: `stock:quote:{code}`
- report: `advisor:report:{code}:{analysisType}`

If Redis is unavailable, cache reads behave as misses and cache writes are ignored.

## Enable Async Advisor Tasks

Async tasks reuse the same optional Redisson connection as the Redis cache. Start the backend with Redis enabled and
turn on the task consumer:

```powershell
$env:APP_CACHE_REDIS_ENABLED="true"
$env:REDIS_HOST="192.168.150.101"
$env:REDIS_PORT="6379"
$env:APP_ADVISOR_TASKS_CONSUMER_ENABLED="true"
$env:APP_ADVISOR_TASKS_CONSUMER_FIXED_DELAY="500"
$env:APP_ADVISOR_TASK_STREAM_NAME="advisor:tasks"
$env:APP_ADVISOR_TASK_STREAM_GROUP="advisor-task-workers"
$env:APP_ADVISOR_TASK_STREAM_CONSUMER="local-worker"

.\gradlew.bat :app:bootRun --console=plain
```

Async task APIs:

```powershell
$task = Invoke-RestMethod -Uri "http://localhost:8080/api/advisor/tasks" `
  -Method Post `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"query":"帮我分析600519","analysisType":"full"}'

Invoke-RestMethod -Uri "http://localhost:8080/api/advisor/tasks/$($task.data.taskId)" `
  -Method Get
```

Expected status flow:

- `PENDING`: task row created and stream message published after commit.
- `PROCESSING`: worker has started the synchronous advisor analysis chain.
- `COMPLETED`: report was generated; `reportId` points to `stock_advisor_report`.
- `FAILED`: worker caught an exception and saved the error message.

## Enable Real LLM Agents

By default, the backend only returns realtime quote data and empty Agent sections. Enable LangChain4j Agent calls with environment variables:

```powershell
$env:ADVISOR_LLM_ENABLED="true"
$env:ADVISOR_LLM_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
$env:ADVISOR_LLM_API_KEY="<your-api-key>"
$env:ADVISOR_LLM_MODEL="qwen-plus"
$env:ADVISOR_LLM_TEMPERATURE="0.2"
$env:ADVISOR_LLM_TIMEOUT="60s"

.\gradlew.bat :app:bootRun --console=plain
```

Do not commit real API keys. If `ADVISOR_LLM_ENABLED=true` but `ADVISOR_LLM_API_KEY` is blank, startup fails with a configuration error.

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
    "fundamentalView": "LLM generated text when ADVISOR_LLM_ENABLED=true",
    "technicalView": "LLM generated text when ADVISOR_LLM_ENABLED=true",
    "valuationView": "LLM generated text when ADVISOR_LLM_ENABLED=true",
    "newsView": "LLM generated text when ADVISOR_LLM_ENABLED=true",
    "riskView": "LLM generated text when ADVISOR_LLM_ENABLED=true",
    "conclusion": "...不构成投资建议...",
    "evidences": []
  }
}
```

The endpoint calls Sina Finance for realtime quote data. If the public Sina endpoint is unavailable, the API returns an error until a fallback data source is added.

## Report History APIs

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/advisor/reports?stockCode=600519" `
  -Method Get
```

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/advisor/reports/1" `
  -Method Get
```

Expected response:

- `code` is `200`
- `success` is `true`
- list items include `id`, `stockCode`, `stockName`, `quoteSummary`, and `createdAt`

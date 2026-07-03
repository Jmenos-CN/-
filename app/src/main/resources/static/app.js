const state = {
  currentStockCode: '600519',
  pollingTimer: null
};

const form = document.querySelector('#advisor-form');
const historyForm = document.querySelector('#history-form');
const reportPanel = document.querySelector('#report-panel');
const historyList = document.querySelector('#history-list');
const statusBadge = document.querySelector('#status');
const taskCard = document.querySelector('#task-card');
const taskStatus = document.querySelector('#task-status');
const taskIdNode = document.querySelector('#task-id');
const taskReportIdNode = document.querySelector('#task-report-id');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const query = document.querySelector('#query').value.trim();
  const analysisType = document.querySelector('#analysisType').value;
  if (!query) {
    setStatus('璇疯緭鍏ヨ姹?, 'bad');
    return;
  }
  await runAsyncReportFlow(query, analysisType);
});

historyForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const stockCode = document.querySelector('#history-stock-code').value.trim();
  if (!stockCode) {
    setStatus('璇疯緭鍏ヨ偂绁ㄤ唬鐮?, 'bad');
    return;
  }
  await loadHistory(stockCode);
});

async function runAsyncReportFlow(query, analysisType) {
  clearPollingTimer();
  showTaskState({ taskId: '-', status: 'CREATING', reportId: null });
  setStatus('鍒涘缓浠诲姟', 'warn');
  try {
    const task = await createAsyncTask(query, analysisType);
    showTaskState(task);
    setStatus('浠诲姟杞涓?, 'warn');
    const completedTask = await pollTaskUntilDone(task.taskId);
    showTaskState(completedTask);
    await loadCompletedReport(completedTask.reportId);
    setStatus('鎶ュ憡宸茬敓鎴?, 'good');
  } catch (error) {
    setStatus(error.message || '寮傛浠诲姟澶辫触', 'bad');
    showTaskState({ taskId: taskIdNode.textContent, status: 'FAILED', reportId: null, errorMessage: error.message });
  }
}

async function createAsyncTask(query, analysisType) {
  const response = await requestJson('/api/advisor/tasks', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify({ query, analysisType })
  });
  return response.data;
}

async function pollTaskUntilDone(taskId) {
  const maxAttempts = 60;
  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const response = await requestJson(`/api/advisor/tasks/${encodeURIComponent(taskId)}`);
    const task = response.data;
    showTaskState(task);
    if (task.status === 'COMPLETED') {
      if (!task.reportId) {
        throw new Error('浠诲姟瀹屾垚浣嗙己灏戞姤鍛?ID');
      }
      return task;
    }
    if (task.status === 'FAILED') {
      throw new Error(task.errorMessage || '浠诲姟鎵ц澶辫触');
    }
    await delay(1000);
  }
  throw new Error('浠诲姟杞瓒呮椂');
}

async function loadCompletedReport(reportId) {
  const response = await requestJson(`/api/advisor/reports/${encodeURIComponent(reportId)}`);
  renderReport(response.data);
  state.currentStockCode = response.data.stockCode;
  document.querySelector('#history-stock-code').value = response.data.stockCode;
  await loadHistory(response.data.stockCode, { quiet: true });
}

async function analyzeReport(query, analysisType) {
  const response = await requestJson('/api/advisor/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify({ query, analysisType })
  });
  renderReport(response.data);
  state.currentStockCode = response.data.stockCode;
  document.querySelector('#history-stock-code').value = response.data.stockCode;
  await loadHistory(response.data.stockCode, { quiet: true });
  return response.data;
}

async function loadHistory(stockCode, options = {}) {
  if (!options.quiet) {
    setStatus('鏌ヨ鍘嗗彶', 'warn');
  }
  const response = await requestJson(`/api/advisor/reports?stockCode=${encodeURIComponent(stockCode)}`);
  renderHistory(response.data || []);
  if (!options.quiet) {
    setStatus('鍘嗗彶宸叉洿鏂?, 'good');
  }
}

async function loadReportDetail(id) {
  setStatus('鍔犺浇璇︽儏', 'warn');
  const response = await requestJson(`/api/advisor/reports/${id}`);
  renderReport(response.data);
  showTaskState({ taskId: '-', status: 'DETAIL_LOADED', reportId: id });
  setStatus('璇︽儏宸插姞杞?, 'good');
}

async function requestJson(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  const payload = await response.json();
  if (!payload.success) {
    throw new Error(payload.message || '璇锋眰澶辫触');
  }
  return payload;
}

function renderReport(report) {
  const template = document.querySelector('#report-template');
  const node = template.content.cloneNode(true);
  node.querySelector('.stock-code').textContent = `${report.stockCode || '-'} 路 ${formatTime(report.analysisTime)}`;
  node.querySelector('.stock-name').textContent = report.stockName || '-';
  node.querySelector('.conclusion').textContent = report.conclusion || '鏆傛棤缁撹';
  node.querySelector('#quality-card').replaceWith(renderQuality(report.quality));
  node.querySelector('.quality-detail').append(renderQualityWarnings(report.quality));
  node.querySelector('#insight-list').replaceChildren(...renderInsights(report.insights || []));
  node.querySelector('#evidence-list').replaceChildren(...renderEvidences(report.evidences || []));
  reportPanel.replaceChildren(node);
}

function renderQuality(quality) {
  const score = Number(quality?.qualityScore ?? 0);
  const card = document.createElement('div');
  card.id = 'quality-card';
  card.className = `quality-card ${qualityLevel(score)}`;
  card.innerHTML = `<span class="quality-score">${score}</span><span>璇佹嵁璐ㄩ噺</span>`;
  return card;
}

function renderQualityWarnings(quality) {
  const fragment = document.createDocumentFragment();
  const warnings = quality?.warnings || [];
  const missing = quality?.missingEvidenceTypes || [];
  if (warnings.length === 0 && missing.length === 0) {
    const p = document.createElement('p');
    p.textContent = '鏍稿績璇佹嵁瀹屾暣锛屽綋鍓嶆姤鍛婂叿澶囪緝濂界殑瑙ｉ噴鍩虹銆?;
    fragment.append(p);
    return fragment;
  }
  const list = document.createElement('ul');
  [...warnings, ...missing.map((type) => `缂哄皯 ${type} 璇佹嵁`)].forEach((item) => {
    const li = document.createElement('li');
    li.textContent = item;
    list.append(li);
  });
  fragment.append(list);
  return fragment;
}

function renderInsights(insights) {
  if (insights.length === 0) {
    return [emptyBlock('鏆傛棤鍙В閲婃礊瀵?)];
  }
  return insights.map((insight, index) => {
    const details = document.createElement('details');
    details.open = index === 0;
    const evidence = (insight.supportingEvidence || []).map((item) => `<span class="tag">${escapeHtml(item)}</span>`).join('');
    details.innerHTML = `
      <summary>${escapeHtml(insight.type || 'INSIGHT')} 路 ${escapeHtml(insight.title || '')}</summary>
      <p>${escapeHtml(insight.summary || '鏆傛棤璇存槑')}</p>
      <div class="meta-row">
        <span class="tag">椋庨櫓锛?{escapeHtml(insight.riskLevel || '-')}</span>
        <span class="tag">缃俊搴︼細${escapeHtml(String(insight.confidence ?? '-'))}</span>
      </div>
      <div class="meta-row">${evidence}</div>
    `;
    return details;
  });
}

function renderEvidences(evidences) {
  if (evidences.length === 0) {
    return [emptyBlock('鏆傛棤璇佹嵁閾?)];
  }
  return evidences.map((evidence) => {
    const item = document.createElement('article');
    item.className = 'evidence-item';
    item.innerHTML = `
      <strong>${escapeHtml(evidence.source || 'Unknown Source')}</strong>
      <div>${escapeHtml(evidence.title || '-')}</div>
      <div class="evidence-value">${escapeHtml(evidence.value || '-')}</div>
      <div class="meta-row"><span class="tag">${escapeHtml(formatTime(evidence.fetchedAt))}</span></div>
    `;
    return item;
  });
}

function renderHistory(reports) {
  if (reports.length === 0) {
    historyList.replaceChildren(emptyBlock('鏆傛棤鍘嗗彶鎶ュ憡'));
    return;
  }
  historyList.replaceChildren(...reports.map((report) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'history-item';
    button.innerHTML = `
      <strong>${escapeHtml(report.stockCode)} 路 ${escapeHtml(report.stockName || '')}</strong>
      <div class="evidence-value">${escapeHtml(report.quoteSummary || '')}</div>
      <div class="meta-row"><span class="tag">${escapeHtml(formatTime(report.createdAt))}</span></div>
    `;
    button.addEventListener('click', () => loadReportDetail(report.id));
    return button;
  }));
}

function showTaskState(task) {
  taskCard.hidden = false;
  taskStatus.textContent = task.errorMessage ? `${task.status}: ${task.errorMessage}` : task.status;
  taskIdNode.textContent = task.taskId ? `task ${task.taskId}` : 'task -';
  taskReportIdNode.textContent = task.reportId ? `report ${task.reportId}` : 'report -';
}

function clearPollingTimer() {
  if (state.pollingTimer) {
    clearTimeout(state.pollingTimer);
    state.pollingTimer = null;
  }
}

function delay(milliseconds) {
  return new Promise((resolve) => {
    state.pollingTimer = setTimeout(resolve, milliseconds);
  });
}

function emptyBlock(message) {
  const block = document.createElement('div');
  block.className = 'quality-detail';
  block.textContent = message;
  return block;
}

function qualityLevel(score) {
  if (score >= 80) {
    return 'good';
  }
  if (score >= 50) {
    return 'warn';
  }
  return 'bad';
}

function formatTime(value) {
  if (!value) {
    return '-';
  }
  return String(value).replace('T', ' ').slice(0, 19);
}

function escapeHtml(value) {
  return String(value)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
}

function setStatus(text, level = '') {
  statusBadge.textContent = text;
  statusBadge.className = `status ${level}`;
}

window.addEventListener('error', (event) => {
  setStatus(event.message || '椤甸潰閿欒', 'bad');
});

loadHistory(state.currentStockCode, { quiet: true }).catch(() => {
  historyList.replaceChildren(emptyBlock('鍘嗗彶鎶ュ憡鎺ュ彛鏆備笉鍙敤'));
});

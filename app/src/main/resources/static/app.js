const state = {
  currentStockCode: '600519'
};

const form = document.querySelector('#advisor-form');
const historyForm = document.querySelector('#history-form');
const reportPanel = document.querySelector('#report-panel');
const historyList = document.querySelector('#history-list');
const statusBadge = document.querySelector('#status');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const query = document.querySelector('#query').value.trim();
  const analysisType = document.querySelector('#analysisType').value;
  if (!query) {
    setStatus('请输入请求', 'bad');
    return;
  }
  await analyzeReport(query, analysisType);
});

historyForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const stockCode = document.querySelector('#history-stock-code').value.trim();
  if (!stockCode) {
    setStatus('请输入股票代码', 'bad');
    return;
  }
  await loadHistory(stockCode);
});

async function analyzeReport(query, analysisType) {
  setStatus('分析中', 'warn');
  const response = await requestJson('/api/advisor/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify({ query, analysisType })
  });
  renderReport(response.data);
  state.currentStockCode = response.data.stockCode;
  document.querySelector('#history-stock-code').value = response.data.stockCode;
  await loadHistory(response.data.stockCode, { quiet: true });
  setStatus('已生成', 'good');
}

async function loadHistory(stockCode, options = {}) {
  if (!options.quiet) {
    setStatus('查询历史', 'warn');
  }
  const response = await requestJson(`/api/advisor/reports?stockCode=${encodeURIComponent(stockCode)}`);
  renderHistory(response.data || []);
  if (!options.quiet) {
    setStatus('历史已更新', 'good');
  }
}

async function loadReportDetail(id) {
  setStatus('加载详情', 'warn');
  const response = await requestJson(`/api/advisor/reports/${id}`);
  renderReport(response.data);
  setStatus('详情已加载', 'good');
}

async function requestJson(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  const payload = await response.json();
  if (!payload.success) {
    throw new Error(payload.message || '请求失败');
  }
  return payload;
}

function renderReport(report) {
  const template = document.querySelector('#report-template');
  const node = template.content.cloneNode(true);
  node.querySelector('.stock-code').textContent = `${report.stockCode || '-'} · ${formatTime(report.analysisTime)}`;
  node.querySelector('.stock-name').textContent = report.stockName || '-';
  node.querySelector('.conclusion').textContent = report.conclusion || '暂无结论';
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
  card.innerHTML = `<span class="quality-score">${score}</span><span>证据质量</span>`;
  return card;
}

function renderQualityWarnings(quality) {
  const fragment = document.createDocumentFragment();
  const warnings = quality?.warnings || [];
  const missing = quality?.missingEvidenceTypes || [];
  if (warnings.length === 0 && missing.length === 0) {
    const p = document.createElement('p');
    p.textContent = '核心证据完整，当前报告具备较好的解释基础。';
    fragment.append(p);
    return fragment;
  }
  const list = document.createElement('ul');
  [...warnings, ...missing.map((type) => `缺少 ${type} 证据`)].forEach((item) => {
    const li = document.createElement('li');
    li.textContent = item;
    list.append(li);
  });
  fragment.append(list);
  return fragment;
}

function renderInsights(insights) {
  if (insights.length === 0) {
    return [emptyBlock('暂无可解释洞察')];
  }
  return insights.map((insight, index) => {
    const details = document.createElement('details');
    details.open = index === 0;
    const evidence = (insight.supportingEvidence || []).map((item) => `<span class="tag">${escapeHtml(item)}</span>`).join('');
    details.innerHTML = `
      <summary>${escapeHtml(insight.type || 'INSIGHT')} · ${escapeHtml(insight.title || '')}</summary>
      <p>${escapeHtml(insight.summary || '暂无说明')}</p>
      <div class="meta-row">
        <span class="tag">风险：${escapeHtml(insight.riskLevel || '-')}</span>
        <span class="tag">置信度：${escapeHtml(String(insight.confidence ?? '-'))}</span>
      </div>
      <div class="meta-row">${evidence}</div>
    `;
    return details;
  });
}

function renderEvidences(evidences) {
  if (evidences.length === 0) {
    return [emptyBlock('暂无证据链')];
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
    historyList.replaceChildren(emptyBlock('暂无历史报告'));
    return;
  }
  historyList.replaceChildren(...reports.map((report) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'history-item';
    button.innerHTML = `
      <strong>${escapeHtml(report.stockCode)} · ${escapeHtml(report.stockName || '')}</strong>
      <div class="evidence-value">${escapeHtml(report.quoteSummary || '')}</div>
      <div class="meta-row"><span class="tag">${escapeHtml(formatTime(report.createdAt))}</span></div>
    `;
    button.addEventListener('click', () => loadReportDetail(report.id));
    return button;
  }));
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
  setStatus(event.message || '页面错误', 'bad');
});

loadHistory(state.currentStockCode, { quiet: true }).catch(() => {
  historyList.replaceChildren(emptyBlock('历史报告接口暂不可用'));
});

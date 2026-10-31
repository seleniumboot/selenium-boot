// ── Tab navigation ──
function showTab(tabId, el) {
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    document.getElementById('tab-' + tabId).classList.add('active');
    if (el) el.classList.add('active');
}
// ── Lightbox ──
function openLightbox(src) {
    document.getElementById('lightbox-img').src = src;
    document.getElementById('lightbox').classList.add('open');
    document.body.style.overflow = 'hidden';
}
function closeLightbox() {
    document.getElementById('lightbox').classList.remove('open');
    document.getElementById('lightbox-img').src = '';
    document.body.style.overflow = '';
}
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') closeLightbox();
});

// ── Dark mode ──
(function() {
    try { if (localStorage.getItem('sb-theme') === 'dark') document.documentElement.setAttribute('data-theme', 'dark'); } catch(e) {}
})();
function toggleDark() {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    document.documentElement.setAttribute('data-theme', isDark ? 'light' : 'dark');
    try { localStorage.setItem('sb-theme', isDark ? 'light' : 'dark'); } catch(e) {}
}

// ── Charts ──
const executionData = JSON.parse('{{EXECUTION_PERCENTILES}}');
const donutData     = JSON.parse('{{DONUT_DATA}}');
const themeColors = getComputedStyle(document.documentElement);
const color = name => themeColors.getPropertyValue(name).trim();
const chartFontFamily = 'Roboto, "Segoe UI", Arial, sans-serif';
function safeVal(obj, key) { return obj && obj[key] ? obj[key] : 0; }
if (window.Chart) {
    new Chart(document.getElementById('donutChart'), {
        type: 'doughnut',
        data: {
            labels: ['Passed', 'Failed', 'Skipped'],
            datasets: [{ data: [donutData.passed, donutData.failed, donutData.skipped],
                backgroundColor: [color('--good'), color('--bad'), color('--warn')],
                borderWidth: 2, borderColor: color('--surface') }]
        },
        options: { responsive: true, maintainAspectRatio: true, cutout: '65%',
            plugins: { legend: { position: 'bottom', labels: { font: { family: chartFontFamily, size: 12 } } } } }
    });
    new Chart(document.getElementById('executionChart'), {
        type: 'bar',
        data: {
            labels: ['P50', 'P90', 'P95', 'P99'],
            datasets: [{ label: 'Execution Time (ms)',
                data: [safeVal(executionData,'p50'), safeVal(executionData,'p90'),
                       safeVal(executionData,'p95'), safeVal(executionData,'p99')],
                backgroundColor: color('--primary') + '40', borderColor: color('--primary'),
                borderWidth: 1, borderRadius: 4 }]
        },
        options: { responsive: true, maintainAspectRatio: false,
            plugins: { legend: { labels: { font: { family: chartFontFamily, size: 13 } } } },
            scales: { x: { grid: { display: false } }, y: { beginAtZero: true } } }
    });
}

// ── Trend over runs ──
const trendData = JSON.parse('{{TREND_DATA}}');
if (window.Chart && trendData.length >= 2) {
    document.getElementById('trendRow').style.display = '';
    const labels = trendData.map(r => r.label);
    const line = (el, label, data, color, max) => new Chart(document.getElementById(el), {
        type: 'line',
        data: { labels, datasets: [{ label, data, borderColor: color, backgroundColor: color + '33',
            fill: true, tension: .3, pointRadius: 3 }] },
        options: { responsive: true, maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { x: { grid: { display: false } }, y: { beginAtZero: true, max } } }
    });
    line('trendRateChart', 'Pass rate (%)', trendData.map(r => r.passRate), color('--good'), 100);
    line('trendTimeChart', 'Duration (s)', trendData.map(r => r.durationSec), color('--primary'));
    line('trendFlakyChart', 'Flaky tests', trendData.map(r => r.flakyTests), color('--warn'));
}

// ── Collapse / expand class groups ──
function toggleGroup(groupId) {
    const members = document.querySelectorAll(`.group-member[data-group="${groupId}"]`);
    const icon    = document.getElementById('gicon-' + groupId);
    const isOpen  = icon && !icon.classList.contains('closed');
    members.forEach(r => { r.style.display = isOpen ? 'none' : ''; });
    if (icon) icon.classList.toggle('closed', isOpen);
}

// ── Expand / collapse detail rows ──
function toggleDetail(idx) {
    const row  = document.getElementById('detail-' + idx);
    const icon = document.getElementById('icon-' + idx);
    if (!row) return;
    const isOpen = row.style.display !== 'none';
    row.style.display = isOpen ? 'none' : '';
    if (icon) icon.classList.toggle('open', !isOpen);
}

// ── Tag filtering ──
let activeTagFilter = '';
function filterByTag(tagName, event) {
    if (event) event.stopPropagation();
    activeTagFilter = (tagName || '').toLowerCase().trim();
    showTab('tests', document.querySelector('.nav-item[onclick*=\'tests\']'));
    const pill = document.getElementById('activeTagPill');
    const nameSpan = document.getElementById('activeTagName');
    if (pill && nameSpan) {
        if (activeTagFilter) {
            nameSpan.textContent = 'Tag: ' + tagName;
            pill.style.display = 'inline-flex';
        } else {
            pill.style.display = 'none';
        }
    }
    try {
        if (activeTagFilter) {
            localStorage.setItem('sb-tag-filter', tagName);
        } else {
            localStorage.removeItem('sb-tag-filter');
        }
    } catch (e) {}
    applySearch();
}
function clearTagFilter() {
    activeTagFilter = '';
    const pill = document.getElementById('activeTagPill');
    if (pill) pill.style.display = 'none';
    try {
        localStorage.removeItem('sb-tag-filter');
    } catch (e) {}
    applySearch();
}

// ── Filter + search (Test Cases tab) ──
let activeFilter = 'ALL';
function setFilter(status, btn) {
    activeFilter = status;
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    try {
        if (status === 'ALL') {
            localStorage.removeItem('sb-status-filter');
        } else {
            localStorage.setItem('sb-status-filter', status);
        }
    } catch (e) {}
    applySearch();
}
function applySearch() {
    const q = document.getElementById('searchInput').value.toLowerCase();
    const members = document.querySelectorAll('#testTable tbody tr.group-member:not(.detail-row)');
    const visibleGroups = new Set();
    members.forEach(row => {
        const rowStatus = row.getAttribute('data-status') || '';
        const rowTest   = row.getAttribute('data-test')   || '';
        const groupId   = row.getAttribute('data-group')  || '';
        const rowTags   = (row.getAttribute('data-tags')  || '').split(',').map(t => t.trim().toLowerCase()).filter(Boolean);
        const hasRetry  = row.querySelector('.retry-badge') !== null;
        const matchFilter = activeFilter === 'ALL'
            || activeFilter === rowStatus
            || (activeFilter === 'FLAKY' && hasRetry);
        const matchTag = !activeTagFilter || rowTags.includes(activeTagFilter);
        const matchSearch = !q
            || rowTest.includes(q)
            || groupId.toLowerCase().includes(q)
            || rowTags.some(t => t.includes(q));
        const show = matchFilter && matchTag && matchSearch;
        row.style.display = show ? '' : 'none';
        if (show) visibleGroups.add(groupId);
        const next = row.nextElementSibling;
        if (next && next.classList.contains('detail-row') && !show) next.style.display = 'none';
    });
    document.querySelectorAll('#testTable tbody tr.group-header').forEach(hdr => {
        const groupId = hdr.getAttribute('data-group') || '';
        hdr.style.display = visibleGroups.has(groupId) ? '' : 'none';
    });
}

// ── Filter restoration on load ──
(function() {
    try {
        // Restore status filter
        const savedStatus = localStorage.getItem('sb-status-filter');
        if (savedStatus && ['PASSED', 'FAILED', 'SKIPPED', 'FLAKY'].includes(savedStatus)) {
            const btn = document.querySelector(".filter-btn[onclick*=\"'" + savedStatus + "'\"]");
            if (btn) {
                activeFilter = savedStatus;
                document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
            }
        } else if (savedStatus === 'ALL') {
            const btn = document.querySelector(".filter-btn[onclick*=\"'ALL'\"]");
            if (btn) {
                activeFilter = 'ALL';
                document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
            }
        }

        // Restore tag filter
        const savedTag = localStorage.getItem('sb-tag-filter');
        if (savedTag) {
            const tagToMatch = savedTag.toLowerCase().trim();
            if (tagToMatch) {
                const rows = document.querySelectorAll('#testTable tbody tr.group-member');
                let tagFound = false;
                for (let i = 0; i < rows.length; i++) {
                    const rawTags = rows[i].getAttribute('data-tags');
                    if (rawTags) {
                        const tags = rawTags.split(',').map(t => t.trim().toLowerCase());
                        if (tags.includes(tagToMatch)) {
                            tagFound = true;
                            break;
                        }
                    }
                }
                if (tagFound) {
                    activeTagFilter = tagToMatch;
                    const pill = document.getElementById('activeTagPill');
                    const nameSpan = document.getElementById('activeTagName');
                    if (pill && nameSpan) {
                        nameSpan.textContent = 'Tag: ' + savedTag;
                        pill.style.display = 'inline-flex';
                    }
                }
            }
        }

        // Apply filters to table if any restored
        if (activeFilter !== 'ALL' || activeTagFilter) {
            applySearch();
        }
    } catch (e) {}
})();


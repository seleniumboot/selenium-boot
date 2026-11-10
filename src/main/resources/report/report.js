// ── Tab navigation ──
function showTab(tabId, el) {
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    document.getElementById('tab-' + tabId).classList.add('active');
    if (el) el.classList.add('active');
}
// ── Lightbox ──
var lightboxImgs = [];
var lightboxIdx = 0;
// Screenshots of the same test: its row thumbnail plus the thumbnails in its detail row.
function collectTestScreenshots(img) {
    var row = img.closest('tr');
    if (!row) return [img];
    var testRow = row.classList.contains('detail-row') ? row.previousElementSibling : row;
    var detailRow = testRow && testRow.nextElementSibling;
    var rows = [testRow];
    if (detailRow && detailRow.classList.contains('detail-row')) rows.push(detailRow);
    var imgs = [];
    rows.forEach(function(r) {
        if (r) r.querySelectorAll('img.screenshot-thumb, img.step-thumb').forEach(function(i) { imgs.push(i); });
    });
    return imgs.indexOf(img) >= 0 ? imgs : [img];
}
function showLightbox() {
    document.getElementById('lightbox-img').src = lightboxImgs[lightboxIdx].src;
    var multi = lightboxImgs.length > 1;
    document.getElementById('lightbox-counter').textContent = multi ? (lightboxIdx + 1) + ' / ' + lightboxImgs.length : '';
    document.getElementById('lightbox-prev').style.display = multi ? '' : 'none';
    document.getElementById('lightbox-next').style.display = multi ? '' : 'none';
    document.getElementById('lightbox-prev').disabled = lightboxIdx === 0;
    document.getElementById('lightbox-next').disabled = lightboxIdx === lightboxImgs.length - 1;
}
function openLightbox(img) {
    lightboxImgs = collectTestScreenshots(img);
    lightboxIdx = lightboxImgs.indexOf(img);
    showLightbox();
    document.getElementById('lightbox').classList.add('open');
    document.body.style.overflow = 'hidden';
}
function stepLightbox(delta, e) {
    if (e) e.stopPropagation();
    var next = lightboxIdx + delta;
    if (next < 0 || next >= lightboxImgs.length) return;
    lightboxIdx = next;
    showLightbox();
}
function closeLightbox() {
    document.getElementById('lightbox').classList.remove('open');
    document.getElementById('lightbox-img').src = '';
    document.body.style.overflow = '';
    lightboxImgs = [];
}
document.addEventListener('keydown', function(e) {
    if (!document.getElementById('lightbox').classList.contains('open')) return;
    if (e.key === 'Escape') closeLightbox();
    else if (e.key === 'ArrowLeft') stepLightbox(-1);
    else if (e.key === 'ArrowRight') stepLightbox(1);
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
const donutData     = {{DONUT_DATA}};
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
const trendData = {{TREND_DATA}};
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
    // Hide the flaky card entirely when no run in the history had flaky tests.
    if (trendData.some(r => r.flakyTests > 0)) {
        line('trendFlakyChart', 'Flaky tests', trendData.map(r => r.flakyTests), color('--warn'));
    } else {
        document.getElementById('trendFlakyCard').style.display = 'none';
    }
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

// ── Expand all / collapse all (tests matching the active filter and search) ──
function setAllDetails(open) {
    const q = document.getElementById('searchInput').value.toLowerCase();
    if (open) {
        document.querySelectorAll('#testTable tbody tr.group-header').forEach(hdr => {
            if (hdr.style.display === 'none') return;
            const icon = document.getElementById('gicon-' + hdr.getAttribute('data-group'));
            if (icon) icon.classList.remove('closed');
        });
    }
    document.querySelectorAll('#testTable tbody tr.group-member.expandable').forEach(row => {
        if (!rowMatches(row, q) || !groupIsOpen(row.getAttribute('data-group'))) return;
        const detail = row.nextElementSibling;
        if (!detail || !detail.classList.contains('detail-row')) return;
        row.style.display = '';
        detail.style.display = open ? '' : 'none';
        const icon = row.querySelector('.expand-icon');
        if (icon) icon.classList.toggle('open', open);
    });
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
function rowMatches(row, q) {
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
    return matchFilter && matchTag && matchSearch;
}
function groupIsOpen(groupId) {
    const icon = document.getElementById('gicon-' + groupId);
    return !icon || !icon.classList.contains('closed');
}
function applySearch() {
    const q = document.getElementById('searchInput').value.toLowerCase();
    const members = document.querySelectorAll('#testTable tbody tr.group-member:not(.detail-row)');
    const visibleGroups = new Set();
    members.forEach(row => {
        const groupId = row.getAttribute('data-group') || '';
        const match = rowMatches(row, q);
        const show = match && groupIsOpen(groupId);
        row.style.display = show ? '' : 'none';
        if (match) visibleGroups.add(groupId);
        const next = row.nextElementSibling;
        if (next && next.classList.contains('detail-row') && !show) next.style.display = 'none';
    });
    document.querySelectorAll('#testTable tbody tr.group-header').forEach(hdr => {
        const groupId = hdr.getAttribute('data-group') || '';
        hdr.style.display = visibleGroups.has(groupId) ? '' : 'none';
    });
}

// ── Column sorting (Test Cases tab) ──
let currentSortCol = null;
let currentSortDir = 'asc';

function getNumericVal(row, attr, fallbackIndex) {
    const val = row.getAttribute(attr);
    if (val !== null && val !== '') {
        const n = parseFloat(val);
        if (!isNaN(n)) return n;
    }
    const numericCells = row.querySelectorAll('td.numeric');
    if (numericCells.length > fallbackIndex) {
        const n = parseFloat(numericCells[fallbackIndex].textContent.trim());
        if (!isNaN(n)) return n;
    }
    return 0;
}

function compareTestRows(pairA, pairB, colKey, dir) {
    const rowA = pairA.testRow;
    const rowB = pairB.testRow;
    let cmp = 0;

    if (colKey === 'test') {
        const a = (rowA.getAttribute('data-test') || '').toLowerCase();
        const b = (rowB.getAttribute('data-test') || '').toLowerCase();
        cmp = a.localeCompare(b, undefined, { numeric: true, sensitivity: 'base' });
    } else if (colKey === 'status') {
        const a = (rowA.getAttribute('data-status') || '').toUpperCase();
        const b = (rowB.getAttribute('data-status') || '').toUpperCase();
        cmp = a.localeCompare(b);
        if (cmp === 0) {
            const testA = (rowA.getAttribute('data-test') || '').toLowerCase();
            const testB = (rowB.getAttribute('data-test') || '').toLowerCase();
            cmp = testA.localeCompare(testB, undefined, { numeric: true, sensitivity: 'base' });
        }
    } else if (colKey === 'logic') {
        const a = getNumericVal(rowA, 'data-logic-ms', 0);
        const b = getNumericVal(rowB, 'data-logic-ms', 0);
        cmp = a - b;
        if (cmp === 0) {
            const testA = (rowA.getAttribute('data-test') || '').toLowerCase();
            const testB = (rowB.getAttribute('data-test') || '').toLowerCase();
            cmp = testA.localeCompare(testB, undefined, { numeric: true, sensitivity: 'base' });
        }
    } else if (colKey === 'total') {
        const a = getNumericVal(rowA, 'data-total-ms', 1);
        const b = getNumericVal(rowB, 'data-total-ms', 1);
        cmp = a - b;
        if (cmp === 0) {
            const testA = (rowA.getAttribute('data-test') || '').toLowerCase();
            const testB = (rowB.getAttribute('data-test') || '').toLowerCase();
            cmp = testA.localeCompare(testB, undefined, { numeric: true, sensitivity: 'base' });
        }
    }

    return dir === 'desc' ? -cmp : cmp;
}

function updateSortHeaders(activeColKey, dir) {
    const headers = document.querySelectorAll('#testTable thead th.sortable');
    headers.forEach(th => {
        const key = th.getAttribute('data-sort-key');
        const icon = th.querySelector('.sort-icon');
        if (key === activeColKey) {
            th.classList.add('active');
            th.setAttribute('aria-sort', dir === 'asc' ? 'ascending' : 'descending');
            th.title = 'Sorted ' + (dir === 'asc' ? 'ascending' : 'descending') + '. Click to reverse.';
            if (icon) {
                icon.classList.remove('asc', 'desc');
                icon.classList.add(dir);
            }
        } else {
            th.classList.remove('active');
            th.setAttribute('aria-sort', 'none');
            th.title = 'Click to sort';
            if (icon) {
                icon.classList.remove('asc', 'desc');
            }
        }
    });
}

function sortTestTable(colKey) {
    const table = document.getElementById('testTable');
    if (!table) return;

    // Toggle direction if already active column; otherwise default to ascending
    if (currentSortCol === colKey) {
        currentSortDir = currentSortDir === 'asc' ? 'desc' : 'asc';
    } else {
        currentSortCol = colKey;
        currentSortDir = 'asc';
    }

    const groupHeaders = document.querySelectorAll('#testTable tbody tr.group-header');

    if (groupHeaders.length > 0) {
        // Group-Preserving sorting: sort test rows within each group
        const allMemberRows = Array.from(document.querySelectorAll('#testTable tbody tr.group-member:not(.detail-row)'));

        groupHeaders.forEach(hdr => {
            const groupId = hdr.getAttribute('data-group');
            const groupMembers = allMemberRows.filter(r => r.getAttribute('data-group') === groupId);

            // Collect atomic test row + detail row pairs
            const pairs = groupMembers.map(testRow => {
                const next = testRow.nextElementSibling;
                const detailRow = (next && next.classList.contains('detail-row')) ? next : null;
                return { testRow, detailRow };
            });

            // Sort pairs within group
            pairs.sort((a, b) => compareTestRows(a, b, colKey, currentSortDir));

            // Re-insert pairs in sorted order immediately after group header
            let anchor = hdr;
            pairs.forEach(pair => {
                anchor.insertAdjacentElement('afterend', pair.testRow);
                anchor = pair.testRow;
                if (pair.detailRow) {
                    anchor.insertAdjacentElement('afterend', pair.detailRow);
                    anchor = pair.detailRow;
                }
            });
        });
    } else {
        // Fallback for ungrouped tables
        const tbody = table.querySelector('tbody');
        if (tbody) {
            const testRows = Array.from(tbody.querySelectorAll('tr.group-member:not(.detail-row)'));
            const pairs = testRows.map(testRow => {
                const next = testRow.nextElementSibling;
                const detailRow = (next && next.classList.contains('detail-row')) ? next : null;
                return { testRow, detailRow };
            });

            pairs.sort((a, b) => compareTestRows(a, b, colKey, currentSortDir));

            pairs.forEach(pair => {
                tbody.appendChild(pair.testRow);
                if (pair.detailRow) tbody.appendChild(pair.detailRow);
            });
        }
    }

    // Update sort indicators on column headers
    updateSortHeaders(currentSortCol, currentSortDir);

    // Refresh visibility respecting active status filters, search query, and tag filters
    applySearch();
}

// ── Copy failure details ──
function copyFailure(btn, event) {
    if (event) event.stopPropagation();
    const panel = btn.closest('.detail-panel');
    const err   = panel.querySelector('.error-msg');
    const trace = panel.querySelector('.stack-trace');
    const text  = [
        'Test: ' + btn.getAttribute('data-test-id'),
        err   ? 'Error: ' + err.textContent : '',
        trace ? '\n' + trace.textContent : ''
    ].filter(Boolean).join('\n');
    const done = ok => {
        const label = btn.getAttribute('data-label') || btn.textContent;
        btn.setAttribute('data-label', label);
        btn.textContent = ok ? 'Copied' : 'Copy failed';
        setTimeout(() => { btn.textContent = label; }, 1500);
    };
    const fallback = () => {
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        let ok = false;
        try { ok = document.execCommand('copy'); } catch (e) {}
        document.body.removeChild(ta);
        done(ok);
    };
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(() => done(true), fallback);
    } else {
        fallback();
    }
}

// ── Export single test as standalone HTML ──
function escapeHtmlForExport(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function buildStandaloneTestHtml(testId, testRow, panel, activeTheme) {
    const styleTag = document.querySelector('style');
    const css = styleTag ? styleTag.textContent : '';

    const hasBrowser = testRow && testRow.querySelector('.browser-cell') !== null;
    const colspan = hasBrowser ? 8 : 7;
    const browserTh = hasBrowser ? '<th>Browser</th>' : '';

    const rawGroupName = testRow ? (testRow.getAttribute('data-group') || '').replace(/^failures:/, '') : '';
    const browser = testRow ? (testRow.getAttribute('data-browser') || (testRow.querySelector('.browser-cell') ? testRow.querySelector('.browser-cell').textContent.trim() : '')) : '';

    let groupTitle = rawGroupName || 'Test Details';
    if (browser && !groupTitle.includes(browser)) {
        groupTitle = groupTitle + ' [' + browser + ']';
    }

    const lastDot = testId.lastIndexOf('.');
    const methodName = lastDot >= 0 ? testId.substring(lastDot + 1) : testId;

    // Clone test row
    let testRowHtml = '';
    if (testRow) {
        const testRowClone = testRow.cloneNode(true);
        testRowClone.style.display = '';
        testRowClone.setAttribute('onclick', 'toggleStandaloneDetail()');
        const icon = testRowClone.querySelector('.expand-icon');
        if (icon) {
            icon.classList.add('open');
        }
        testRowHtml = testRowClone.outerHTML;
    }

    // Clone detail row
    let detailRowHtml = '';
    const detailRow = testRow ? testRow.nextElementSibling : null;
    if (detailRow && detailRow.classList.contains('detail-row')) {
        const detailRowClone = detailRow.cloneNode(true);
        detailRowClone.style.display = '';
        detailRowClone.id = 'standaloneDetailRow';
        const panelClone = detailRowClone.querySelector('.detail-panel');
        if (panelClone) {
            panelClone.querySelectorAll('.copy-failure-btn, .download-test-btn').forEach(function(b) { b.remove(); });
        }
        detailRowHtml = detailRowClone.outerHTML;
    } else if (panel) {
        const panelClone = panel.cloneNode(true);
        panelClone.querySelectorAll('.copy-failure-btn, .download-test-btn').forEach(function(b) { b.remove(); });
        detailRowHtml = '<tr class="detail-row" id="standaloneDetailRow"><td colspan="' + colspan + '">'
            + '<div class="detail-panel">' + panelClone.innerHTML + '</div>'
            + '</td></tr>';
    }

    const groupHeaderHtml = '<tr class="group-header" data-group="' + escapeHtmlForExport(rawGroupName) + '">'
        + '<td colspan="' + colspan + '">'
        + '<span class="group-icon">&#x25BC;</span> <strong>' + escapeHtmlForExport(groupTitle) + '</strong>'
        + '</td></tr>';

    const toggleText = activeTheme === 'dark' ? '&#9728; Light Mode' : '&#9790; Dark Mode';

    const lightboxHtml = '<div class="lightbox" id="lightbox" onclick="closeLightbox()">'
        + '<span class="lightbox-close" onclick="closeLightbox()">&times;</span>'
        + '<button type="button" class="lightbox-nav lightbox-prev" id="lightbox-prev" onclick="stepLightbox(-1, event)" aria-label="Previous screenshot">&#10094;</button>'
        + '<img id="lightbox-img" src="" alt="Screenshot" onclick="event.stopPropagation()">'
        + '<button type="button" class="lightbox-nav lightbox-next" id="lightbox-next" onclick="stepLightbox(1, event)" aria-label="Next screenshot">&#10095;</button>'
        + '<span class="lightbox-counter" id="lightbox-counter"></span>'
        + '</div>';

    const offlineScript = 'var lightboxImgs = [];\n'
        + 'var lightboxIdx = 0;\n'
        + 'function openLightbox(img) {\n'
        + '    lightboxImgs = Array.from(document.querySelectorAll("img.screenshot-thumb, img.step-thumb, img.screenshot-main"));\n'
        + '    lightboxIdx = lightboxImgs.indexOf(img);\n'
        + '    if (lightboxIdx < 0) lightboxIdx = 0;\n'
        + '    showLightbox();\n'
        + '    document.getElementById("lightbox").classList.add("open");\n'
        + '    document.body.style.overflow = "hidden";\n'
        + '}\n'
        + 'function showLightbox() {\n'
        + '    if (!lightboxImgs.length) return;\n'
        + '    document.getElementById("lightbox-img").src = lightboxImgs[lightboxIdx].src;\n'
        + '    var multi = lightboxImgs.length > 1;\n'
        + '    var counter = document.getElementById("lightbox-counter");\n'
        + '    if (counter) counter.textContent = multi ? (lightboxIdx + 1) + " / " + lightboxImgs.length : "";\n'
        + '    var prev = document.getElementById("lightbox-prev");\n'
        + '    var next = document.getElementById("lightbox-next");\n'
        + '    if (prev) { prev.style.display = multi ? "" : "none"; prev.disabled = lightboxIdx === 0; }\n'
        + '    if (next) { next.style.display = multi ? "" : "none"; next.disabled = lightboxIdx === lightboxImgs.length - 1; }\n'
        + '}\n'
        + 'function stepLightbox(delta, e) {\n'
        + '    if (e) e.stopPropagation();\n'
        + '    var next = lightboxIdx + delta;\n'
        + '    if (next < 0 || next >= lightboxImgs.length) return;\n'
        + '    lightboxIdx = next;\n'
        + '    showLightbox();\n'
        + '}\n'
        + 'function closeLightbox() {\n'
        + '    var lb = document.getElementById("lightbox");\n'
        + '    if (lb) lb.classList.remove("open");\n'
        + '    var img = document.getElementById("lightbox-img");\n'
        + '    if (img) img.src = "";\n'
        + '    document.body.style.overflow = "";\n'
        + '}\n'
        + 'document.addEventListener("keydown", function(e) {\n'
        + '    var lb = document.getElementById("lightbox");\n'
        + '    if (!lb || !lb.classList.contains("open")) return;\n'
        + '    if (e.key === "Escape") closeLightbox();\n'
        + '    else if (e.key === "ArrowLeft") stepLightbox(-1);\n'
        + '    else if (e.key === "ArrowRight") stepLightbox(1);\n'
        + '});\n'
        + 'function toggleDark() {\n'
        + '    var isDark = document.documentElement.getAttribute("data-theme") === "dark";\n'
        + '    var newTheme = isDark ? "light" : "dark";\n'
        + '    document.documentElement.setAttribute("data-theme", newTheme);\n'
        + '    var btn = document.querySelector(".dark-toggle");\n'
        + '    if (btn) btn.innerHTML = newTheme === "dark" ? "&#9728; Light Mode" : "&#9790; Dark Mode";\n'
        + '}\n'
        + 'function toggleStandaloneDetail() {\n'
        + '    var row = document.getElementById("standaloneDetailRow");\n'
        + '    var icon = document.querySelector(".expand-icon");\n'
        + '    if (!row) return;\n'
        + '    var isHidden = row.style.display === "none";\n'
        + '    row.style.display = isHidden ? "" : "none";\n'
        + '    if (icon) icon.classList.toggle("open", isHidden);\n'
        + '}\n';

    return '<!DOCTYPE html>\n'
        + '<html lang="en" data-theme="' + escapeHtmlForExport(activeTheme) + '">\n'
        + '<head>\n'
        + '    <meta charset="UTF-8">\n'
        + '    <meta name="viewport" content="width=device-width, initial-scale=1.0">\n'
        + '    <title>' + escapeHtmlForExport(testId || methodName) + ' - Single Test Report</title>\n'
        + '    <' + 'style>\n' + css + '\n    <' + '/style>\n'
        + '</head>\n'
        + '<body>\n'
        + '    <div class="app-bar">\n'
        + '        <div class="app-bar-left">\n'
        + '            <h1>Selenium Boot Report</h1>\n'
        + '            <p>Single Test Export &mdash; ' + escapeHtmlForExport(testId || methodName) + '</p>\n'
        + '        </div>\n'
        + '        <button type="button" class="dark-toggle" onclick="toggleDark()">' + toggleText + '</button>\n'
        + '    </div>\n'
        + '    <div class="layout" style="justify-content: center;">\n'
        + '        <main class="main-content" style="max-width: 1400px; width: 100%; margin: 0 auto; padding: 24px 20px;">\n'
        + '            <div class="tab-heading">Test Cases</div>\n'
        + '            <div class="card table-card">\n'
        + '                <div class="table-wrap">\n'
        + '                    <table id="testTable">\n'
        + '                        <thead>\n'
        + '                            <tr>\n'
        + '                                <th>Test</th>\n'
        + '                                <th>Description</th>\n'
        + '                                ' + browserTh + '\n'
        + '                                <th>Status</th>\n'
        + '                                <th>Logic (ms)</th>\n'
        + '                                <th>Total (ms)</th>\n'
        + '                                <th>Screenshot</th>\n'
        + '                                <th>Detail</th>\n'
        + '                            </tr>\n'
        + '                        </thead>\n'
        + '                        <tbody>\n'
        + '                            ' + groupHeaderHtml + '\n'
        + '                            ' + testRowHtml + '\n'
        + '                            ' + detailRowHtml + '\n'
        + '                        </tbody>\n'
        + '                    </table>\n'
        + '                </div>\n'
        + '            </div>\n'
        + '        </main>\n'
        + '    </div>\n'
        + '    ' + lightboxHtml + '\n'
        + '    <' + 'script>\n' + offlineScript + '    <' + '/script>\n'
        + '</body>\n'
        + '</html>';
}

function downloadTest(btn, event) {
    if (event) event.stopPropagation();
    const panel = btn.closest('.detail-panel');
    const detailRow = btn.closest('tr.detail-row');
    const testRow = detailRow ? detailRow.previousElementSibling : null;
    if (!panel || !testRow) return;

    const label = btn.getAttribute('data-label') || btn.textContent;
    btn.setAttribute('data-label', label);
    btn.textContent = 'Exporting...';
    btn.disabled = true;

    try {
        const testId = btn.getAttribute('data-download-id') || btn.getAttribute('data-test-id') || testRow.getAttribute('data-test') || 'test';
        const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
        const activeTheme = isDark ? 'dark' : 'light';
        const htmlDoc = buildStandaloneTestHtml(testId, testRow, panel, activeTheme);

        const filename = (testId || 'test').replace(/[^a-zA-Z0-9._-]/g, '_') + '.html';
        const blob = new Blob([htmlDoc], { type: 'text/html;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(function() { URL.revokeObjectURL(url); }, 1000);
        btn.textContent = 'Downloaded';
    } catch (e) {
        console.error('Failed to export test:', e);
        btn.textContent = 'Export failed';
    } finally {
        setTimeout(function() {
            btn.textContent = label;
            btn.disabled = false;
        }, 1500);
    }
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


// ── Print handling ──
let printState = null;

// Prepare the complete report for printing.
window.addEventListener('beforeprint', function () {
    // 1. Save interactive state before modifying anything
    printState = {
        // Tab state
        activeTabPaneId: (function() {
            const activePane = document.querySelector('.tab-pane.active');
            return activePane ? activePane.id : 'tab-dashboard';
        })(),
        activeNavItemIndex: (function() {
            const items = Array.from(document.querySelectorAll('.nav-item'));
            return items.findIndex(item => item.classList.contains('active'));
        })(),
        // Filter and search state
        activeFilter: activeFilter,
        activeTagFilter: activeTagFilter,
        searchValue: (function() {
            const searchInput = document.getElementById('searchInput');
            return searchInput ? searchInput.value : '';
        })(),
        activeFilterBtnIndex: (function() {
            const btns = Array.from(document.querySelectorAll('.filter-btn'));
            return btns.findIndex(b => b.classList.contains('active'));
        })(),
        activeTagPillDisplay: (function() {
            const pill = document.getElementById('activeTagPill');
            return pill ? pill.style.display : 'none';
        })(),
        // Group row and icon states
        groupMembers: Array.from(document.querySelectorAll('.group-member')).map(row => ({
            el: row,
            display: row.style.display
        })),
        groupHeaders: Array.from(document.querySelectorAll('.group-header')).map(row => ({
            el: row,
            display: row.style.display
        })),
        groupIcons: Array.from(document.querySelectorAll('.group-icon')).map(icon => ({
            el: icon,
            isClosed: icon.classList.contains('closed')
        })),
        // Detail row and icon states
        detailRows: Array.from(document.querySelectorAll('.detail-row')).map(row => ({
            el: row,
            display: row.style.display
        })),
        expandIcons: Array.from(document.querySelectorAll('.expand-icon')).map(icon => ({
            el: icon,
            isOpen: icon.classList.contains('open')
        }))
    };

    // 2. Expand all test groups so collapsed groups do not exclude results
    document.querySelectorAll('.group-member:not(.detail-row)').forEach(function (row) {
        row.style.display = '';
    });

    document.querySelectorAll('.group-header').forEach(function (row) {
        row.style.display = '';
    });

    document.querySelectorAll('.group-icon').forEach(function (icon) {
        icon.classList.remove('closed');
    });

    // 3. Expand failed-test detail rows before printing so error messages and stack traces are included
    document.querySelectorAll('#testTable tbody tr.group-member[data-status="FAILED"], #failureTable tbody tr.group-member').forEach(function (failedRow) {
        const next = failedRow.nextElementSibling;
        if (next && next.classList.contains('detail-row')) {
            next.style.display = '';
        }
        const icon = failedRow.querySelector('.expand-icon');
        if (icon) {
            icon.classList.add('open');
        }
    });

    // 4. Ensure filters/search do not exclude tests from the printed report
    document.querySelectorAll('.filter-btn').forEach(function (button) {
        button.classList.remove('active');
    });

    const allFilter = document.querySelector('.filter-btn[onclick*="ALL"]');
    if (allFilter) {
        allFilter.classList.add('active');
    }

    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.value = '';
    }

    const activeTagPill = document.getElementById('activeTagPill');
    if (activeTagPill) {
        activeTagPill.style.display = 'none';
    }
});

// Restore original interactive state after printing.
window.addEventListener('afterprint', function () {
    if (!printState) return;

    // 1. Restore tabs
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    const originalTabPane = document.getElementById(printState.activeTabPaneId);
    if (originalTabPane) {
        originalTabPane.classList.add('active');
    }

    const navItems = document.querySelectorAll('.nav-item');
    if (printState.activeNavItemIndex >= 0 && navItems[printState.activeNavItemIndex]) {
        navItems[printState.activeNavItemIndex].classList.add('active');
    }

    // 2. Restore filters and search query
    activeFilter = printState.activeFilter;
    activeTagFilter = printState.activeTagFilter;

    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    const filterBtns = document.querySelectorAll('.filter-btn');
    if (printState.activeFilterBtnIndex >= 0 && filterBtns[printState.activeFilterBtnIndex]) {
        filterBtns[printState.activeFilterBtnIndex].classList.add('active');
    }

    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.value = printState.searchValue;
    }

    const activeTagPill = document.getElementById('activeTagPill');
    if (activeTagPill) {
        activeTagPill.style.display = printState.activeTagPillDisplay;
    }

    // 3. Restore group rows and icons
    printState.groupMembers.forEach(item => {
        item.el.style.display = item.display;
    });

    printState.groupHeaders.forEach(item => {
        item.el.style.display = item.display;
    });

    printState.groupIcons.forEach(item => {
        if (item.isClosed) {
            item.el.classList.add('closed');
        } else {
            item.el.classList.remove('closed');
        }
    });

    // 4. Restore detail rows and icons
    printState.detailRows.forEach(item => {
        item.el.style.display = item.display;
    });

    printState.expandIcons.forEach(item => {
        if (item.isOpen) {
            item.el.classList.add('open');
        } else {
            item.el.classList.remove('open');
        }
    });

    // 5. Re-apply search/filters if there were any active
    if (printState.activeFilter !== 'ALL' || printState.activeTagFilter || printState.searchValue) {
        applySearch();
    }

    printState = null;
});

/* ============================================
   Apple Task Manager - Frontend Application
   ============================================ */

// -----------------------------------------------
// STATE
// -----------------------------------------------
let currentUser = null;
let currentView = 'dashboard';
let allUsers    = [];
let taskFilters = { status: '', sortBy: 'date', order: 'desc' };
let reviewingTaskId = null;
let calendarDate = new Date();

// -----------------------------------------------
// API HELPERS
// -----------------------------------------------
async function api(url, method = 'GET', body = null) {
    const opts = {
        method,
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' }
    };
    if (body) opts.body = JSON.stringify(body);
    try {
        const res = await fetch(url, opts);
        if (res.status === 401) {
            showLoginScreen();
            return null;
        }
        return res;
    } catch (err) {
        console.error('Network error:', err);
        toast('Network error. Please check your connection.', 'error');
        return null;
    }
}

async function apiJson(url, method = 'GET', body = null) {
    const res = await api(url, method, body);
    if (!res) return null;
    try {
        return await res.json();
    } catch {
        return null;
    }
}

// -----------------------------------------------
// TOAST NOTIFICATIONS
// -----------------------------------------------
function toast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.textContent = message;
    container.appendChild(el);
    setTimeout(() => {
        el.style.animation = 'toast-out 0.25s ease forwards';
        setTimeout(() => el.remove(), 260);
    }, 3200);
}

// -----------------------------------------------
// AUTH
// -----------------------------------------------
async function checkAuth() {
    const data = await apiJson('/api/auth/me');
    if (data && data.id) {
        currentUser = data;
        showApp();
    } else {
        showLoginScreen();
    }
}

async function login(username, password) {
    const data = await apiJson('/api/auth/login', 'POST', { username, password });
    if (!data) return;
    if (data.error) {
        showLoginError(data.error);
        return;
    }
    currentUser = data;
    showApp();
}

async function logout() {
    await api('/api/auth/logout', 'POST');
    currentUser = null;
    showLoginScreen();
    toast('Signed out successfully.');
}

// -----------------------------------------------
// SCREEN TRANSITIONS
// -----------------------------------------------
function showLoginScreen() {
    document.getElementById('login-screen').classList.remove('hidden');
    document.getElementById('app').classList.add('hidden');
    document.getElementById('login-username').value = '';
    document.getElementById('login-password').value = '';
    document.getElementById('login-error').classList.add('hidden');
}

function showLoginError(msg) {
    const el = document.getElementById('login-error');
    el.textContent = msg;
    el.classList.remove('hidden');
}

function showApp() {
    document.getElementById('login-screen').classList.add('hidden');
    document.getElementById('app').classList.remove('hidden');
    updateSidebarUser();
    loadUsers().then(() => navigateTo('dashboard'));
}

// -----------------------------------------------
// SIDEBAR USER INFO
// -----------------------------------------------
function updateSidebarUser() {
    if (!currentUser) return;
    const initials = currentUser.name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase();
    document.getElementById('user-avatar-sidebar').textContent = initials;
    document.getElementById('sidebar-username').textContent = currentUser.name;
    document.getElementById('sidebar-role').textContent = currentUser.role;

    const approvalsLink = document.querySelector('.nav-approvals');
    if (currentUser.role === 'MANAGER' || currentUser.role === 'ADMIN') {
        approvalsLink.classList.remove('hidden');
    } else {
        approvalsLink.classList.add('hidden');
    }
}

// -----------------------------------------------
// NAVIGATION
// -----------------------------------------------
function navigateTo(view) {
    currentView = view;
    document.querySelectorAll('.nav-item').forEach(el => {
        el.classList.toggle('active', el.dataset.view === view);
    });

    const titles = {
        dashboard: 'Dashboard',
        tasks:     'All Tasks',
        approvals: 'Pending Approvals',
        calendar:  'Calendar'
    };
    document.getElementById('page-title').textContent = titles[view] || view;

    const area = document.getElementById('content-area');
    area.innerHTML = '<div class="loading-state"><div class="spinner"></div> Loading...</div>';

    switch (view) {
        case 'dashboard':  renderDashboard(); break;
        case 'tasks':      renderTaskList(taskFilters); break;
        case 'approvals':  renderTaskList({ status: 'PENDING', sortBy: 'date', order: 'desc' }); break;
        case 'calendar':   renderCalendar(); break;
    }
}

// -----------------------------------------------
// USERS
// -----------------------------------------------
async function loadUsers() {
    const data = await apiJson('/api/users');
    if (data) allUsers = data;
}

// -----------------------------------------------
// DASHBOARD
// -----------------------------------------------
async function renderDashboard() {
    const [stats, tasks] = await Promise.all([
        apiJson('/api/tasks/stats'),
        apiJson('/api/tasks?sortBy=date&order=desc')
    ]);

    if (!stats || !tasks) return;

    updatePendingBadge(stats.pending);

    const recent = tasks.slice(0, 6);

    const area = document.getElementById('content-area');
    area.innerHTML = `
        <div class="stats-grid">
            <div class="stat-card total">
                <div class="stat-label">Total Tasks</div>
                <div class="stat-value">${stats.total}</div>
            </div>
            <div class="stat-card pending">
                <div class="stat-label"><span class="stat-indicator"></span>Pending</div>
                <div class="stat-value">${stats.pending}</div>
            </div>
            <div class="stat-card approved">
                <div class="stat-label"><span class="stat-indicator"></span>Approved</div>
                <div class="stat-value">${stats.approved}</div>
            </div>
            <div class="stat-card rejected">
                <div class="stat-label"><span class="stat-indicator"></span>Rejected</div>
                <div class="stat-value">${stats.rejected}</div>
            </div>
        </div>
        <div class="section-title">Recent Tasks</div>
        <div class="table-card">
            ${buildTaskTable(recent, true)}
        </div>
    `;

    attachTableEvents();
}

function updatePendingBadge(count) {
    const badge = document.getElementById('pending-badge');
    if (count > 0) {
        badge.textContent = count;
        badge.classList.remove('hidden');
    } else {
        badge.classList.add('hidden');
    }
}

// -----------------------------------------------
// TASK LIST
// -----------------------------------------------
async function renderTaskList(filters = {}) {
    const params = new URLSearchParams();
    if (filters.status)  params.set('status', filters.status);
    if (filters.sortBy)  params.set('sortBy', filters.sortBy);
    if (filters.order)   params.set('order', filters.order);

    const tasks = await apiJson(`/api/tasks?${params}`);
    if (!tasks) return;

    const isApprovals = currentView === 'approvals';

    const area = document.getElementById('content-area');
    area.innerHTML = `
        <div class="table-toolbar">
            ${!isApprovals ? `
            <select class="filter-select" id="filter-status">
                <option value="">All Statuses</option>
                <option value="PENDING"  ${filters.status === 'PENDING'  ? 'selected' : ''}>Pending</option>
                <option value="APPROVED" ${filters.status === 'APPROVED' ? 'selected' : ''}>Approved</option>
                <option value="REJECTED" ${filters.status === 'REJECTED' ? 'selected' : ''}>Rejected</option>
            </select>
            <select class="filter-select" id="filter-sort">
                <option value="date"     ${filters.sortBy === 'date'     ? 'selected' : ''}>Sort: Date</option>
                <option value="priority" ${filters.sortBy === 'priority' ? 'selected' : ''}>Sort: Priority</option>
            </select>
            <select class="filter-select" id="filter-order">
                <option value="desc" ${filters.order === 'desc' ? 'selected' : ''}>Descending</option>
                <option value="asc"  ${filters.order === 'asc'  ? 'selected' : ''}>Ascending</option>
            </select>
            ` : `<span class="text-muted">Showing all pending tasks requiring your review</span>`}
            <span style="margin-left:auto;" class="text-muted">${tasks.length} task${tasks.length !== 1 ? 's' : ''}</span>
        </div>
        <div class="table-card">
            ${buildTaskTable(tasks, false)}
        </div>
    `;

    if (!isApprovals) {
        document.getElementById('filter-status').addEventListener('change', e => {
            taskFilters.status = e.target.value;
            renderTaskList(taskFilters);
        });
        document.getElementById('filter-sort').addEventListener('change', e => {
            taskFilters.sortBy = e.target.value;
            renderTaskList(taskFilters);
        });
        document.getElementById('filter-order').addEventListener('change', e => {
            taskFilters.order = e.target.value;
            renderTaskList(taskFilters);
        });
    }

    attachTableEvents();
}

// -----------------------------------------------
// TASK TABLE BUILDER
// -----------------------------------------------
function buildTaskTable(tasks, compact) {
    if (!tasks || tasks.length === 0) {
        return `<div class="empty-state">
            <div class="empty-state-icon">&#128203;</div>
            <h3>No tasks found</h3>
            <p>No tasks match your current filters.</p>
        </div>`;
    }

    const canReview = currentUser && (currentUser.role === 'MANAGER' || currentUser.role === 'ADMIN');

    const rows = tasks.map(t => {
        const statusBadge = `<span class="status-badge status-${t.status}">${statusIcon(t.status)} ${t.status}</span>`;
        const priorityBadge = `<span class="priority-badge priority-${t.priority}">${t.priority}</span>`;
        const actionCell = (canReview && t.status === 'PENDING')
            ? `<div class="action-btns">
                <button class="btn-approve" data-id="${t.id}">Approve</button>
                <button class="btn-reject" data-id="${t.id}">Reject</button>
               </div>`
            : `<span class="text-muted text-sm">${t.status !== 'PENDING' ? (t.reviewedByName || '—') : '—'}</span>`;

        return `<tr>
            <td>
                <div class="task-title-cell">${escHtml(t.title)}</div>
                ${!compact && t.description ? `<div class="task-desc">${escHtml(t.description)}</div>` : ''}
            </td>
            <td>${priorityBadge}</td>
            <td>${statusBadge}</td>
            <td class="text-muted text-sm">${t.assignedUserName ? escHtml(t.assignedUserName) : '—'}</td>
            ${!compact ? `<td class="text-muted text-sm">${t.createdByName ? escHtml(t.createdByName) : '—'}</td>` : ''}
            <td class="text-muted text-sm">${formatDate(t.dateTime)}</td>
            <td>${actionCell}</td>
        </tr>`;
    }).join('');

    return `<table class="data-table">
        <thead>
            <tr>
                <th>Task</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Assigned To</th>
                ${!compact ? '<th>Created By</th>' : ''}
                <th>Due Date</th>
                <th>Action</th>
            </tr>
        </thead>
        <tbody>${rows}</tbody>
    </table>`;
}

function attachTableEvents() {
    document.querySelectorAll('.btn-approve').forEach(btn => {
        btn.addEventListener('click', () => openReviewModal(Number(btn.dataset.id), 'APPROVED'));
    });
    document.querySelectorAll('.btn-reject').forEach(btn => {
        btn.addEventListener('click', () => openReviewModal(Number(btn.dataset.id), 'REJECTED'));
    });
}

// -----------------------------------------------
// REVIEW MODAL
// -----------------------------------------------
function openReviewModal(taskId, defaultAction) {
    reviewingTaskId = taskId;
    document.getElementById('review-modal-title').textContent =
        defaultAction === 'APPROVED' ? 'Approve Task' : 'Reject Task';
    document.getElementById('review-comments').value = '';
    document.getElementById('review-error').classList.add('hidden');
    document.getElementById('review-modal').classList.remove('hidden');
}

function closeReviewModal() {
    document.getElementById('review-modal').classList.add('hidden');
    reviewingTaskId = null;
}

async function submitReview(status) {
    const comments = document.getElementById('review-comments').value.trim();
    const errEl = document.getElementById('review-error');
    errEl.classList.add('hidden');

    const data = await apiJson(`/api/tasks/${reviewingTaskId}/approve`, 'PUT', { status, comments });
    if (!data) return;

    if (data.error) {
        errEl.textContent = data.error;
        errEl.classList.remove('hidden');
        return;
    }

    closeReviewModal();
    toast(`Task ${status === 'APPROVED' ? 'approved' : 'rejected'} successfully.`,
          status === 'APPROVED' ? 'success' : 'warning');
    navigateTo(currentView);
}

// -----------------------------------------------
// CREATE TASK MODAL
// -----------------------------------------------
function openCreateTaskModal() {
    document.getElementById('task-form').reset();
    document.getElementById('form-error').classList.add('hidden');

    // Set default datetime to now + 1 day
    const dt = new Date();
    dt.setDate(dt.getDate() + 1);
    dt.setSeconds(0, 0);
    document.getElementById('task-datetime').value = dt.toISOString().slice(0, 16);

    // Populate users
    const select = document.getElementById('task-assignee');
    select.innerHTML = '<option value="">Unassigned</option>';
    allUsers.forEach(u => {
        const opt = document.createElement('option');
        opt.value = u.id;
        opt.textContent = `${u.name} (${u.role})`;
        select.appendChild(opt);
    });

    document.getElementById('task-modal').classList.remove('hidden');
    document.getElementById('task-title').focus();
}

function closeCreateTaskModal() {
    document.getElementById('task-modal').classList.add('hidden');
}

async function submitCreateTask(e) {
    e.preventDefault();
    const errEl = document.getElementById('form-error');
    errEl.classList.add('hidden');

    const title       = document.getElementById('task-title').value.trim();
    const description = document.getElementById('task-description').value.trim();
    const datetimeVal = document.getElementById('task-datetime').value;
    const priority    = document.getElementById('task-priority').value;
    const assigneeId  = document.getElementById('task-assignee').value;

    if (!title || !datetimeVal || !priority) {
        errEl.textContent = 'Please fill in all required fields.';
        errEl.classList.remove('hidden');
        return;
    }

    const payload = {
        title,
        description: description || null,
        dateTime: datetimeVal + ':00', // LocalDateTime format
        priority,
        assignedUserId: assigneeId ? Number(assigneeId) : null
    };

    const data = await apiJson('/api/tasks', 'POST', payload);
    if (!data) return;

    if (data.error) {
        errEl.textContent = data.error;
        errEl.classList.remove('hidden');
        return;
    }

    closeCreateTaskModal();
    toast('Task created successfully!', 'success');
    navigateTo(currentView === 'dashboard' ? 'dashboard' : currentView);
}

// -----------------------------------------------
// CALENDAR VIEW
// -----------------------------------------------
async function renderCalendar() {
    const tasks = await apiJson('/api/tasks');
    if (!tasks) return;

    const year  = calendarDate.getFullYear();
    const month = calendarDate.getMonth();

    const monthName = calendarDate.toLocaleString('default', { month: 'long', year: 'numeric' });

    // Group tasks by date string (YYYY-MM-DD)
    const tasksByDate = {};
    tasks.forEach(t => {
        if (!t.dateTime) return;
        const key = t.dateTime.slice(0, 10);
        if (!tasksByDate[key]) tasksByDate[key] = [];
        tasksByDate[key].push(t);
    });

    // Build calendar
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const today = new Date();

    const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const dayHeaders = dayNames.map(d => `<div class="calendar-day-name">${d}</div>`).join('');

    let cells = '';
    let dayCount = 1;

    // Empty cells before first day
    for (let i = 0; i < firstDay; i++) {
        const prevDate = new Date(year, month, -firstDay + i + 1);
        cells += `<div class="calendar-cell other-month">
            <div class="day-num">${prevDate.getDate()}</div>
        </div>`;
    }

    // Days in month
    for (let d = 1; d <= daysInMonth; d++) {
        const isToday = today.getFullYear() === year && today.getMonth() === month && today.getDate() === d;
        const dateKey = `${year}-${String(month + 1).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
        const dayTasks = tasksByDate[dateKey] || [];

        const taskDots = dayTasks.slice(0, 3).map(t =>
            `<div class="calendar-task-dot ${t.status}" title="${escHtml(t.title)}">${escHtml(t.title)}</div>`
        ).join('');

        const more = dayTasks.length > 3
            ? `<div class="text-muted text-sm">+${dayTasks.length - 3} more</div>`
            : '';

        cells += `<div class="calendar-cell${isToday ? ' today' : ''}">
            <div class="day-num">${d}</div>
            ${taskDots}
            ${more}
        </div>`;
    }

    // Fill remaining cells
    const totalCells = firstDay + daysInMonth;
    const remainder = totalCells % 7 === 0 ? 0 : 7 - (totalCells % 7);
    for (let i = 1; i <= remainder; i++) {
        cells += `<div class="calendar-cell other-month">
            <div class="day-num">${i}</div>
        </div>`;
    }

    const area = document.getElementById('content-area');
    area.innerHTML = `
        <div class="calendar-header">
            <div class="calendar-nav">
                <button class="btn btn-ghost btn-sm" id="cal-prev">&larr; Prev</button>
                <div class="calendar-month-title">${monthName}</div>
                <button class="btn btn-ghost btn-sm" id="cal-next">Next &rarr;</button>
            </div>
            <div style="display:flex;gap:12px;align-items:center;">
                <span><span class="status-badge status-PENDING">Pending</span></span>
                <span><span class="status-badge status-APPROVED">Approved</span></span>
                <span><span class="status-badge status-REJECTED">Rejected</span></span>
            </div>
        </div>
        <div class="calendar-grid">
            <div class="calendar-days-header">${dayHeaders}</div>
            <div class="calendar-body">${cells}</div>
        </div>
        <div class="text-muted text-sm" style="margin-top:10px;">
            Showing ${tasks.length} total tasks across all months.
        </div>
    `;

    document.getElementById('cal-prev').addEventListener('click', () => {
        calendarDate.setMonth(calendarDate.getMonth() - 1);
        renderCalendar();
    });
    document.getElementById('cal-next').addEventListener('click', () => {
        calendarDate.setMonth(calendarDate.getMonth() + 1);
        renderCalendar();
    });
}

// -----------------------------------------------
// CSV EXPORT
// -----------------------------------------------
async function exportCsv() {
    const statusFilter = currentView === 'approvals' ? 'PENDING' : (taskFilters.status || '');
    const params = statusFilter ? `?status=${statusFilter}` : '';

    const res = await api(`/api/tasks/export/csv${params}`);
    if (!res) return;

    const blob = await res.blob();
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = 'tasks-export.csv';
    a.click();
    URL.revokeObjectURL(url);
    toast('CSV exported successfully.', 'success');
}

// -----------------------------------------------
// HELPERS
// -----------------------------------------------
function escHtml(str) {
    if (!str) return '';
    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function formatDate(isoStr) {
    if (!isoStr) return '—';
    const d = new Date(isoStr);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
         + ' ' + d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
}

function statusIcon(status) {
    return { PENDING: '&#9679;', APPROVED: '&#10003;', REJECTED: '&#10007;' }[status] || '';
}

// -----------------------------------------------
// EVENT LISTENERS (init)
// -----------------------------------------------
document.addEventListener('DOMContentLoaded', () => {

    // Login form
    document.getElementById('login-form').addEventListener('submit', async e => {
        e.preventDefault();
        const username = document.getElementById('login-username').value.trim();
        const password = document.getElementById('login-password').value;
        if (!username || !password) return;
        await login(username, password);
    });

    // Logout
    document.getElementById('logout-btn').addEventListener('click', logout);

    // Sidebar navigation
    document.querySelectorAll('.nav-item').forEach(link => {
        link.addEventListener('click', e => {
            e.preventDefault();
            const view = link.dataset.view;
            if (view) navigateTo(view);
        });
    });

    // Create task button
    document.getElementById('create-task-btn').addEventListener('click', openCreateTaskModal);

    // Export CSV button
    document.getElementById('export-btn').addEventListener('click', exportCsv);

    // Create task modal
    document.getElementById('modal-close').addEventListener('click', closeCreateTaskModal);
    document.getElementById('form-cancel').addEventListener('click', closeCreateTaskModal);
    document.getElementById('task-form').addEventListener('submit', submitCreateTask);

    // Close modal on overlay click
    document.getElementById('task-modal').addEventListener('click', e => {
        if (e.target === document.getElementById('task-modal')) closeCreateTaskModal();
    });

    // Review modal
    document.getElementById('review-modal-close').addEventListener('click', closeReviewModal);
    document.getElementById('review-cancel').addEventListener('click', closeReviewModal);
    document.getElementById('review-modal').addEventListener('click', e => {
        if (e.target === document.getElementById('review-modal')) closeReviewModal();
    });

    document.getElementById('review-approve-btn').addEventListener('click', () => submitReview('APPROVED'));
    document.getElementById('review-reject-btn').addEventListener('click',  () => submitReview('REJECTED'));

    // Keyboard shortcuts
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape') {
            closeCreateTaskModal();
            closeReviewModal();
        }
    });

    // Bootstrap the app
    checkAuth();
});

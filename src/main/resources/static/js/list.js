let currentStatus = 'ALL';

function setFilter(status, btn) {
    currentStatus = status;
    document.querySelectorAll('.filter-pill').forEach(b => {
        b.classList.remove('active', 'active-green', 'active-red');
    });
    if (status === 'CONFIRMED') btn.classList.add('active-green');
    else if (status === 'CANCELLED') btn.classList.add('active-red');
    else btn.classList.add('active');
    filterSearch();
}

function filterSearch() {
    const query = (document.getElementById('tableSearchInput')?.value || '').toLowerCase().trim();
    const rows = document.querySelectorAll('.booking-row');
    rows.forEach(row => {
        const rowStatus = row.getAttribute('data-status');
        const matchesStatus = (currentStatus === 'ALL' || rowStatus === currentStatus);
        const matchesQuery = query === '' || row.innerText.toLowerCase().includes(query);
        row.style.display = (matchesStatus && matchesQuery) ? '' : 'none';
    });
}

function togglePolicyDrawer(forceState) {
    const layout = document.getElementById('workspaceLayout');
    if (!layout) return;
    const isCurrentlyOpen = layout.classList.contains('policy-open');
    const newState = (forceState !== undefined) ? forceState : !isCurrentlyOpen;

    if (newState) {
        layout.classList.add('policy-open');
        localStorage.setItem('roompulse_policy_open', 'true');
    } else {
        layout.classList.remove('policy-open');
        localStorage.setItem('roompulse_policy_open', 'false');
    }
    updateTriggerState(newState);
}

function updateTriggerState(isOpen) {
    const toolbarBtn = document.getElementById('toolbarPolicyBtn');
    if (toolbarBtn) {
        if (isOpen) {
            toolbarBtn.classList.add('active');
            toolbarBtn.innerHTML = '<i class="bi bi-layout-sidebar-inset-reverse"></i><span>Ẩn quy định</span>';
            toolbarBtn.setAttribute('title', 'Ẩn bảng quy định để mở rộng toàn màn hình');
        } else {
            toolbarBtn.classList.remove('active');
            toolbarBtn.innerHTML = '<i class="bi bi-shield-check"></i><span>Quy định</span>';
            toolbarBtn.setAttribute('title', 'Xem quy định đặt phòng song song');
        }
    }
}

document.addEventListener('DOMContentLoaded', function() {
    localStorage.removeItem('roompulse_theme');
    document.documentElement.removeAttribute('data-theme');
    const savedState = localStorage.getItem('roompulse_policy_open');
    if (savedState === 'true') {
        togglePolicyDrawer(true);
    } else {
        updateTriggerState(false);
    }
});

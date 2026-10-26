// =========================================================================
// MediSync - Health & Medicine Management Frontend Application
// =========================================================================

const API_BASE = ''; // Same origin (http://localhost:5000)

let currentTab = 'dashboard';
let cachedUsers = [];
let cachedDoctors = [];
let cachedMedicines = [];
let cachedPrescriptions = [];

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
    setupTabNavigation();
    checkDatabaseStatus();
    loadDashboard();
    // Pre-cache lists for dropdowns
    loadUsers();
    loadDoctors();
    loadMedicines();
});

// -------------------------------------------------------------
// TAB NAVIGATION
// -------------------------------------------------------------
function setupTabNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabId = btn.getAttribute('data-tab');
            switchTab(tabId);
        });
    });
}

function switchTab(tabId) {
    currentTab = tabId;

    // Update nav active class
    document.querySelectorAll('.nav-item').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
    });

    // Update tab panes
    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.classList.remove('active');
    });
    const targetPane = document.getElementById(`tab-${tabId}`);
    if (targetPane) targetPane.classList.add('active');

    // Update Title & Subtitle
    const pageTitle = document.getElementById('pageTitle');
    const pageSubtitle = document.getElementById('pageSubtitle');

    const titles = {
        dashboard: { title: 'Dashboard Overview', sub: 'Live health records and inventory statistics' },
        users: { title: 'Patient Directory', sub: 'Registered patients, contact information, and medical profiles' },
        doctors: { title: 'Medical Staff & Doctors', sub: 'Registered physicians, specializations, and load' },
        medicines: { title: 'Medicine Inventory', sub: 'Real-time pharmaceutical stocks, expiry tracking, and pricing' },
        prescriptions: { title: 'Prescription Records', sub: 'Prescriptions issued by doctors to patients' },
        reminders: { title: 'Dose Reminders & Schedules', sub: 'Patient medication adherence and dose reminders' },
        analytics: { title: 'SQL Analytics & Reports', sub: 'Advanced database aggregations and SQL query outputs' }
    };

    if (titles[tabId]) {
        pageTitle.textContent = titles[tabId].title;
        pageSubtitle.textContent = titles[tabId].sub;
    }

    refreshCurrentTab();
}

function refreshCurrentTab() {
    if (currentTab === 'dashboard') loadDashboard();
    else if (currentTab === 'users') loadUsers();
    else if (currentTab === 'doctors') loadDoctors();
    else if (currentTab === 'medicines') loadMedicines();
    else if (currentTab === 'prescriptions') loadPrescriptions();
    else if (currentTab === 'reminders') loadReminders();
    else if (currentTab === 'analytics') loadAnalytics();
}

// -------------------------------------------------------------
// SYSTEM STATUS CHECK
// -------------------------------------------------------------
async function checkDatabaseStatus() {
    const pulse = document.getElementById('dbPulse');
    const label = document.getElementById('dbStatusText');
    try {
        const res = await fetch(`${API_BASE}/api/status`);
        const data = await res.json();
        if (data.mysqlConnected) {
            pulse.style.background = '#10b981';
            pulse.style.boxShadow = '0 0 8px #10b981';
            label.textContent = 'MySQL Connected';
            label.style.color = '#10b981';
        } else {
            throw new Error(data.error);
        }
    } catch (err) {
        pulse.style.background = '#ef4444';
        pulse.style.boxShadow = '0 0 8px #ef4444';
        label.textContent = 'Connection Error';
        label.style.color = '#ef4444';
    }
}

// -------------------------------------------------------------
// 1. DASHBOARD OVERVIEW
// -------------------------------------------------------------
async function loadDashboard() {
    try {
        const res = await fetch(`${API_BASE}/api/stats`);
        const stats = await res.json();

        document.getElementById('statUsers').textContent = stats.users || 0;
        document.getElementById('statDoctors').textContent = stats.doctors || 0;
        document.getElementById('statMedicines').textContent = stats.medicines || 0;
        document.getElementById('statActivePrescriptions').textContent = stats.activePrescriptions || 0;
        document.getElementById('statLowStock').textContent = stats.lowStock || 0;
        document.getElementById('statPendingReminders').textContent = stats.pendingReminders || 0;

        // Load recent active prescriptions in dashboard
        loadDashboardPrescriptions();
        loadDashboardReminders();
    } catch (err) {
        console.error('Error loading dashboard stats:', err);
    }
}

async function loadDashboardPrescriptions() {
    const tbody = document.querySelector('#dashboardPrescriptionsTable tbody');
    try {
        const res = await fetch(`${API_BASE}/api/prescriptions?status=Active`);
        const data = await res.json();

        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="loading-cell">No active prescriptions currently.</td></tr>';
            return;
        }

        tbody.innerHTML = data.slice(0, 5).map(p => `
            <tr>
                <td><strong>${escapeHtml(p.user_name)}</strong></td>
                <td>${escapeHtml(p.doctor_name)} <br><small style="color:var(--text-muted)">${escapeHtml(p.doctor_specialization || '')}</small></td>
                <td>${escapeHtml(p.medicine_name)} <br><small style="color:var(--text-muted)">${escapeHtml(p.medicine_brand || '')}</small></td>
                <td>${escapeHtml(p.dosage)} (${p.duration_days} days)</td>
                <td><span class="badge badge-success">${p.status}</span></td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="5" class="loading-cell">Failed to load prescriptions</td></tr>';
    }
}

async function loadDashboardReminders() {
    const container = document.getElementById('dashboardRemindersList');
    try {
        const res = await fetch(`${API_BASE}/api/reminders?status=Pending`);
        const data = await res.json();

        if (data.length === 0) {
            container.innerHTML = '<div class="loading-state">No pending reminders! All caught up.</div>';
            return;
        }

        container.innerHTML = data.slice(0, 4).map(r => `
            <div class="reminder-item">
                <div class="reminder-info">
                    <h4>${escapeHtml(r.user_name)}</h4>
                    <p>${escapeHtml(r.message)}</p>
                    <small><i class="fa-regular fa-clock"></i> ${formatDateTime(r.reminder_datetime)}</small>
                </div>
                <div>
                    <button class="btn btn-sm btn-outline" onclick="updateReminderStatus(${r.reminder_id}, 'Completed')">
                        <i class="fa-solid fa-check"></i> Done
                    </button>
                </div>
            </div>
        `).join('');
    } catch (err) {
        container.innerHTML = '<div class="loading-state">Failed to load reminders</div>';
    }
}

// -------------------------------------------------------------
// 2. PATIENTS (USERS) MANAGEMENT
// -------------------------------------------------------------
async function loadUsers(search = '') {
    const tbody = document.querySelector('#usersTable tbody');
    try {
        let url = `${API_BASE}/api/users`;
        if (search) url += `?q=${encodeURIComponent(search)}`;
        const res = await fetch(url);
        cachedUsers = await res.json();

        if (cachedUsers.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="loading-cell">No patients found.</td></tr>';
            return;
        }

        tbody.innerHTML = cachedUsers.map(u => `
            <tr>
                <td>#${u.user_id}</td>
                <td><strong>${escapeHtml(u.name)}</strong></td>
                <td>${escapeHtml(u.email || '-')}</td>
                <td>${escapeHtml(u.phone)}</td>
                <td>${u.dob || '-'}</td>
                <td>${escapeHtml(u.address || '-')}</td>
                <td>
                    <div style="display:flex; gap:6px;">
                        <button class="btn-icon-sm" title="Edit Patient" onclick="openEditUserModal(${u.user_id})">
                            <i class="fa-solid fa-pen"></i>
                        </button>
                        <button class="btn-icon-sm" title="Delete Patient" style="color:var(--danger-color)" onclick="deleteUser(${u.user_id})">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="7" class="loading-cell">Error loading patients</td></tr>';
    }
}

let userSearchTimeout;
function handleUsersSearch() {
    clearTimeout(userSearchTimeout);
    userSearchTimeout = setTimeout(() => {
        const query = document.getElementById('usersSearchInput').value.trim();
        loadUsers(query);
    }, 300);
}

function openAddUserModal() {
    const html = `
        <form onsubmit="handleUserSubmit(event)">
            <div class="form-group">
                <label>Full Name *</label>
                <input type="text" class="form-control" name="name" required placeholder="e.g. Aarav Shah">
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Phone Number *</label>
                    <input type="text" class="form-control" name="phone" required placeholder="e.g. 9876543210">
                </div>
                <div class="form-group">
                    <label>Email Address</label>
                    <input type="email" class="form-control" name="email" placeholder="e.g. aarav@gmail.com">
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Date of Birth</label>
                    <input type="date" class="form-control" name="dob">
                </div>
                <div class="form-group">
                    <label>City / Address</label>
                    <input type="text" class="form-control" name="address" placeholder="e.g. Mumbai, Maharashtra">
                </div>
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Patient</button>
            </div>
        </form>
    `;
    openModal('Register New Patient', html);
}

function openEditUserModal(id) {
    const u = cachedUsers.find(user => user.user_id === id);
    if (!u) return;

    const html = `
        <form onsubmit="handleUserEditSubmit(event, ${id})">
            <div class="form-group">
                <label>Full Name *</label>
                <input type="text" class="form-control" name="name" value="${escapeHtml(u.name)}" required>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Phone Number *</label>
                    <input type="text" class="form-control" name="phone" value="${escapeHtml(u.phone)}" required>
                </div>
                <div class="form-group">
                    <label>Email Address</label>
                    <input type="email" class="form-control" name="email" value="${escapeHtml(u.email || '')}">
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Date of Birth</label>
                    <input type="date" class="form-control" name="dob" value="${u.dob || ''}">
                </div>
                <div class="form-group">
                    <label>City / Address</label>
                    <input type="text" class="form-control" name="address" value="${escapeHtml(u.address || '')}">
                </div>
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Update Patient</button>
            </div>
        </form>
    `;
    openModal('Edit Patient Details', html);
}

async function handleUserSubmit(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const body = Object.fromEntries(formData.entries());

    try {
        const res = await fetch(`${API_BASE}/api/users`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Patient registered successfully!');
        closeModal();
        loadUsers();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleUserEditSubmit(e, id) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const body = Object.fromEntries(formData.entries());

    try {
        const res = await fetch(`${API_BASE}/api/users/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Patient updated successfully!');
        closeModal();
        loadUsers();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteUser(id) {
    if (!confirm('Are you sure you want to delete this patient record? Associated prescriptions will also be removed.')) return;
    try {
        const res = await fetch(`${API_BASE}/api/users/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Patient removed.');
        loadUsers();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// -------------------------------------------------------------
// 3. DOCTORS MANAGEMENT
// -------------------------------------------------------------
async function loadDoctors(search = '') {
    const grid = document.getElementById('doctorsGrid');
    try {
        let url = `${API_BASE}/api/doctors`;
        if (search) url += `?q=${encodeURIComponent(search)}`;
        const res = await fetch(url);
        cachedDoctors = await res.json();

        if (cachedDoctors.length === 0) {
            grid.innerHTML = '<div class="loading-state">No doctors found.</div>';
            return;
        }

        grid.innerHTML = cachedDoctors.map(d => `
            <div class="doctor-card">
                <div class="doctor-avatar">
                    <i class="fa-solid fa-user-doctor"></i>
                </div>
                <h3 class="doctor-name">${escapeHtml(d.name)}</h3>
                <span class="doctor-spec">${escapeHtml(d.specialization)}</span>
                <span class="doctor-phone"><i class="fa-solid fa-phone"></i> ${escapeHtml(d.phone || 'No phone')}</span>
                <div class="doctor-actions">
                    <button class="btn btn-sm btn-edit" onclick="openEditDoctorModal(${d.doctor_id})">
                        <i class="fa-solid fa-pen"></i> Edit
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteDoctor(${d.doctor_id})">
                        <i class="fa-solid fa-trash"></i> Delete
                    </button>
                </div>
            </div>
        `).join('');
    } catch (err) {
        grid.innerHTML = '<div class="loading-state">Error loading doctors</div>';
    }
}

let doctorSearchTimeout;
function handleDoctorsSearch() {
    clearTimeout(doctorSearchTimeout);
    doctorSearchTimeout = setTimeout(() => {
        const query = document.getElementById('doctorsSearchInput').value.trim();
        loadDoctors(query);
    }, 300);
}

function openAddDoctorModal() {
    const html = `
        <form onsubmit="handleDoctorSubmit(event)">
            <div class="form-group">
                <label>Doctor Name *</label>
                <input type="text" class="form-control" name="name" required placeholder="e.g. Dr. Priya Joshi">
            </div>
            <div class="form-group">
                <label>Specialization *</label>
                <input type="text" class="form-control" name="specialization" required placeholder="e.g. Cardiologist / General Physician">
            </div>
            <div class="form-group">
                <label>Phone Number</label>
                <input type="text" class="form-control" name="phone" placeholder="e.g. 9876501235">
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Add Doctor</button>
            </div>
        </form>
    `;
    openModal('Add New Doctor', html);
}

function openEditDoctorModal(id) {
    const d = cachedDoctors.find(doc => doc.doctor_id === id);
    if (!d) return;

    const html = `
        <form onsubmit="handleDoctorEditSubmit(event, ${id})">
            <div class="form-group">
                <label>Doctor Name *</label>
                <input type="text" class="form-control" name="name" value="${escapeHtml(d.name)}" required>
            </div>
            <div class="form-group">
                <label>Specialization *</label>
                <input type="text" class="form-control" name="specialization" value="${escapeHtml(d.specialization)}" required>
            </div>
            <div class="form-group">
                <label>Phone Number</label>
                <input type="text" class="form-control" name="phone" value="${escapeHtml(d.phone || '')}">
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Update Doctor</button>
            </div>
        </form>
    `;
    openModal('Edit Doctor Details', html);
}

async function handleDoctorSubmit(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const body = Object.fromEntries(formData.entries());

    try {
        const res = await fetch(`${API_BASE}/api/doctors`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Doctor profile added!');
        closeModal();
        loadDoctors();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleDoctorEditSubmit(e, id) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const body = Object.fromEntries(formData.entries());

    try {
        const res = await fetch(`${API_BASE}/api/doctors/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Doctor profile updated!');
        closeModal();
        loadDoctors();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteDoctor(id) {
    if (!confirm('Are you sure you want to remove this doctor?')) return;
    try {
        const res = await fetch(`${API_BASE}/api/doctors/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Doctor removed.');
        loadDoctors();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// -------------------------------------------------------------
// 4. MEDICINES (INVENTORY) MANAGEMENT
// -------------------------------------------------------------
let currentMedicineFilter = 'all';

async function loadMedicines(search = '') {
    const tbody = document.querySelector('#medicinesTable tbody');
    try {
        let url = `${API_BASE}/api/medicines?`;
        if (currentMedicineFilter === 'lowStock') url += 'lowStock=true&';
        if (search) url += `q=${encodeURIComponent(search)}`;

        const res = await fetch(url);
        cachedMedicines = await res.json();

        if (cachedMedicines.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="loading-cell">No medicines found.</td></tr>';
            return;
        }

        tbody.innerHTML = cachedMedicines.map(m => {
            const isLowStock = m.stock_quantity < 60;
            const stockBadge = isLowStock
                ? `<span class="badge badge-warning"><i class="fa-solid fa-triangle-exclamation"></i> Low (${m.stock_quantity})</span>`
                : `<span class="badge badge-success">${m.stock_quantity} in stock</span>`;

            return `
                <tr>
                    <td>#${m.medicine_id}</td>
                    <td><strong>${escapeHtml(m.name)}</strong></td>
                    <td>${escapeHtml(m.brand || '-')}</td>
                    <td>${m.expiry_date}</td>
                    <td>${stockBadge}</td>
                    <td><strong>₹${parseFloat(m.unit_price).toFixed(2)}</strong></td>
                    <td>
                        <div class="quick-adjust">
                            <button class="btn-icon-sm" title="Add 10 units" onclick="adjustMedicineStock(${m.medicine_id}, 10)">+10</button>
                            <button class="btn-icon-sm" title="Deduct 5 units" onclick="adjustMedicineStock(${m.medicine_id}, -5)">-5</button>
                        </div>
                    </td>
                    <td>
                        <div style="display:flex; gap:6px;">
                            <button class="btn-icon-sm" title="Edit Medicine" onclick="openEditMedicineModal(${m.medicine_id})">
                                <i class="fa-solid fa-pen"></i>
                            </button>
                            <button class="btn-icon-sm" title="Delete Medicine" style="color:var(--danger-color)" onclick="deleteMedicine(${m.medicine_id})">
                                <i class="fa-solid fa-trash"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="8" class="loading-cell">Error loading medicines</td></tr>';
    }
}

function filterMedicines(type, el) {
    currentMedicineFilter = type;
    document.querySelectorAll('#tab-medicines .filter-pill').forEach(btn => btn.classList.remove('active'));
    el.classList.add('active');
    loadMedicines();
}

let medicineSearchTimeout;
function handleMedicinesSearch() {
    clearTimeout(medicineSearchTimeout);
    medicineSearchTimeout = setTimeout(() => {
        const query = document.getElementById('medicinesSearchInput').value.trim();
        loadMedicines(query);
    }, 300);
}

async function adjustMedicineStock(id, delta) {
    try {
        const res = await fetch(`${API_BASE}/api/medicines/${id}/stock`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ adjustment: delta })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast(`Stock updated (${delta > 0 ? '+' : ''}${delta})`);
        loadMedicines();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function openAddMedicineModal() {
    const html = `
        <form onsubmit="handleMedicineSubmit(event)">
            <div class="form-row">
                <div class="form-group">
                    <label>Medicine Name *</label>
                    <input type="text" class="form-control" name="name" required placeholder="e.g. Paracetamol">
                </div>
                <div class="form-group">
                    <label>Brand / Manufacturer</label>
                    <input type="text" class="form-control" name="brand" placeholder="e.g. Cipla">
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Expiry Date *</label>
                    <input type="date" class="form-control" name="expiry_date" required>
                </div>
                <div class="form-group">
                    <label>Initial Stock Quantity</label>
                    <input type="number" class="form-control" name="stock_quantity" value="100" min="0">
                </div>
            </div>
            <div class="form-group">
                <label>Unit Price (₹) *</label>
                <input type="number" step="0.01" class="form-control" name="unit_price" required placeholder="e.g. 15.50" min="0">
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Add Medicine</button>
            </div>
        </form>
    `;
    openModal('Add Medicine to Inventory', html);
}

function openEditMedicineModal(id) {
    const m = cachedMedicines.find(med => med.medicine_id === id);
    if (!m) return;

    const html = `
        <form onsubmit="handleMedicineEditSubmit(event, ${id})">
            <div class="form-row">
                <div class="form-group">
                    <label>Medicine Name *</label>
                    <input type="text" class="form-control" name="name" value="${escapeHtml(m.name)}" required>
                </div>
                <div class="form-group">
                    <label>Brand / Manufacturer</label>
                    <input type="text" class="form-control" name="brand" value="${escapeHtml(m.brand || '')}">
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Expiry Date *</label>
                    <input type="date" class="form-control" name="expiry_date" value="${m.expiry_date}" required>
                </div>
                <div class="form-group">
                    <label>Stock Quantity</label>
                    <input type="number" class="form-control" name="stock_quantity" value="${m.stock_quantity}" min="0">
                </div>
            </div>
            <div class="form-group">
                <label>Unit Price (₹) *</label>
                <input type="number" step="0.01" class="form-control" name="unit_price" value="${m.unit_price}" required min="0">
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Update Medicine</button>
            </div>
        </form>
    `;
    openModal('Edit Medicine Details', html);
}

async function handleMedicineSubmit(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const body = Object.fromEntries(formData.entries());

    try {
        const res = await fetch(`${API_BASE}/api/medicines`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Medicine added to inventory!');
        closeModal();
        loadMedicines();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleMedicineEditSubmit(e, id) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const body = Object.fromEntries(formData.entries());

    try {
        const res = await fetch(`${API_BASE}/api/medicines/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Medicine details updated!');
        closeModal();
        loadMedicines();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteMedicine(id) {
    if (!confirm('Are you sure you want to remove this medicine?')) return;
    try {
        const res = await fetch(`${API_BASE}/api/medicines/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Medicine deleted.');
        loadMedicines();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// -------------------------------------------------------------
// 5. PRESCRIPTIONS MANAGEMENT
// -------------------------------------------------------------
let currentPrescriptionFilter = '';

async function loadPrescriptions() {
    const tbody = document.querySelector('#prescriptionsTable tbody');
    try {
        let url = `${API_BASE}/api/prescriptions`;
        if (currentPrescriptionFilter) url += `?status=${encodeURIComponent(currentPrescriptionFilter)}`;
        const res = await fetch(url);
        cachedPrescriptions = await res.json();

        if (cachedPrescriptions.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="loading-cell">No prescriptions recorded.</td></tr>';
            return;
        }

        tbody.innerHTML = cachedPrescriptions.map(p => {
            let statusBadge = `<span class="badge badge-success">${p.status}</span>`;
            if (p.status === 'Completed') statusBadge = `<span class="badge badge-info">${p.status}</span>`;
            if (p.status === 'Cancelled') statusBadge = `<span class="badge badge-danger">${p.status}</span>`;

            return `
                <tr>
                    <td>#${p.prescription_id}</td>
                    <td><strong>${escapeHtml(p.user_name)}</strong> <br><small style="color:var(--text-muted)">${p.user_phone}</small></td>
                    <td>${escapeHtml(p.doctor_name)} <br><small style="color:var(--accent-cyan)">${escapeHtml(p.doctor_specialization || '')}</small></td>
                    <td><strong>${escapeHtml(p.medicine_name)}</strong> <br><small style="color:var(--text-muted)">${escapeHtml(p.medicine_brand || '')}</small></td>
                    <td>${p.date_issued}</td>
                    <td>${escapeHtml(p.dosage)}</td>
                    <td>${p.duration_days} days</td>
                    <td>${statusBadge}</td>
                    <td>
                        <div style="display:flex; gap:6px; align-items:center;">
                            <select class="form-control" style="padding:4px 8px; font-size:0.75rem; width:auto;" onchange="updatePrescriptionStatus(${p.prescription_id}, this.value)">
                                <option value="Active" ${p.status === 'Active' ? 'selected' : ''}>Active</option>
                                <option value="Completed" ${p.status === 'Completed' ? 'selected' : ''}>Completed</option>
                                <option value="Cancelled" ${p.status === 'Cancelled' ? 'selected' : ''}>Cancelled</option>
                            </select>
                            <button class="btn-icon-sm" title="Delete Prescription" style="color:var(--danger-color)" onclick="deletePrescription(${p.prescription_id})">
                                <i class="fa-solid fa-trash"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="9" class="loading-cell">Error loading prescriptions</td></tr>';
    }
}

function filterPrescriptions(status, el) {
    currentPrescriptionFilter = status;
    document.querySelectorAll('#tab-prescriptions .filter-pill').forEach(btn => btn.classList.remove('active'));
    el.classList.add('active');
    loadPrescriptions();
}

async function updatePrescriptionStatus(id, newStatus) {
    try {
        const res = await fetch(`${API_BASE}/api/prescriptions/${id}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast(`Prescription status changed to ${newStatus}`);
        loadPrescriptions();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function openAddPrescriptionModal() {
    // Ensure we have users, doctors, and medicines
    if (cachedUsers.length === 0) {
        const res = await fetch(`${API_BASE}/api/users`);
        cachedUsers = await res.json();
    }
    if (cachedDoctors.length === 0) {
        const res = await fetch(`${API_BASE}/api/doctors`);
        cachedDoctors = await res.json();
    }
    if (cachedMedicines.length === 0) {
        const res = await fetch(`${API_BASE}/api/medicines`);
        cachedMedicines = await res.json();
    }

    const userOptions = cachedUsers.map(u => `<option value="${u.user_id}">${escapeHtml(u.name)} (${u.phone})</option>`).join('');
    const doctorOptions = cachedDoctors.map(d => `<option value="${d.doctor_id}">${escapeHtml(d.name)} - ${escapeHtml(d.specialization)}</option>`).join('');
    const medicineOptions = cachedMedicines.map(m => `<option value="${m.medicine_id}">${escapeHtml(m.name)} (${escapeHtml(m.brand || '')}) - Stock: ${m.stock_quantity}</option>`).join('');

    const today = new Date().toISOString().split('T')[0];

    const html = `
        <form onsubmit="handlePrescriptionSubmit(event)">
            <div class="form-group">
                <label>Select Patient *</label>
                <select class="form-control" name="user_id" required>
                    <option value="">-- Choose Patient --</option>
                    ${userOptions}
                </select>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Prescribing Doctor *</label>
                    <select class="form-control" name="doctor_id" required>
                        <option value="">-- Choose Doctor --</option>
                        ${doctorOptions}
                    </select>
                </div>
                <div class="form-group">
                    <label>Prescribed Medicine *</label>
                    <select class="form-control" name="medicine_id" required>
                        <option value="">-- Choose Medicine --</option>
                        ${medicineOptions}
                    </select>
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Date Issued *</label>
                    <input type="date" class="form-control" name="date_issued" value="${today}" required>
                </div>
                <div class="form-group">
                    <label>Duration (in days) *</label>
                    <input type="number" class="form-control" name="duration_days" value="5" min="1" required>
                </div>
            </div>
            <div class="form-group">
                <label>Dosage Instructions *</label>
                <input type="text" class="form-control" name="dosage" required placeholder="e.g. 500 mg twice daily after food">
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Issue Prescription</button>
            </div>
        </form>
    `;
    openModal('Issue New Prescription', html);
}

async function handlePrescriptionSubmit(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const body = Object.fromEntries(formData.entries());

    try {
        const res = await fetch(`${API_BASE}/api/prescriptions`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Prescription issued successfully!');
        closeModal();
        loadPrescriptions();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deletePrescription(id) {
    if (!confirm('Are you sure you want to delete this prescription? Any linked dose reminders will also be removed.')) return;
    try {
        const res = await fetch(`${API_BASE}/api/prescriptions/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Prescription deleted.');
        loadPrescriptions();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// -------------------------------------------------------------
// 6. DOSE REMINDERS MANAGEMENT
// -------------------------------------------------------------
let currentReminderFilter = '';

async function loadReminders() {
    const tbody = document.querySelector('#remindersTable tbody');
    try {
        let url = `${API_BASE}/api/reminders`;
        if (currentReminderFilter) url += `?status=${encodeURIComponent(currentReminderFilter)}`;
        const res = await fetch(url);
        const reminders = await res.json();

        if (reminders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="loading-cell">No reminders found.</td></tr>';
            return;
        }

        tbody.innerHTML = reminders.map(r => {
            let statusBadge = `<span class="badge badge-warning">${r.status}</span>`;
            if (r.status === 'Sent') statusBadge = `<span class="badge badge-info">${r.status}</span>`;
            if (r.status === 'Completed') statusBadge = `<span class="badge badge-success">${r.status}</span>`;

            return `
                <tr>
                    <td>#${r.reminder_id}</td>
                    <td><strong>${escapeHtml(r.user_name)}</strong> <br><small style="color:var(--text-muted)">${r.user_phone}</small></td>
                    <td>${escapeHtml(r.medicine_name || '-')} <br><small style="color:var(--accent-cyan)">${escapeHtml(r.dosage || '')}</small></td>
                    <td><strong>${formatDateTime(r.reminder_datetime)}</strong></td>
                    <td>${escapeHtml(r.message)}</td>
                    <td>${statusBadge}</td>
                    <td>
                        <div style="display:flex; gap:6px;">
                            ${r.status !== 'Completed' ? `
                                <button class="btn btn-sm btn-outline" onclick="updateReminderStatus(${r.reminder_id}, 'Completed')">
                                    <i class="fa-solid fa-check"></i> Complete
                                </button>
                            ` : '<span style="color:var(--text-muted); font-size:0.75rem;">Done</span>'}
                        </div>
                    </td>
                    <td>
                        <button class="btn-icon-sm" title="Delete Reminder" style="color:var(--danger-color)" onclick="deleteReminder(${r.reminder_id})">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="8" class="loading-cell">Error loading reminders</td></tr>';
    }
}

function filterReminders(status, el) {
    currentReminderFilter = status;
    document.querySelectorAll('#tab-reminders .filter-pill').forEach(btn => btn.classList.remove('active'));
    el.classList.add('active');
    loadReminders();
}

async function updateReminderStatus(id, newStatus) {
    try {
        const res = await fetch(`${API_BASE}/api/reminders/${id}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast(`Reminder marked as ${newStatus}`);
        loadReminders();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function openAddReminderModal() {
    // Need users and active prescriptions
    if (cachedPrescriptions.length === 0) {
        const res = await fetch(`${API_BASE}/api/prescriptions`);
        cachedPrescriptions = await res.json();
    }

    const prescriptionOptions = cachedPrescriptions.map(p => `
        <option value="${p.prescription_id}" data-user="${p.user_id}">
            #${p.prescription_id} - ${escapeHtml(p.user_name)}: ${escapeHtml(p.medicine_name)} (${escapeHtml(p.dosage)})
        </option>
    `).join('');

    // Default datetime to tomorrow 8:00 AM
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(8, 0, 0, 0);
    const dtDefault = tomorrow.toISOString().slice(0, 16);

    const html = `
        <form onsubmit="handleReminderSubmit(event)">
            <div class="form-group">
                <label>Linked Prescription & Patient *</label>
                <select class="form-control" name="prescription_id" id="reminderPrescriptionSelect" required onchange="handlePrescriptionSelection(this)">
                    <option value="">-- Select Prescription --</option>
                    ${prescriptionOptions}
                </select>
            </div>
            <input type="hidden" name="user_id" id="reminderUserId">
            <div class="form-group">
                <label>Reminder Date & Time *</label>
                <input type="datetime-local" class="form-control" name="reminder_datetime" value="${dtDefault}" required>
            </div>
            <div class="form-group">
                <label>Reminder Message *</label>
                <input type="text" class="form-control" name="message" id="reminderMessage" required placeholder="e.g. Take medicine after breakfast">
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Schedule Reminder</button>
            </div>
        </form>
    `;
    openModal('Schedule Dose Reminder', html);
}

function handlePrescriptionSelection(selectEl) {
    const opt = selectEl.options[selectEl.selectedIndex];
    const userId = opt.getAttribute('data-user');
    document.getElementById('reminderUserId').value = userId || '';
}

async function handleReminderSubmit(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const body = Object.fromEntries(formData.entries());

    // Format datetime string for MySQL DATETIME
    if (body.reminder_datetime) {
        body.reminder_datetime = body.reminder_datetime.replace('T', ' ') + ':00';
    }

    try {
        const res = await fetch(`${API_BASE}/api/reminders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Dose reminder scheduled!');
        closeModal();
        loadReminders();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteReminder(id) {
    if (!confirm('Are you sure you want to remove this reminder?')) return;
    try {
        const res = await fetch(`${API_BASE}/api/reminders/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);

        showToast('Reminder deleted.');
        loadReminders();
        loadDashboard();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// -------------------------------------------------------------
// 7. SQL ANALYTICS & REPORTS (From MySQL Queries)
// -------------------------------------------------------------
async function loadAnalytics() {
    try {
        const res = await fetch(`${API_BASE}/api/reports/analytics`);
        const data = await res.json();

        // 1. Stock Overview Banner
        if (data.stockOverview) {
            document.getElementById('analyticsTotalStock').textContent = `${data.stockOverview.total_stock || 0} Units`;
            document.getElementById('analyticsAvgPrice').textContent = `₹${data.stockOverview.avg_price || 0}`;
            document.getElementById('analyticsPriceRange').textContent = `₹${data.stockOverview.min_price || 0} - ₹${data.stockOverview.max_price || 0}`;
        }

        // 2. Most Prescribed Medicines
        const topMedList = document.getElementById('topPrescribedList');
        if (data.topPrescribed && data.topPrescribed.length > 0) {
            topMedList.innerHTML = data.topPrescribed.map(m => `
                <div class="analytics-row">
                    <span class="analytics-row-label"><strong>${escapeHtml(m.name)}</strong></span>
                    <span class="analytics-row-value">${m.times_prescribed} prescriptions</span>
                </div>
            `).join('');
        } else {
            topMedList.innerHTML = '<div class="loading-state">No data available</div>';
        }

        // 3. Doctor Consultation Load
        const docList = document.getElementById('doctorActivityList');
        if (data.doctorActivity && data.doctorActivity.length > 0) {
            docList.innerHTML = data.doctorActivity.map(d => `
                <div class="analytics-row">
                    <div>
                        <strong>${escapeHtml(d.name)}</strong>
                        <div style="font-size:0.75rem; color:var(--text-muted);">${escapeHtml(d.specialization)}</div>
                    </div>
                    <span class="analytics-row-value">${d.total_prescriptions} issued</span>
                </div>
            `).join('');
        } else {
            docList.innerHTML = '<div class="loading-state">No data available</div>';
        }

        // 4. Prescriptions per Patient
        const patientList = document.getElementById('patientPrescriptionsList');
        if (data.userPrescriptionCounts && data.userPrescriptionCounts.length > 0) {
            patientList.innerHTML = data.userPrescriptionCounts.map(u => `
                <div class="analytics-row">
                    <span class="analytics-row-label"><strong>${escapeHtml(u.name)}</strong></span>
                    <span class="analytics-row-value">${u.total_prescriptions} total</span>
                </div>
            `).join('');
        } else {
            patientList.innerHTML = '<div class="loading-state">No data available</div>';
        }

        // 5. Low Stock Alert Table
        const lowStockTbody = document.querySelector('#lowStockAnalyticsTable tbody');
        if (data.lowStock && data.lowStock.length > 0) {
            lowStockTbody.innerHTML = data.lowStock.map(m => `
                <tr>
                    <td><strong>${escapeHtml(m.name)}</strong></td>
                    <td>${escapeHtml(m.brand || '-')}</td>
                    <td><span class="badge badge-warning">${m.stock_quantity} left</span></td>
                    <td>₹${parseFloat(m.unit_price).toFixed(2)}</td>
                </tr>
            `).join('');
        } else {
            lowStockTbody.innerHTML = '<tr><td colspan="4" class="loading-cell">All medicines have adequate stock (>60 units).</td></tr>';
        }
    } catch (err) {
        console.error('Error loading analytics:', err);
    }
}

// -------------------------------------------------------------
// QUICK ACTION MODAL
// -------------------------------------------------------------
function openQuickActionModal() {
    const html = `
        <div style="display:grid; grid-template-columns: 1fr 1fr; gap:12px;">
            <button class="btn btn-secondary" style="padding:16px; justify-content:center; flex-direction:column; gap:8px;" onclick="closeModal(); openAddUserModal();">
                <i class="fa-solid fa-user-plus" style="font-size:1.5rem; color:var(--primary-color);"></i>
                <span>Add Patient</span>
            </button>
            <button class="btn btn-secondary" style="padding:16px; justify-content:center; flex-direction:column; gap:8px;" onclick="closeModal(); openAddDoctorModal();">
                <i class="fa-solid fa-user-doctor" style="font-size:1.5rem; color:var(--secondary-color);"></i>
                <span>Add Doctor</span>
            </button>
            <button class="btn btn-secondary" style="padding:16px; justify-content:center; flex-direction:column; gap:8px;" onclick="closeModal(); openAddMedicineModal();">
                <i class="fa-solid fa-pills" style="font-size:1.5rem; color:var(--accent-cyan);"></i>
                <span>Add Medicine</span>
            </button>
            <button class="btn btn-secondary" style="padding:16px; justify-content:center; flex-direction:column; gap:8px;" onclick="closeModal(); openAddPrescriptionModal();">
                <i class="fa-solid fa-file-prescription" style="font-size:1.5rem; color:var(--accent-purple);"></i>
                <span>Issue Prescription</span>
            </button>
        </div>
    `;
    openModal('Quick Actions', html);
}

// -------------------------------------------------------------
// UI UTILITIES: MODAL & TOAST
// -------------------------------------------------------------
function openModal(title, bodyHtml) {
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalBody').innerHTML = bodyHtml;
    document.getElementById('modalOverlay').classList.add('active');
}

function closeModal() {
    document.getElementById('modalOverlay').classList.remove('active');
}

function handleOverlayClick(e) {
    if (e.target.id === 'modalOverlay') closeModal();
}

function showToast(message, type = 'success') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type === 'error' ? 'toast-error' : ''}`;
    toast.innerHTML = `
        <i class="fa-solid ${type === 'error' ? 'fa-circle-exclamation' : 'fa-circle-check'}" style="color:${type === 'error' ? 'var(--danger-color)' : 'var(--primary-color)'};"></i>
        <span>${escapeHtml(message)}</span>
    `;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"']/g, m => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[m]);
}

function formatDateTime(dtStr) {
    if (!dtStr) return '-';
    try {
        const d = new Date(dtStr);
        return d.toLocaleString('en-US', {
            month: 'short', day: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    } catch {
        return dtStr;
    }
}

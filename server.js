const express = require('express');
const cors = require('cors');
const path = require('path');
const { pool, testConnection } = require('./db');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// -------------------------------------------------------------
// SYSTEM STATUS & DASHBOARD KPIS
// -------------------------------------------------------------
app.get('/api/status', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT 1 + 1 AS result');
        res.json({
            status: 'online',
            database: process.env.DB_NAME || 'medicine_management',
            mysqlConnected: true,
            timestamp: new Date().toISOString()
        });
    } catch (err) {
        res.status(500).json({
            status: 'degraded',
            mysqlConnected: false,
            error: err.message
        });
    }
});

app.get('/api/stats', async (req, res) => {
    try {
        const [[{ total_users }]] = await pool.query('SELECT COUNT(*) AS total_users FROM users');
        const [[{ total_doctors }]] = await pool.query('SELECT COUNT(*) AS total_doctors FROM doctor');
        const [[{ total_medicines }]] = await pool.query('SELECT COUNT(*) AS total_medicines FROM medicine');
        const [[{ low_stock_count }]] = await pool.query('SELECT COUNT(*) AS low_stock_count FROM medicine WHERE stock_quantity < 60');
        const [[{ active_prescriptions }]] = await pool.query("SELECT COUNT(*) AS active_prescriptions FROM prescription WHERE status = 'Active'");
        const [[{ pending_reminders }]] = await pool.query("SELECT COUNT(*) AS pending_reminders FROM reminder WHERE status = 'Pending'");

        res.json({
            users: total_users,
            doctors: total_doctors,
            medicines: total_medicines,
            lowStock: low_stock_count,
            activePrescriptions: active_prescriptions,
            pendingReminders: pending_reminders
        });
    } catch (err) {
        console.error('Error fetching stats:', err);
        res.status(500).json({ error: err.message });
    }
});

// -------------------------------------------------------------
// USERS / PATIENTS ROUTES
// -------------------------------------------------------------
app.get('/api/users', async (req, res) => {
    try {
        const search = req.query.q;
        let query = 'SELECT * FROM users';
        let params = [];
        if (search) {
            query += ' WHERE name LIKE ? OR phone LIKE ? OR email LIKE ?';
            const term = `%${search}%`;
            params = [term, term, term];
        }
        query += ' ORDER BY user_id DESC';
        const [users] = await pool.query(query, params);
        res.json(users);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.get('/api/users/:id', async (req, res) => {
    try {
        const [users] = await pool.query('SELECT * FROM users WHERE user_id = ?', [req.params.id]);
        if (users.length === 0) return res.status(404).json({ error: 'User not found' });

        const [prescriptions] = await pool.query(`
            SELECT p.*, d.name AS doctor_name, m.name AS medicine_name
            FROM prescription p
            JOIN doctor d ON p.doctor_id = d.doctor_id
            JOIN medicine m ON p.medicine_id = m.medicine_id
            WHERE p.user_id = ?
            ORDER BY p.date_issued DESC
        `, [req.params.id]);

        const [reminders] = await pool.query(`
            SELECT * FROM reminder WHERE user_id = ? ORDER BY reminder_datetime ASC
        `, [req.params.id]);

        res.json({
            ...users[0],
            prescriptions,
            reminders
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/users', async (req, res) => {
    try {
        const { name, email, phone, dob, address } = req.body;
        if (!name || !phone) {
            return res.status(400).json({ error: 'Name and Phone are required' });
        }
        const [result] = await pool.query(
            'INSERT INTO users (name, email, phone, dob, address) VALUES (?, ?, ?, ?, ?)',
            [name, email || null, phone, dob || null, address || null]
        );
        res.status(201).json({
            user_id: result.insertId,
            name, email, phone, dob, address,
            message: 'User created successfully'
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.put('/api/users/:id', async (req, res) => {
    try {
        const { name, email, phone, dob, address } = req.body;
        await pool.query(
            'UPDATE users SET name = ?, email = ?, phone = ?, dob = ?, address = ? WHERE user_id = ?',
            [name, email, phone, dob, address, req.params.id]
        );
        res.json({ message: 'User updated successfully' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/users/:id', async (req, res) => {
    try {
        await pool.query('DELETE FROM users WHERE user_id = ?', [req.params.id]);
        res.json({ message: 'User deleted successfully' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// -------------------------------------------------------------
// DOCTORS ROUTES
// -------------------------------------------------------------
app.get('/api/doctors', async (req, res) => {
    try {
        const search = req.query.q;
        let query = 'SELECT * FROM doctor';
        let params = [];
        if (search) {
            query += ' WHERE name LIKE ? OR specialization LIKE ? OR phone LIKE ?';
            const term = `%${search}%`;
            params = [term, term, term];
        }
        query += ' ORDER BY doctor_id DESC';
        const [doctors] = await pool.query(query, params);
        res.json(doctors);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/doctors', async (req, res) => {
    try {
        const { name, specialization, phone } = req.body;
        if (!name || !specialization) {
            return res.status(400).json({ error: 'Name and Specialization are required' });
        }
        const [result] = await pool.query(
            'INSERT INTO doctor (name, specialization, phone) VALUES (?, ?, ?)',
            [name, specialization, phone || null]
        );
        res.status(201).json({
            doctor_id: result.insertId,
            name, specialization, phone,
            message: 'Doctor added successfully'
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.put('/api/doctors/:id', async (req, res) => {
    try {
        const { name, specialization, phone } = req.body;
        await pool.query(
            'UPDATE doctor SET name = ?, specialization = ?, phone = ? WHERE doctor_id = ?',
            [name, specialization, phone, req.params.id]
        );
        res.json({ message: 'Doctor updated successfully' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/doctors/:id', async (req, res) => {
    try {
        await pool.query('DELETE FROM doctor WHERE doctor_id = ?', [req.params.id]);
        res.json({ message: 'Doctor deleted successfully' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// -------------------------------------------------------------
// MEDICINES (INVENTORY) ROUTES
// -------------------------------------------------------------
app.get('/api/medicines', async (req, res) => {
    try {
        const { q, lowStock } = req.query;
        let conditions = [];
        let params = [];

        if (lowStock === 'true') {
            conditions.push('stock_quantity < 60');
        }
        if (q) {
            conditions.push('(name LIKE ? OR brand LIKE ?)');
            params.push(`%${q}%`, `%${q}%`);
        }

        let query = 'SELECT * FROM medicine';
        if (conditions.length > 0) {
            query += ' WHERE ' + conditions.join(' AND ');
        }
        query += ' ORDER BY name ASC';

        const [medicines] = await pool.query(query, params);
        res.json(medicines);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/medicines', async (req, res) => {
    try {
        const { name, brand, expiry_date, stock_quantity, unit_price } = req.body;
        if (!name || !expiry_date || unit_price === undefined) {
            return res.status(400).json({ error: 'Name, Expiry Date, and Unit Price are required' });
        }
        const [result] = await pool.query(
            'INSERT INTO medicine (name, brand, expiry_date, stock_quantity, unit_price) VALUES (?, ?, ?, ?, ?)',
            [name, brand || null, expiry_date, stock_quantity || 0, unit_price]
        );
        res.status(201).json({
            medicine_id: result.insertId,
            name, brand, expiry_date, stock_quantity, unit_price,
            message: 'Medicine added successfully'
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.put('/api/medicines/:id', async (req, res) => {
    try {
        const { name, brand, expiry_date, stock_quantity, unit_price } = req.body;
        await pool.query(
            'UPDATE medicine SET name = ?, brand = ?, expiry_date = ?, stock_quantity = ?, unit_price = ? WHERE medicine_id = ?',
            [name, brand, expiry_date, stock_quantity, unit_price, req.params.id]
        );
        res.json({ message: 'Medicine updated successfully' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.patch('/api/medicines/:id/stock', async (req, res) => {
    try {
        const { adjustment } = req.body; // e.g. +10 or -5
        if (adjustment === undefined) {
            return res.status(400).json({ error: 'Adjustment number required' });
        }
        await pool.query(
            'UPDATE medicine SET stock_quantity = GREATEST(0, stock_quantity + ?) WHERE medicine_id = ?',
            [adjustment, req.params.id]
        );
        const [[updated]] = await pool.query('SELECT * FROM medicine WHERE medicine_id = ?', [req.params.id]);
        res.json({ message: 'Stock updated', medicine: updated });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/medicines/:id', async (req, res) => {
    try {
        await pool.query('DELETE FROM medicine WHERE medicine_id = ?', [req.params.id]);
        res.json({ message: 'Medicine deleted successfully' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// -------------------------------------------------------------
// PRESCRIPTION ROUTES
// -------------------------------------------------------------
app.get('/api/prescriptions', async (req, res) => {
    try {
        const { status, user_id } = req.query;
        let conditions = [];
        let params = [];

        if (status) {
            conditions.push('p.status = ?');
            params.push(status);
        }
        if (user_id) {
            conditions.push('p.user_id = ?');
            params.push(user_id);
        }

        let query = `
            SELECT 
                p.prescription_id, p.user_id, p.doctor_id, p.medicine_id,
                p.date_issued, p.dosage, p.duration_days, p.status,
                u.name AS user_name, u.phone AS user_phone,
                d.name AS doctor_name, d.specialization AS doctor_specialization,
                m.name AS medicine_name, m.brand AS medicine_brand, m.unit_price
            FROM prescription p
            JOIN users u ON p.user_id = u.user_id
            JOIN doctor d ON p.doctor_id = d.doctor_id
            JOIN medicine m ON p.medicine_id = m.medicine_id
        `;

        if (conditions.length > 0) {
            query += ' WHERE ' + conditions.join(' AND ');
        }
        query += ' ORDER BY p.prescription_id DESC';

        const [prescriptions] = await pool.query(query, params);
        res.json(prescriptions);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/prescriptions', async (req, res) => {
    try {
        const { user_id, doctor_id, medicine_id, date_issued, dosage, duration_days, status } = req.body;
        if (!user_id || !doctor_id || !medicine_id || !dosage || !duration_days) {
            return res.status(400).json({ error: 'All fields (Patient, Doctor, Medicine, Dosage, Duration) are required' });
        }
        const today = date_issued || new Date().toISOString().split('T')[0];
        const [result] = await pool.query(
            'INSERT INTO prescription (user_id, doctor_id, medicine_id, date_issued, dosage, duration_days, status) VALUES (?, ?, ?, ?, ?, ?, ?)',
            [user_id, doctor_id, medicine_id, today, dosage, duration_days, status || 'Active']
        );
        res.status(201).json({
            prescription_id: result.insertId,
            message: 'Prescription created successfully'
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.patch('/api/prescriptions/:id/status', async (req, res) => {
    try {
        const { status } = req.body;
        if (!['Active', 'Completed', 'Cancelled'].includes(status)) {
            return res.status(400).json({ error: 'Invalid status' });
        }
        await pool.query('UPDATE prescription SET status = ? WHERE prescription_id = ?', [status, req.params.id]);
        res.json({ message: 'Prescription status updated' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/prescriptions/:id', async (req, res) => {
    try {
        await pool.query('DELETE FROM prescription WHERE prescription_id = ?', [req.params.id]);
        res.json({ message: 'Prescription deleted successfully' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// -------------------------------------------------------------
// REMINDERS ROUTES
// -------------------------------------------------------------
app.get('/api/reminders', async (req, res) => {
    try {
        const { status } = req.query;
        let query = `
            SELECT 
                r.reminder_id, r.user_id, r.prescription_id, r.reminder_datetime, r.message, r.status,
                u.name AS user_name, u.phone AS user_phone,
                p.dosage,
                m.name AS medicine_name
            FROM reminder r
            JOIN users u ON r.user_id = u.user_id
            LEFT JOIN prescription p ON r.prescription_id = p.prescription_id
            LEFT JOIN medicine m ON p.medicine_id = m.medicine_id
        `;
        let params = [];
        if (status) {
            query += ' WHERE r.status = ?';
            params.push(status);
        }
        query += ' ORDER BY r.reminder_datetime ASC';

        const [reminders] = await pool.query(query, params);
        res.json(reminders);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/reminders', async (req, res) => {
    try {
        const { user_id, prescription_id, reminder_datetime, message, status } = req.body;
        if (!user_id || !prescription_id || !reminder_datetime || !message) {
            return res.status(400).json({ error: 'Patient, Prescription, Datetime, and Message are required' });
        }
        const [result] = await pool.query(
            'INSERT INTO reminder (user_id, prescription_id, reminder_datetime, message, status) VALUES (?, ?, ?, ?, ?)',
            [user_id, prescription_id, reminder_datetime, message, status || 'Pending']
        );
        res.status(201).json({
            reminder_id: result.insertId,
            message: 'Reminder scheduled successfully'
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.patch('/api/reminders/:id/status', async (req, res) => {
    try {
        const { status } = req.body;
        if (!['Pending', 'Sent', 'Completed'].includes(status)) {
            return res.status(400).json({ error: 'Invalid status' });
        }
        await pool.query('UPDATE reminder SET status = ? WHERE reminder_id = ?', [status, req.params.id]);
        res.json({ message: 'Reminder status updated' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/reminders/:id', async (req, res) => {
    try {
        await pool.query('DELETE FROM reminder WHERE reminder_id = ?', [req.params.id]);
        res.json({ message: 'Reminder deleted successfully' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// -------------------------------------------------------------
// ANALYTICS & REPORTS (Executes your SQL queries)
// -------------------------------------------------------------
app.get('/api/reports/analytics', async (req, res) => {
    try {
        // 1. Stock & Pricing Aggregates
        const [[stockOverview]] = await pool.query(`
            SELECT 
                SUM(stock_quantity) AS total_stock,
                ROUND(AVG(unit_price), 2) AS avg_price,
                MAX(unit_price) AS max_price,
                MIN(unit_price) AS min_price
            FROM medicine
        `);

        // 2. Times Each Medicine Has Been Prescribed
        const [topPrescribed] = await pool.query(`
            SELECT 
                m.name,
                COUNT(p.prescription_id) AS times_prescribed
            FROM medicine m
            LEFT JOIN prescription p ON m.medicine_id = p.medicine_id
            GROUP BY m.medicine_id, m.name
            ORDER BY times_prescribed DESC
        `);

        // 3. Prescriptions Count per User
        const [userPrescriptionCounts] = await pool.query(`
            SELECT 
                u.name,
                COUNT(p.prescription_id) AS total_prescriptions
            FROM users u
            LEFT JOIN prescription p ON u.user_id = p.user_id
            GROUP BY u.user_id, u.name
            ORDER BY total_prescriptions DESC
        `);

        // 4. Doctor Prescription Activity
        const [doctorActivity] = await pool.query(`
            SELECT 
                d.name,
                d.specialization,
                COUNT(p.prescription_id) AS total_prescriptions
            FROM doctor d
            LEFT JOIN prescription p ON d.doctor_id = p.doctor_id
            GROUP BY d.doctor_id, d.name, d.specialization
            ORDER BY total_prescriptions DESC
        `);

        // 5. Low Stock Medicines (< 60)
        const [lowStock] = await pool.query(`
            SELECT name, brand, stock_quantity, unit_price
            FROM medicine
            WHERE stock_quantity < 60
            ORDER BY stock_quantity ASC
        `);

        // 6. Active Prescriptions Overview with details
        const [activePrescriptions] = await pool.query(`
            SELECT u.name AS user_name, d.name AS doctor_name, m.name AS medicine_name, p.dosage, p.duration_days
            FROM prescription p
            JOIN users u ON p.user_id = u.user_id
            JOIN doctor d ON p.doctor_id = d.doctor_id
            JOIN medicine m ON p.medicine_id = m.medicine_id
            WHERE p.status = 'Active'
        `);

        res.json({
            stockOverview,
            topPrescribed,
            userPrescriptionCounts,
            doctorActivity,
            lowStock,
            activePrescriptions
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Fallback to index.html for SPA
app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start Server locally if run directly
if (require.main === module) {
    app.listen(PORT, async () => {
        console.log(`======================================================`);
        console.log(` Health & Medicine Management Server is RUNNING!`);
        console.log(` Local URL: http://localhost:${PORT}`);
        console.log(`======================================================`);
        await testConnection();
    });
}

module.exports = app;

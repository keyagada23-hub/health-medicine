const mysql = require('mysql2/promise');
require('dotenv').config();

const isCloud = process.env.DB_HOST && 
                process.env.DB_HOST !== 'localhost' && 
                process.env.DB_HOST !== '127.0.0.1';

const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT, 10) || 3306,
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'medicine_management',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0,
    dateStrings: true,
    ssl: (process.env.DB_SSL === 'true' || isCloud) ? { minVersion: 'TLSv1.2', rejectUnauthorized: false } : undefined
});

// Test connection on startup
async function testConnection() {
    try {
        const connection = await pool.getConnection();
        console.log(`[DB] Successfully connected to MySQL database "${process.env.DB_NAME || 'medicine_management'}" on ${process.env.DB_HOST || 'localhost'}:${process.env.DB_PORT || 3306}`);
        connection.release();
        return true;
    } catch (err) {
        console.error('[DB] MySQL Connection Error:', err.message);
        return false;
    }
}

module.exports = {
    pool,
    testConnection
};

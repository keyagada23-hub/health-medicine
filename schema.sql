-- Medicine Management Database Schema & Seed Script
CREATE DATABASE IF NOT EXISTS medicine_management;
USE medicine_management;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(15) NOT NULL,
    dob DATE,
    address VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS doctor (
    doctor_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    phone VARCHAR(15) UNIQUE
);

CREATE TABLE IF NOT EXISTS medicine (
    medicine_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    brand VARCHAR(100),
    expiry_date DATE NOT NULL,
    stock_quantity INT DEFAULT 0,
    unit_price DECIMAL(10,2) NOT NULL,
    CONSTRAINT chk_stock CHECK (stock_quantity >= 0),
    CONSTRAINT chk_price CHECK (unit_price >= 0)
);

CREATE TABLE IF NOT EXISTS prescription (
    prescription_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    doctor_id INT NOT NULL,
    medicine_id INT NOT NULL,
    date_issued DATE NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    duration_days INT NOT NULL,
    status ENUM('Active', 'Completed', 'Cancelled') DEFAULT 'Active',
    CONSTRAINT chk_duration CHECK (duration_days > 0),
    CONSTRAINT fk_prescription_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(doctor_id) ON DELETE CASCADE,
    CONSTRAINT fk_prescription_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reminder (
    reminder_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    prescription_id INT NOT NULL,
    reminder_datetime DATETIME NOT NULL,
    message VARCHAR(255) NOT NULL,
    status ENUM('Pending', 'Sent', 'Completed') DEFAULT 'Pending',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (prescription_id) REFERENCES prescription(prescription_id) ON DELETE CASCADE
);

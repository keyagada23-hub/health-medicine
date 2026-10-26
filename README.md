# Health & Medicine Management System (Java Swing + MySQL)

A complete **Java Swing Desktop Application (GUI)** connected directly to your local **MySQL 8.0** database (`medicine_management`) via **JDBC**.

---

## 🚀 How to Run the Java Application

### Option 1: Double-click
Simply double-click the **`run_app.bat`** file in `c:\Users\Keya\Documents\health`.

### Option 2: From Terminal / PowerShell
```powershell
cd c:\Users\Keya\Documents\health
javac -cp ".;mysql-connector-j-8.3.0.jar" DBConnection.java MedicineApp.java
java -cp ".;mysql-connector-j-8.3.0.jar" MedicineApp
```

---

## 🗄️ Database Connection
- **Database**: `medicine_management`
- **Host**: `localhost:3306`
- **Driver**: `com.mysql.cj.jdbc.Driver` (`mysql-connector-j-8.3.0.jar`)
- **Connection Helper**: `DBConnection.java`

---

## 🖥️ Modules & Features Included in Java GUI

1. **👥 Patients (Users)**:
   - Live `JTable` of patients.
   - Form inputs for Name, Email, Phone, DOB, and Address.
   - Click any row to automatically load details for instant Editing or Deletion.
   - Real-time search filter field.

2. **🩺 Doctors**:
   - Live `JTable` of registered doctors.
   - Form inputs for Doctor Name, Specialization, and Phone.
   - Add, Update, and Delete actions.
   - Search filter field.

3. **💊 Medicines (Inventory)**:
   - Full inventory management table with unit prices (₹) and stock quantities.
   - Expiry date tracking (YYYY-MM-DD).
   - **Quick Stock Adjust Buttons**: `+10 Stock` and `-5 Stock` for instant inventory updates.
   - Low Stock warning filter (`< 60 units`).

4. **📋 Prescriptions**:
   - `JTable` displaying prescriptions with joined Patient, Doctor, and Medicine names.
   - Interactive dropdown selectors populated live from MySQL (`Users`, `Doctor`, `Medicine`).
   - Status updates (`Active`, `Completed`, `Cancelled`).

5. **⏰ Dose Reminders**:
   - Schedule medication reminders with datetime, message, and patient info.
   - One-click `Mark Completed` button.

6. **📊 SQL Reports & Analytics**:
   - Runs and displays all the analytical SQL queries:
     - Medicine stock unit totals, average unit price, min/max price.
     - Most prescribed medicines frequency table.
     - Prescriptions count per patient.
     - Low stock alert list.
     - Active prescriptions summary.

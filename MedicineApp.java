import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MedicineApp extends JFrame {

    // Theme Colors
    private static final Color PRIMARY_COLOR = new Color(2, 132, 199);    // Medical Cyan/Blue
    private static final Color SUCCESS_COLOR = new Color(22, 163, 74);   // Forest Green
    private static final Color WARNING_COLOR = new Color(217, 119, 6);   // Amber
    private static final Color DANGER_COLOR = new Color(220, 38, 38);    // Red
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(226, 232, 240);

    // KPI Summary Labels
    private JLabel lblKpiUsers, lblKpiDocs, lblKpiMeds, lblKpiPresc, lblKpiLowStock, lblKpiReminders;

    // Tabs
    private JTabbedPane tabbedPane;

    // --- TAB 1: USERS / PATIENTS ---
    private JTable usersTable;
    private DefaultTableModel usersModel;
    private JTextField txtUserId, txtUserName, txtUserEmail, txtUserPhone, txtUserDob, txtUserAddress;
    private JTextField txtUserSearch;

    // --- TAB 2: DOCTORS ---
    private JTable doctorsTable;
    private DefaultTableModel doctorsModel;
    private JTextField txtDocId, txtDocName, txtDocSpec, txtDocPhone;
    private JTextField txtDocSearch;

    // --- TAB 3: MEDICINES ---
    private JTable medicinesTable;
    private DefaultTableModel medicinesModel;
    private JTextField txtMedId, txtMedName, txtMedBrand, txtMedExpiry, txtMedStock, txtMedPrice;
    private JTextField txtMedSearch;

    // --- TAB 4: PRESCRIPTIONS ---
    private JTable prescriptionsTable;
    private DefaultTableModel prescriptionsModel;
    private JTextField txtPrescId, txtPrescDosage, txtPrescDuration, txtPrescDate;
    private JComboBox<ComboItem> cbPrescUser, cbPrescDoctor, cbPrescMedicine;
    private JComboBox<String> cbPrescStatus;

    // --- TAB 5: REMINDERS ---
    private JTable remindersTable;
    private DefaultTableModel remindersModel;
    private JTextField txtRemId, txtRemDateTime, txtRemMessage;
    private JComboBox<ComboItem> cbRemPrescription;
    private JComboBox<String> cbRemStatus;

    // --- TAB 6: REPORTS & SQL QUERIES ---
    private JTextArea txtReportsOutput;

    public MedicineApp() {
        setTitle("MediSync - Health & Medicine Management System");
        setSize(1200, 780);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        loadAllData();
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 10));
        rootPanel.setBackground(new Color(248, 250, 252));
        rootPanel.setBorder(new EmptyBorder(12, 16, 16, 16));

        // 1. TOP HEADER & KPI CARDS
        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);

        // Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(CARD_BG);
        headerPanel.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(14, 20, 14, 20)
        ));

        // Brand Logo & Title
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandBox.setOpaque(false);

        JLabel lblLogo = new JLabel("🏥");
        lblLogo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JPanel titleText = new JPanel(new GridLayout(2, 1, 0, 2));
        titleText.setOpaque(false);
        JLabel lblTitle = new JLabel("MediSync - Health & Medicine Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSubtitle = new JLabel("Full-Stack Java Swing Application | Connected via JDBC to MySQL");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(100, 116, 139));

        titleText.add(lblTitle);
        titleText.add(lblSubtitle);
        brandBox.add(lblLogo);
        brandBox.add(titleText);

        // Connection Pill Badge
        JPanel statusPill = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        statusPill.setBackground(new Color(220, 252, 231));
        statusPill.setBorder(new CompoundBorder(
                new LineBorder(new Color(187, 247, 208), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        JLabel lblStatus = new JLabel("● MySQL: medicine_management (3306)");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(SUCCESS_COLOR);
        statusPill.add(lblStatus);

        headerPanel.add(brandBox, BorderLayout.WEST);
        headerPanel.add(statusPill, BorderLayout.EAST);
        topContainer.add(headerPanel, BorderLayout.NORTH);

        // KPI Stat Cards Grid
        JPanel kpiGrid = new JPanel(new GridLayout(1, 6, 10, 0));
        kpiGrid.setOpaque(false);

        lblKpiUsers = new JLabel("-");
        lblKpiDocs = new JLabel("-");
        lblKpiMeds = new JLabel("-");
        lblKpiPresc = new JLabel("-");
        lblKpiLowStock = new JLabel("-");
        lblKpiReminders = new JLabel("-");

        kpiGrid.add(createKpiCard("Patients", lblKpiUsers, new Color(2, 132, 199), "👥"));
        kpiGrid.add(createKpiCard("Doctors", lblKpiDocs, new Color(79, 70, 229), "🩺"));
        kpiGrid.add(createKpiCard("Medicines", lblKpiMeds, new Color(13, 148, 136), "💊"));
        kpiGrid.add(createKpiCard("Active Presc.", lblKpiPresc, SUCCESS_COLOR, "📋"));
        kpiGrid.add(createKpiCard("Low Stock", lblKpiLowStock, WARNING_COLOR, "⚠️"));
        kpiGrid.add(createKpiCard("Reminders", lblKpiReminders, new Color(147, 51, 234), "⏰"));

        topContainer.add(kpiGrid, BorderLayout.SOUTH);
        rootPanel.add(topContainer, BorderLayout.NORTH);

        // 2. MAIN TABS
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("  👥 Patients (Users)  ", createUsersPanel());
        tabbedPane.addTab("  🩺 Doctors  ", createDoctorsPanel());
        tabbedPane.addTab("  💊 Medicines Inventory  ", createMedicinesPanel());
        tabbedPane.addTab("  📋 Prescriptions  ", createPrescriptionsPanel());
        tabbedPane.addTab("  ⏰ Dose Reminders  ", createRemindersPanel());
        tabbedPane.addTab("  📊 SQL Reports & Analytics  ", createReportsPanel());

        rootPanel.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(rootPanel);
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, Color accentColor, String icon) {
        JPanel card = new JPanel(new BorderLayout(8, 2));
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 1));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTitle.setForeground(new Color(100, 116, 139));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        valueLabel.setForeground(accentColor);

        textPanel.add(lblTitle);
        textPanel.add(valueLabel);

        card.add(lblIcon, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // 1. PATIENTS (USERS) TAB
    // =========================================================================
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Left Form Card
        JPanel formCard = new JPanel(new BorderLayout());
        formCard.setBackground(CARD_BG);
        formCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
        formCard.setPreferredSize(new Dimension(340, 0));

        JLabel formTitle = new JLabel("Patient Registration & Profile");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formTitle.setForeground(new Color(15, 23, 42));
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setOpaque(false);
        formBody.setBorder(new EmptyBorder(14, 0, 14, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        txtUserId = createStyledTextField(false);
        txtUserName = createStyledTextField(true);
        txtUserEmail = createStyledTextField(true);
        txtUserPhone = createStyledTextField(true);
        txtUserDob = createStyledTextField(true);
        txtUserAddress = createStyledTextField(true);

        int row = 0;
        addFormRow(formBody, gbc, "User ID (Auto):", txtUserId, row++);
        addFormRow(formBody, gbc, "Full Name *:", txtUserName, row++);
        addFormRow(formBody, gbc, "Phone Number *:", txtUserPhone, row++);
        addFormRow(formBody, gbc, "Email Address:", txtUserEmail, row++);
        addFormRow(formBody, gbc, "DOB (YYYY-MM-DD):", txtUserDob, row++);
        addFormRow(formBody, gbc, "Address / City:", txtUserAddress, row++);

        // Actions
        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        btnGrid.setOpaque(false);

        JButton btnAdd = createStyledButton("Add Patient", PRIMARY_COLOR, Color.WHITE);
        JButton btnUpdate = createStyledButton("Update", new Color(79, 70, 229), Color.WHITE);
        JButton btnDelete = createStyledButton("Delete", DANGER_COLOR, Color.WHITE);
        JButton btnClear = createStyledButton("Clear", new Color(241, 245, 249), new Color(51, 65, 85));

        btnAdd.addActionListener(e -> addUser());
        btnUpdate.addActionListener(e -> updateUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnClear.addActionListener(e -> clearUserForm());

        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formBody.add(btnGrid, gbc);

        gbc.gridy = row++; gbc.weighty = 1.0;
        formBody.add(new JLabel(""), gbc);

        formCard.add(formBody, BorderLayout.CENTER);

        // Right Table Card
        JPanel tableCard = new JPanel(new BorderLayout(8, 8));
        tableCard.setBackground(CARD_BG);
        tableCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        // Search Bar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setOpaque(false);
        JLabel lblSearch = new JLabel("🔍 Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtUserSearch = createStyledTextField(true);
        txtUserSearch.setPreferredSize(new Dimension(240, 32));
        txtUserSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterTable(usersTable, txtUserSearch.getText().trim());
            }
        });
        searchBar.add(lblSearch);
        searchBar.add(txtUserSearch);
        tableCard.add(searchBar, BorderLayout.NORTH);

        String[] cols = {"User ID", "Full Name", "Phone", "Email", "DOB", "Address"};
        usersModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = new JTable(usersModel);
        styleTable(usersTable);

        usersTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int r = usersTable.getSelectedRow();
                if (r != -1) {
                    int mr = usersTable.convertRowIndexToModel(r);
                    txtUserId.setText(usersModel.getValueAt(mr, 0).toString());
                    txtUserName.setText(String.valueOf(usersModel.getValueAt(mr, 1)));
                    txtUserPhone.setText(String.valueOf(usersModel.getValueAt(mr, 2)));
                    txtUserEmail.setText(usersModel.getValueAt(mr, 3) == null ? "" : usersModel.getValueAt(mr, 3).toString());
                    txtUserDob.setText(usersModel.getValueAt(mr, 4) == null ? "" : usersModel.getValueAt(mr, 4).toString());
                    txtUserAddress.setText(usersModel.getValueAt(mr, 5) == null ? "" : usersModel.getValueAt(mr, 5).toString());
                }
            }
        });

        tableCard.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        panel.add(formCard, BorderLayout.WEST);
        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    private void loadUsers() {
        usersModel.setRowCount(0);
        String sql = "SELECT * FROM Users ORDER BY user_id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usersModel.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getDate("dob"),
                        rs.getString("address")
                });
            }
        } catch (SQLException ex) {
            showError("Failed to load users: " + ex.getMessage());
        }
        updateKpis();
    }

    private void addUser() {
        String name = txtUserName.getText().trim();
        String phone = txtUserPhone.getText().trim();
        String email = txtUserEmail.getText().trim();
        String dob = txtUserDob.getText().trim();
        String addr = txtUserAddress.getText().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            showWarning("Name and Phone are required.");
            return;
        }

        String sql = "INSERT INTO Users (name, email, phone, dob, address) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email.isEmpty() ? null : email);
            ps.setString(3, phone);
            ps.setDate(4, dob.isEmpty() ? null : Date.valueOf(dob));
            ps.setString(5, addr.isEmpty() ? null : addr);
            ps.executeUpdate();

            showInfo("Patient added successfully!");
            clearUserForm();
            loadUsers();
            refreshPrescriptionDropdowns();
        } catch (Exception ex) {
            showError("Error adding user: " + ex.getMessage());
        }
    }

    private void updateUser() {
        String idStr = txtUserId.getText().trim();
        if (idStr.isEmpty()) {
            showWarning("Select a patient from the table to update.");
            return;
        }

        String sql = "UPDATE Users SET name = ?, email = ?, phone = ?, dob = ?, address = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txtUserName.getText().trim());
            ps.setString(2, txtUserEmail.getText().trim().isEmpty() ? null : txtUserEmail.getText().trim());
            ps.setString(3, txtUserPhone.getText().trim());
            ps.setDate(4, txtUserDob.getText().trim().isEmpty() ? null : Date.valueOf(txtUserDob.getText().trim()));
            ps.setString(5, txtUserAddress.getText().trim().isEmpty() ? null : txtUserAddress.getText().trim());
            ps.setInt(6, Integer.parseInt(idStr));
            ps.executeUpdate();

            showInfo("Patient updated successfully!");
            clearUserForm();
            loadUsers();
            refreshPrescriptionDropdowns();
        } catch (Exception ex) {
            showError("Error updating user: " + ex.getMessage());
        }
    }

    private void deleteUser() {
        String idStr = txtUserId.getText().trim();
        if (idStr.isEmpty()) {
            showWarning("Select a patient to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete patient #" + idStr + "? Related prescriptions will also be removed.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM Users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idStr));
            ps.executeUpdate();

            showInfo("Patient deleted.");
            clearUserForm();
            loadUsers();
            loadPrescriptions();
            refreshPrescriptionDropdowns();
        } catch (Exception ex) {
            showError("Error deleting user: " + ex.getMessage());
        }
    }

    private void clearUserForm() {
        txtUserId.setText("");
        txtUserName.setText("");
        txtUserEmail.setText("");
        txtUserPhone.setText("");
        txtUserDob.setText("");
        txtUserAddress.setText("");
    }

    // =========================================================================
    // 2. DOCTORS TAB
    // =========================================================================
    private JPanel createDoctorsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Form Card
        JPanel formCard = new JPanel(new BorderLayout());
        formCard.setBackground(CARD_BG);
        formCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
        formCard.setPreferredSize(new Dimension(340, 0));

        JLabel formTitle = new JLabel("Doctor Profile & Staff");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formTitle.setForeground(new Color(15, 23, 42));
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setOpaque(false);
        formBody.setBorder(new EmptyBorder(14, 0, 14, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        txtDocId = createStyledTextField(false);
        txtDocName = createStyledTextField(true);
        txtDocSpec = createStyledTextField(true);
        txtDocPhone = createStyledTextField(true);

        int row = 0;
        addFormRow(formBody, gbc, "Doctor ID (Auto):", txtDocId, row++);
        addFormRow(formBody, gbc, "Doctor Name *:", txtDocName, row++);
        addFormRow(formBody, gbc, "Specialization *:", txtDocSpec, row++);
        addFormRow(formBody, gbc, "Contact Phone:", txtDocPhone, row++);

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        btnGrid.setOpaque(false);

        JButton btnAdd = createStyledButton("Add Doctor", PRIMARY_COLOR, Color.WHITE);
        JButton btnUpdate = createStyledButton("Update", new Color(79, 70, 229), Color.WHITE);
        JButton btnDelete = createStyledButton("Delete", DANGER_COLOR, Color.WHITE);
        JButton btnClear = createStyledButton("Clear", new Color(241, 245, 249), new Color(51, 65, 85));

        btnAdd.addActionListener(e -> addDoctor());
        btnUpdate.addActionListener(e -> updateDoctor());
        btnDelete.addActionListener(e -> deleteDoctor());
        btnClear.addActionListener(e -> clearDoctorForm());

        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formBody.add(btnGrid, gbc);

        gbc.gridy = row++; gbc.weighty = 1.0;
        formBody.add(new JLabel(""), gbc);

        formCard.add(formBody, BorderLayout.CENTER);

        // Table Card
        JPanel tableCard = new JPanel(new BorderLayout(8, 8));
        tableCard.setBackground(CARD_BG);
        tableCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setOpaque(false);
        JLabel lblSearch = new JLabel("🔍 Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtDocSearch = createStyledTextField(true);
        txtDocSearch.setPreferredSize(new Dimension(240, 32));
        txtDocSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterTable(doctorsTable, txtDocSearch.getText().trim());
            }
        });
        searchBar.add(lblSearch);
        searchBar.add(txtDocSearch);
        tableCard.add(searchBar, BorderLayout.NORTH);

        String[] cols = {"Doctor ID", "Doctor Name", "Specialization", "Contact Phone"};
        doctorsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        doctorsTable = new JTable(doctorsModel);
        styleTable(doctorsTable);

        doctorsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int r = doctorsTable.getSelectedRow();
                if (r != -1) {
                    int mr = doctorsTable.convertRowIndexToModel(r);
                    txtDocId.setText(doctorsModel.getValueAt(mr, 0).toString());
                    txtDocName.setText(String.valueOf(doctorsModel.getValueAt(mr, 1)));
                    txtDocSpec.setText(String.valueOf(doctorsModel.getValueAt(mr, 2)));
                    txtDocPhone.setText(doctorsModel.getValueAt(mr, 3) == null ? "" : doctorsModel.getValueAt(mr, 3).toString());
                }
            }
        });

        tableCard.add(new JScrollPane(doctorsTable), BorderLayout.CENTER);

        panel.add(formCard, BorderLayout.WEST);
        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    private void loadDoctors() {
        doctorsModel.setRowCount(0);
        String sql = "SELECT * FROM Doctor ORDER BY doctor_id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                doctorsModel.addRow(new Object[]{
                        rs.getInt("doctor_id"),
                        rs.getString("name"),
                        rs.getString("specialization"),
                        rs.getString("phone")
                });
            }
        } catch (SQLException ex) {
            showError("Failed to load doctors: " + ex.getMessage());
        }
        updateKpis();
    }

    private void addDoctor() {
        String name = txtDocName.getText().trim();
        String spec = txtDocSpec.getText().trim();
        String phone = txtDocPhone.getText().trim();

        if (name.isEmpty() || spec.isEmpty()) {
            showWarning("Doctor Name and Specialization are required.");
            return;
        }

        String sql = "INSERT INTO Doctor (name, specialization, phone) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, spec);
            ps.setString(3, phone.isEmpty() ? null : phone);
            ps.executeUpdate();

            showInfo("Doctor added successfully!");
            clearDoctorForm();
            loadDoctors();
            refreshPrescriptionDropdowns();
        } catch (Exception ex) {
            showError("Error adding doctor: " + ex.getMessage());
        }
    }

    private void updateDoctor() {
        String idStr = txtDocId.getText().trim();
        if (idStr.isEmpty()) {
            showWarning("Select a doctor to update.");
            return;
        }

        String sql = "UPDATE Doctor SET name = ?, specialization = ?, phone = ? WHERE doctor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txtDocName.getText().trim());
            ps.setString(2, txtDocSpec.getText().trim());
            ps.setString(3, txtDocPhone.getText().trim());
            ps.setInt(4, Integer.parseInt(idStr));
            ps.executeUpdate();

            showInfo("Doctor updated successfully!");
            clearDoctorForm();
            loadDoctors();
            refreshPrescriptionDropdowns();
        } catch (Exception ex) {
            showError("Error updating doctor: " + ex.getMessage());
        }
    }

    private void deleteDoctor() {
        String idStr = txtDocId.getText().trim();
        if (idStr.isEmpty()) {
            showWarning("Select a doctor to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete doctor #" + idStr + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM Doctor WHERE doctor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idStr));
            ps.executeUpdate();

            showInfo("Doctor deleted.");
            clearDoctorForm();
            loadDoctors();
            refreshPrescriptionDropdowns();
        } catch (Exception ex) {
            showError("Error deleting doctor: " + ex.getMessage());
        }
    }

    private void clearDoctorForm() {
        txtDocId.setText("");
        txtDocName.setText("");
        txtDocSpec.setText("");
        txtDocPhone.setText("");
    }

    // =========================================================================
    // 3. MEDICINES TAB
    // =========================================================================
    private JPanel createMedicinesPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Form Card
        JPanel formCard = new JPanel(new BorderLayout());
        formCard.setBackground(CARD_BG);
        formCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
        formCard.setPreferredSize(new Dimension(340, 0));

        JLabel formTitle = new JLabel("Medicine Inventory");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formTitle.setForeground(new Color(15, 23, 42));
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setOpaque(false);
        formBody.setBorder(new EmptyBorder(14, 0, 14, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        txtMedId = createStyledTextField(false);
        txtMedName = createStyledTextField(true);
        txtMedBrand = createStyledTextField(true);
        txtMedExpiry = createStyledTextField(true);
        txtMedStock = createStyledTextField(true);
        txtMedPrice = createStyledTextField(true);

        int row = 0;
        addFormRow(formBody, gbc, "Medicine ID (Auto):", txtMedId, row++);
        addFormRow(formBody, gbc, "Medicine Name *:", txtMedName, row++);
        addFormRow(formBody, gbc, "Brand / Maker:", txtMedBrand, row++);
        addFormRow(formBody, gbc, "Expiry (YYYY-MM-DD) *:", txtMedExpiry, row++);
        addFormRow(formBody, gbc, "Stock Quantity:", txtMedStock, row++);
        addFormRow(formBody, gbc, "Unit Price (₹) *:", txtMedPrice, row++);

        // Quick Stock Adjust Buttons
        JPanel adjustPanel = new JPanel(new GridLayout(1, 2, 8, 8));
        adjustPanel.setOpaque(false);
        JButton btnAddStock = createStyledButton("➕ +10 Stock", new Color(13, 148, 136), Color.WHITE);
        JButton btnMinusStock = createStyledButton("➖ -5 Stock", new Color(234, 88, 12), Color.WHITE);
        btnAddStock.addActionListener(e -> adjustStock(10));
        btnMinusStock.addActionListener(e -> adjustStock(-5));
        adjustPanel.add(btnAddStock);
        adjustPanel.add(btnMinusStock);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formBody.add(adjustPanel, gbc);

        // Actions
        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        btnGrid.setOpaque(false);
        JButton btnAdd = createStyledButton("Add Med", PRIMARY_COLOR, Color.WHITE);
        JButton btnUpdate = createStyledButton("Update", new Color(79, 70, 229), Color.WHITE);
        JButton btnDelete = createStyledButton("Delete", DANGER_COLOR, Color.WHITE);
        JButton btnClear = createStyledButton("Clear", new Color(241, 245, 249), new Color(51, 65, 85));

        btnAdd.addActionListener(e -> addMedicine());
        btnUpdate.addActionListener(e -> updateMedicine());
        btnDelete.addActionListener(e -> deleteMedicine());
        btnClear.addActionListener(e -> clearMedicineForm());

        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formBody.add(btnGrid, gbc);

        gbc.gridy = row++; gbc.weighty = 1.0;
        formBody.add(new JLabel(""), gbc);

        formCard.add(formBody, BorderLayout.CENTER);

        // Table Card
        JPanel tableCard = new JPanel(new BorderLayout(8, 8));
        tableCard.setBackground(CARD_BG);
        tableCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setOpaque(false);
        JLabel lblSearch = new JLabel("🔍 Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtMedSearch = createStyledTextField(true);
        txtMedSearch.setPreferredSize(new Dimension(200, 32));
        txtMedSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterTable(medicinesTable, txtMedSearch.getText().trim());
            }
        });

        JButton btnShowLowStock = createStyledButton("⚠️ Low Stock (<60)", new Color(254, 243, 199), new Color(180, 83, 9));
        btnShowLowStock.addActionListener(e -> filterLowStockMedicines());

        JButton btnResetFilter = createStyledButton("Show All", new Color(241, 245, 249), new Color(51, 65, 85));
        btnResetFilter.addActionListener(e -> loadMedicines());

        searchBar.add(lblSearch);
        searchBar.add(txtMedSearch);
        searchBar.add(btnShowLowStock);
        searchBar.add(btnResetFilter);
        tableCard.add(searchBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Medicine Name", "Brand", "Expiry Date", "Stock Qty", "Unit Price (₹)"};
        medicinesModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        medicinesTable = new JTable(medicinesModel);
        styleTable(medicinesTable);

        // Custom renderer for low stock highlighting
        medicinesTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
                try {
                    int stock = Integer.parseInt(val.toString());
                    if (stock < 60) {
                        setText("⚠️ " + stock + " (Low)");
                        setForeground(new Color(180, 83, 9));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(new Color(22, 163, 74));
                    }
                } catch (Exception ignored) {}
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        medicinesTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int r = medicinesTable.getSelectedRow();
                if (r != -1) {
                    int mr = medicinesTable.convertRowIndexToModel(r);
                    txtMedId.setText(medicinesModel.getValueAt(mr, 0).toString());
                    txtMedName.setText(String.valueOf(medicinesModel.getValueAt(mr, 1)));
                    txtMedBrand.setText(medicinesModel.getValueAt(mr, 2) == null ? "" : medicinesModel.getValueAt(mr, 2).toString());
                    txtMedExpiry.setText(String.valueOf(medicinesModel.getValueAt(mr, 3)));
                    txtMedStock.setText(String.valueOf(medicinesModel.getValueAt(mr, 4)));
                    txtMedPrice.setText(String.valueOf(medicinesModel.getValueAt(mr, 5)));
                }
            }
        });

        tableCard.add(new JScrollPane(medicinesTable), BorderLayout.CENTER);

        panel.add(formCard, BorderLayout.WEST);
        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    private void loadMedicines() {
        medicinesModel.setRowCount(0);
        String sql = "SELECT * FROM Medicine ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                medicinesModel.addRow(new Object[]{
                        rs.getInt("medicine_id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getDate("expiry_date"),
                        rs.getInt("stock_quantity"),
                        rs.getBigDecimal("unit_price")
                });
            }
        } catch (SQLException ex) {
            showError("Failed to load medicines: " + ex.getMessage());
        }
        updateKpis();
    }

    private void filterLowStockMedicines() {
        medicinesModel.setRowCount(0);
        String sql = "SELECT * FROM Medicine WHERE stock_quantity < 60 ORDER BY stock_quantity ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                medicinesModel.addRow(new Object[]{
                        rs.getInt("medicine_id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getDate("expiry_date"),
                        rs.getInt("stock_quantity"),
                        rs.getBigDecimal("unit_price")
                });
            }
        } catch (SQLException ex) {
            showError("Failed to load low stock: " + ex.getMessage());
        }
    }

    private void addMedicine() {
        String name = txtMedName.getText().trim();
        String brand = txtMedBrand.getText().trim();
        String expiry = txtMedExpiry.getText().trim();
        String stockStr = txtMedStock.getText().trim();
        String priceStr = txtMedPrice.getText().trim();

        if (name.isEmpty() || expiry.isEmpty() || priceStr.isEmpty()) {
            showWarning("Medicine Name, Expiry Date, and Unit Price are required.");
            return;
        }

        String sql = "INSERT INTO Medicine (name, brand, expiry_date, stock_quantity, unit_price) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, brand.isEmpty() ? null : brand);
            ps.setDate(3, Date.valueOf(expiry));
            ps.setInt(4, stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr));
            ps.setDouble(5, Double.parseDouble(priceStr));
            ps.executeUpdate();

            showInfo("Medicine added to inventory!");
            clearMedicineForm();
            loadMedicines();
            refreshPrescriptionDropdowns();
        } catch (Exception ex) {
            showError("Error adding medicine: " + ex.getMessage());
        }
    }

    private void updateMedicine() {
        String idStr = txtMedId.getText().trim();
        if (idStr.isEmpty()) {
            showWarning("Select a medicine from the table to update.");
            return;
        }

        String sql = "UPDATE Medicine SET name = ?, brand = ?, expiry_date = ?, stock_quantity = ?, unit_price = ? WHERE medicine_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txtMedName.getText().trim());
            ps.setString(2, txtMedBrand.getText().trim());
            ps.setDate(3, Date.valueOf(txtMedExpiry.getText().trim()));
            ps.setInt(4, Integer.parseInt(txtMedStock.getText().trim()));
            ps.setDouble(5, Double.parseDouble(txtMedPrice.getText().trim()));
            ps.setInt(6, Integer.parseInt(idStr));
            ps.executeUpdate();

            showInfo("Medicine updated successfully!");
            clearMedicineForm();
            loadMedicines();
            refreshPrescriptionDropdowns();
        } catch (Exception ex) {
            showError("Error updating medicine: " + ex.getMessage());
        }
    }

    private void adjustStock(int delta) {
        String idStr = txtMedId.getText().trim();
        if (idStr.isEmpty()) {
            showWarning("Select a medicine first to adjust stock.");
            return;
        }

        String sql = "UPDATE Medicine SET stock_quantity = GREATEST(0, stock_quantity + ?) WHERE medicine_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, Integer.parseInt(idStr));
            ps.executeUpdate();

            showInfo("Stock adjusted by " + (delta > 0 ? "+" : "") + delta);
            loadMedicines();
        } catch (Exception ex) {
            showError("Error adjusting stock: " + ex.getMessage());
        }
    }

    private void deleteMedicine() {
        String idStr = txtMedId.getText().trim();
        if (idStr.isEmpty()) {
            showWarning("Select a medicine to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete medicine #" + idStr + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM Medicine WHERE medicine_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idStr));
            ps.executeUpdate();

            showInfo("Medicine deleted.");
            clearMedicineForm();
            loadMedicines();
            refreshPrescriptionDropdowns();
        } catch (Exception ex) {
            showError("Error deleting medicine: " + ex.getMessage());
        }
    }

    private void clearMedicineForm() {
        txtMedId.setText("");
        txtMedName.setText("");
        txtMedBrand.setText("");
        txtMedExpiry.setText("");
        txtMedStock.setText("");
        txtMedPrice.setText("");
    }

    // =========================================================================
    // 4. PRESCRIPTIONS TAB
    // =========================================================================
    private JPanel createPrescriptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Form Card
        JPanel formCard = new JPanel(new BorderLayout());
        formCard.setBackground(CARD_BG);
        formCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
        formCard.setPreferredSize(new Dimension(360, 0));

        JLabel formTitle = new JLabel("Issue & Manage Prescription");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formTitle.setForeground(new Color(15, 23, 42));
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setOpaque(false);
        formBody.setBorder(new EmptyBorder(14, 0, 14, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        txtPrescId = createStyledTextField(false);
        cbPrescUser = new JComboBox<>();
        cbPrescDoctor = new JComboBox<>();
        cbPrescMedicine = new JComboBox<>();
        txtPrescDate = createStyledTextField(true);
        txtPrescDate.setText(LocalDate.now().toString());
        txtPrescDosage = createStyledTextField(true);
        txtPrescDuration = createStyledTextField(true);
        txtPrescDuration.setText("5");
        cbPrescStatus = new JComboBox<>(new String[]{"Active", "Completed", "Cancelled"});

        int row = 0;
        addFormRow(formBody, gbc, "Prescription ID:", txtPrescId, row++);
        addFormRow(formBody, gbc, "Select Patient *:", cbPrescUser, row++);
        addFormRow(formBody, gbc, "Doctor *:", cbPrescDoctor, row++);
        addFormRow(formBody, gbc, "Medicine *:", cbPrescMedicine, row++);
        addFormRow(formBody, gbc, "Date Issued:", txtPrescDate, row++);
        addFormRow(formBody, gbc, "Dosage *:", txtPrescDosage, row++);
        addFormRow(formBody, gbc, "Duration (Days) *:", txtPrescDuration, row++);
        addFormRow(formBody, gbc, "Status:", cbPrescStatus, row++);

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        btnGrid.setOpaque(false);
        JButton btnAdd = createStyledButton("Issue Presc", PRIMARY_COLOR, Color.WHITE);
        JButton btnUpdate = createStyledButton("Update Status", new Color(79, 70, 229), Color.WHITE);
        JButton btnDelete = createStyledButton("Delete", DANGER_COLOR, Color.WHITE);
        JButton btnClear = createStyledButton("Clear", new Color(241, 245, 249), new Color(51, 65, 85));

        btnAdd.addActionListener(e -> addPrescription());
        btnUpdate.addActionListener(e -> updatePrescriptionStatus());
        btnDelete.addActionListener(e -> deletePrescription());
        btnClear.addActionListener(e -> clearPrescriptionForm());

        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formBody.add(btnGrid, gbc);

        gbc.gridy = row++; gbc.weighty = 1.0;
        formBody.add(new JLabel(""), gbc);

        formCard.add(formBody, BorderLayout.CENTER);

        // Table Card
        JPanel tableCard = new JPanel(new BorderLayout(8, 8));
        tableCard.setBackground(CARD_BG);
        tableCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        String[] cols = {"ID", "Patient", "Doctor", "Medicine", "Issued Date", "Dosage", "Days", "Status"};
        prescriptionsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        prescriptionsTable = new JTable(prescriptionsModel);
        styleTable(prescriptionsTable);

        // Status Column Custom Badge Renderer
        prescriptionsTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
                String s = val == null ? "" : val.toString();
                if ("Active".equalsIgnoreCase(s)) {
                    setForeground(new Color(22, 163, 74));
                } else if ("Completed".equalsIgnoreCase(s)) {
                    setForeground(new Color(2, 132, 199));
                } else {
                    setForeground(new Color(220, 38, 38));
                }
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        prescriptionsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int r = prescriptionsTable.getSelectedRow();
                if (r != -1) {
                    int mr = prescriptionsTable.convertRowIndexToModel(r);
                    txtPrescId.setText(prescriptionsModel.getValueAt(mr, 0).toString());
                    txtPrescDate.setText(String.valueOf(prescriptionsModel.getValueAt(mr, 4)));
                    txtPrescDosage.setText(String.valueOf(prescriptionsModel.getValueAt(mr, 5)));
                    txtPrescDuration.setText(String.valueOf(prescriptionsModel.getValueAt(mr, 6)));
                    cbPrescStatus.setSelectedItem(String.valueOf(prescriptionsModel.getValueAt(mr, 7)));
                }
            }
        });

        tableCard.add(new JScrollPane(prescriptionsTable), BorderLayout.CENTER);

        panel.add(formCard, BorderLayout.WEST);
        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    private void loadPrescriptions() {
        prescriptionsModel.setRowCount(0);
        String sql = "SELECT p.prescription_id, u.name AS user_name, d.name AS doctor_name, " +
                "m.name AS medicine_name, p.date_issued, p.dosage, p.duration_days, p.status " +
                "FROM Prescription p " +
                "JOIN Users u ON p.user_id = u.user_id " +
                "JOIN Doctor d ON p.doctor_id = d.doctor_id " +
                "JOIN Medicine m ON p.medicine_id = m.medicine_id " +
                "ORDER BY p.prescription_id DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                prescriptionsModel.addRow(new Object[]{
                        rs.getInt("prescription_id"),
                        rs.getString("user_name"),
                        rs.getString("doctor_name"),
                        rs.getString("medicine_name"),
                        rs.getDate("date_issued"),
                        rs.getString("dosage"),
                        rs.getInt("duration_days"),
                        rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            showError("Failed to load prescriptions: " + ex.getMessage());
        }
        updateKpis();
    }

    private void refreshPrescriptionDropdowns() {
        cbPrescUser.removeAllItems();
        cbPrescDoctor.removeAllItems();
        cbPrescMedicine.removeAllItems();
        if (cbRemPrescription != null) cbRemPrescription.removeAllItems();

        try (Connection conn = DBConnection.getConnection()) {
            // Users
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT user_id, name, phone FROM Users ORDER BY name")) {
                while (rs.next()) {
                    cbPrescUser.addItem(new ComboItem(rs.getInt("user_id"), rs.getString("name") + " (" + rs.getString("phone") + ")"));
                }
            }
            // Doctors
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT doctor_id, name, specialization FROM Doctor ORDER BY name")) {
                while (rs.next()) {
                    cbPrescDoctor.addItem(new ComboItem(rs.getInt("doctor_id"), rs.getString("name") + " - " + rs.getString("specialization")));
                }
            }
            // Medicines
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT medicine_id, name, stock_quantity FROM Medicine ORDER BY name")) {
                while (rs.next()) {
                    cbPrescMedicine.addItem(new ComboItem(rs.getInt("medicine_id"), rs.getString("name") + " (Stock: " + rs.getInt("stock_quantity") + ")"));
                }
            }
            // Prescriptions for Reminders
            if (cbRemPrescription != null) {
                String pSql = "SELECT p.prescription_id, p.user_id, u.name AS u_name, m.name AS m_name " +
                        "FROM Prescription p JOIN Users u ON p.user_id = u.user_id JOIN Medicine m ON p.medicine_id = m.medicine_id";
                try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(pSql)) {
                    while (rs.next()) {
                        cbRemPrescription.addItem(new ComboItem(
                                rs.getInt("prescription_id"),
                                rs.getInt("user_id"),
                                "#" + rs.getInt("prescription_id") + " - " + rs.getString("u_name") + " (" + rs.getString("m_name") + ")"
                        ));
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error loading dropdown items: " + ex.getMessage());
        }
    }

    private void addPrescription() {
        ComboItem userItem = (ComboItem) cbPrescUser.getSelectedItem();
        ComboItem docItem = (ComboItem) cbPrescDoctor.getSelectedItem();
        ComboItem medItem = (ComboItem) cbPrescMedicine.getSelectedItem();

        if (userItem == null || docItem == null || medItem == null) {
            showWarning("Please select Patient, Doctor, and Medicine.");
            return;
        }

        String dosage = txtPrescDosage.getText().trim();
        String duration = txtPrescDuration.getText().trim();
        String dateStr = txtPrescDate.getText().trim();

        if (dosage.isEmpty() || duration.isEmpty()) {
            showWarning("Dosage and Duration are required.");
            return;
        }

        String sql = "INSERT INTO Prescription (user_id, doctor_id, medicine_id, date_issued, dosage, duration_days, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userItem.getId());
            ps.setInt(2, docItem.getId());
            ps.setInt(3, medItem.getId());
            ps.setDate(4, dateStr.isEmpty() ? Date.valueOf(LocalDate.now()) : Date.valueOf(dateStr));
            ps.setString(5, dosage);
            ps.setInt(6, Integer.parseInt(duration));
            ps.setString(7, (String) cbPrescStatus.getSelectedItem());
            ps.executeUpdate();

            showInfo("Prescription issued successfully!");
            clearPrescriptionForm();
            loadPrescriptions();
            refreshPrescriptionDropdowns();
        } catch (Exception ex) {
            showError("Error creating prescription: " + ex.getMessage());
        }
    }

    private void updatePrescriptionStatus() {
        String idStr = txtPrescId.getText().trim();
        if (idStr.isEmpty()) {
            showWarning("Select a prescription from the table to update.");
            return;
        }

        String sql = "UPDATE Prescription SET status = ? WHERE prescription_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (String) cbPrescStatus.getSelectedItem());
            ps.setInt(2, Integer.parseInt(idStr));
            ps.executeUpdate();

            showInfo("Prescription status updated!");
            loadPrescriptions();
        } catch (Exception ex) {
            showError("Error updating prescription: " + ex.getMessage());
        }
    }

    private void deletePrescription() {
        String idStr = txtPrescId.getText().trim();
        if (idStr.isEmpty()) {
            showWarning("Select a prescription to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete prescription #" + idStr + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM Prescription WHERE prescription_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idStr));
            ps.executeUpdate();

            showInfo("Prescription deleted.");
            clearPrescriptionForm();
            loadPrescriptions();
            refreshPrescriptionDropdowns();
        } catch (Exception ex) {
            showError("Error deleting prescription: " + ex.getMessage());
        }
    }

    private void clearPrescriptionForm() {
        txtPrescId.setText("");
        txtPrescDosage.setText("");
        txtPrescDuration.setText("5");
        txtPrescDate.setText(LocalDate.now().toString());
    }

    // =========================================================================
    // 5. REMINDERS TAB
    // =========================================================================
    private JPanel createRemindersPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Form Card
        JPanel formCard = new JPanel(new BorderLayout());
        formCard.setBackground(CARD_BG);
        formCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
        formCard.setPreferredSize(new Dimension(360, 0));

        JLabel formTitle = new JLabel("Dose Reminders & Schedule");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formTitle.setForeground(new Color(15, 23, 42));
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setOpaque(false);
        formBody.setBorder(new EmptyBorder(14, 0, 14, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        txtRemId = createStyledTextField(false);
        cbRemPrescription = new JComboBox<>();
        txtRemDateTime = createStyledTextField(true);
        txtRemDateTime.setText(LocalDateTime.now().plusHours(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00")));
        txtRemMessage = createStyledTextField(true);
        cbRemStatus = new JComboBox<>(new String[]{"Pending", "Sent", "Completed"});

        int row = 0;
        addFormRow(formBody, gbc, "Reminder ID:", txtRemId, row++);
        addFormRow(formBody, gbc, "Prescription / Patient *:", cbRemPrescription, row++);
        addFormRow(formBody, gbc, "Scheduled Datetime *:", txtRemDateTime, row++);
        addFormRow(formBody, gbc, "Message *:", txtRemMessage, row++);
        addFormRow(formBody, gbc, "Status:", cbRemStatus, row++);

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        btnGrid.setOpaque(false);
        JButton btnAdd = createStyledButton("Schedule", PRIMARY_COLOR, Color.WHITE);
        JButton btnComplete = createStyledButton("✔ Mark Done", SUCCESS_COLOR, Color.WHITE);
        JButton btnDelete = createStyledButton("Delete", DANGER_COLOR, Color.WHITE);
        JButton btnClear = createStyledButton("Clear", new Color(241, 245, 249), new Color(51, 65, 85));

        btnAdd.addActionListener(e -> addReminder());
        btnComplete.addActionListener(e -> markReminderCompleted());
        btnDelete.addActionListener(e -> deleteReminder());
        btnClear.addActionListener(e -> clearReminderForm());

        btnGrid.add(btnAdd);
        btnGrid.add(btnComplete);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formBody.add(btnGrid, gbc);

        gbc.gridy = row++; gbc.weighty = 1.0;
        formBody.add(new JLabel(""), gbc);

        formCard.add(formBody, BorderLayout.CENTER);

        // Table Card
        JPanel tableCard = new JPanel(new BorderLayout(8, 8));
        tableCard.setBackground(CARD_BG);
        tableCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        String[] cols = {"Reminder ID", "Patient", "Medicine", "Scheduled Time", "Message", "Status"};
        remindersModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        remindersTable = new JTable(remindersModel);
        styleTable(remindersTable);

        // Status Renderer
        remindersTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
                String s = val == null ? "" : val.toString();
                if ("Completed".equalsIgnoreCase(s)) {
                    setForeground(new Color(22, 163, 74));
                } else if ("Pending".equalsIgnoreCase(s)) {
                    setForeground(new Color(217, 119, 6));
                } else {
                    setForeground(new Color(2, 132, 199));
                }
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        remindersTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int r = remindersTable.getSelectedRow();
                if (r != -1) {
                    int mr = remindersTable.convertRowIndexToModel(r);
                    txtRemId.setText(remindersModel.getValueAt(mr, 0).toString());
                    txtRemDateTime.setText(String.valueOf(remindersModel.getValueAt(mr, 3)));
                    txtRemMessage.setText(String.valueOf(remindersModel.getValueAt(mr, 4)));
                    cbRemStatus.setSelectedItem(String.valueOf(remindersModel.getValueAt(mr, 5)));
                }
            }
        });

        tableCard.add(new JScrollPane(remindersTable), BorderLayout.CENTER);

        panel.add(formCard, BorderLayout.WEST);
        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    private void loadReminders() {
        remindersModel.setRowCount(0);
        String sql = "SELECT r.reminder_id, u.name AS user_name, m.name AS medicine_name, " +
                "r.reminder_datetime, r.message, r.status " +
                "FROM Reminder r " +
                "JOIN Users u ON r.user_id = u.user_id " +
                "JOIN Prescription p ON r.prescription_id = p.prescription_id " +
                "JOIN Medicine m ON p.medicine_id = m.medicine_id " +
                "ORDER BY r.reminder_datetime ASC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                remindersModel.addRow(new Object[]{
                        rs.getInt("reminder_id"),
                        rs.getString("user_name"),
                        rs.getString("medicine_name"),
                        rs.getTimestamp("reminder_datetime"),
                        rs.getString("message"),
                        rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            showError("Failed to load reminders: " + ex.getMessage());
        }
        updateKpis();
    }

    private void addReminder() {
        ComboItem item = (ComboItem) cbRemPrescription.getSelectedItem();
        if (item == null) {
            showWarning("Select a prescription.");
            return;
        }

        String dtStr = txtRemDateTime.getText().trim();
        String msg = txtRemMessage.getText().trim();

        if (dtStr.isEmpty() || msg.isEmpty()) {
            showWarning("Datetime and message are required.");
            return;
        }

        String sql = "INSERT INTO Reminder (user_id, prescription_id, reminder_datetime, message, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getUserId());
            ps.setInt(2, item.getId());
            ps.setTimestamp(3, Timestamp.valueOf(dtStr));
            ps.setString(4, msg);
            ps.setString(5, (String) cbRemStatus.getSelectedItem());
            ps.executeUpdate();

            showInfo("Dose reminder scheduled!");
            clearReminderForm();
            loadReminders();
        } catch (Exception ex) {
            showError("Error adding reminder: " + ex.getMessage());
        }
    }

    private void markReminderCompleted() {
        String idStr = txtRemId.getText().trim();
        if (idStr.isEmpty()) {
            showWarning("Select a reminder from the table to mark done.");
            return;
        }

        String sql = "UPDATE Reminder SET status = 'Completed' WHERE reminder_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idStr));
            ps.executeUpdate();

            showInfo("Reminder marked as Completed!");
            loadReminders();
        } catch (Exception ex) {
            showError("Error updating reminder: " + ex.getMessage());
        }
    }

    private void deleteReminder() {
        String idStr = txtRemId.getText().trim();
        if (idStr.isEmpty()) {
            showWarning("Select a reminder to delete.");
            return;
        }

        String sql = "DELETE FROM Reminder WHERE reminder_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idStr));
            ps.executeUpdate();

            showInfo("Reminder deleted.");
            clearReminderForm();
            loadReminders();
        } catch (Exception ex) {
            showError("Error deleting reminder: " + ex.getMessage());
        }
    }

    private void clearReminderForm() {
        txtRemId.setText("");
        txtRemMessage.setText("");
    }

    // =========================================================================
    // 6. REPORTS & SQL QUERIES TAB
    // =========================================================================
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topBar.setOpaque(false);

        JButton btnRefreshReports = createStyledButton("🔄 Execute & Refresh SQL Analytics", PRIMARY_COLOR, Color.WHITE);
        btnRefreshReports.setPreferredSize(new Dimension(280, 36));
        btnRefreshReports.addActionListener(e -> generateReports());
        topBar.add(btnRefreshReports);

        panel.add(topBar, BorderLayout.NORTH);

        txtReportsOutput = new JTextArea();
        txtReportsOutput.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtReportsOutput.setBackground(CARD_BG);
        txtReportsOutput.setEditable(false);
        txtReportsOutput.setMargin(new Insets(16, 20, 16, 20));
        txtReportsOutput.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        panel.add(new JScrollPane(txtReportsOutput), BorderLayout.CENTER);
        return panel;
    }

    private void generateReports() {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================================================================\n");
        sb.append("                               MEDICINE MANAGEMENT SQL ANALYTICS REPORT                             \n");
        sb.append("====================================================================================================\n\n");

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Stock & Price Aggregates
            sb.append("1. MEDICINE INVENTORY TOTALS & PRICE AGGREGATES:\n");
            sb.append("----------------------------------------------------------------------------------------------------\n");
            String q1 = "SELECT SUM(stock_quantity) AS Total_Stock, AVG(unit_price) AS Average_Price, " +
                    "MAX(unit_price) AS Highest_Price, MIN(unit_price) AS Lowest_Price FROM Medicine";
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(q1)) {
                if (rs.next()) {
                    sb.append(String.format("   • Total Stock Units Available : %d units\n", rs.getInt("Total_Stock")));
                    sb.append(String.format("   • Average Unit Price          : ₹%.2f\n", rs.getDouble("Average_Price")));
                    sb.append(String.format("   • Highest Medicine Price      : ₹%.2f\n", rs.getDouble("Highest_Price")));
                    sb.append(String.format("   • Lowest Medicine Price       : ₹%.2f\n", rs.getDouble("Lowest_Price")));
                }
            }
            sb.append("\n");

            // 2. Most Prescribed Medicines
            sb.append("2. TIMES EACH MEDICINE HAS BEEN PRESCRIBED:\n");
            sb.append("----------------------------------------------------------------------------------------------------\n");
            String q2 = "SELECT M.name, COUNT(P.prescription_id) AS times_prescribed FROM Medicine M " +
                    "LEFT JOIN Prescription P ON M.medicine_id = P.medicine_id GROUP BY M.medicine_id, M.name " +
                    "ORDER BY times_prescribed DESC";
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(q2)) {
                while (rs.next()) {
                    sb.append(String.format("   • %-25s : %d times prescribed\n",
                            rs.getString("name"), rs.getInt("times_prescribed")));
                }
            }
            sb.append("\n");

            // 3. Prescriptions Per Patient
            sb.append("3. TOTAL PRESCRIPTIONS PER PATIENT:\n");
            sb.append("----------------------------------------------------------------------------------------------------\n");
            String q3 = "SELECT U.name, COUNT(P.prescription_id) AS Total_Prescriptions FROM Users U " +
                    "LEFT JOIN Prescription P ON U.user_id = P.user_id GROUP BY U.user_id, U.name " +
                    "ORDER BY Total_Prescriptions DESC";
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(q3)) {
                while (rs.next()) {
                    sb.append(String.format("   • %-25s : %d prescriptions issued\n",
                            rs.getString("name"), rs.getInt("Total_Prescriptions")));
                }
            }
            sb.append("\n");

            // 4. Low Stock Warning
            sb.append("4. LOW STOCK ALERT (stock_quantity < 60):\n");
            sb.append("----------------------------------------------------------------------------------------------------\n");
            String q4 = "SELECT name, brand, stock_quantity, unit_price FROM Medicine WHERE stock_quantity < 60";
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(q4)) {
                boolean hasLow = false;
                while (rs.next()) {
                    hasLow = true;
                    sb.append(String.format("   [WARNING] %s (%s) - Only %d units left! Unit Price: ₹%.2f\n",
                            rs.getString("name"), rs.getString("brand"), rs.getInt("stock_quantity"), rs.getDouble("unit_price")));
                }
                if (!hasLow) sb.append("   All medicines have adequate stock (>60 units).\n");
            }
            sb.append("\n");

            // 5. Active Prescriptions Summary
            sb.append("5. ACTIVE PRESCRIPTIONS DETAILS:\n");
            sb.append("----------------------------------------------------------------------------------------------------\n");
            String q5 = "SELECT U.name AS user_name, D.name AS doctor_name, M.name AS medicine_name, " +
                    "P.dosage, P.duration_days FROM Prescription P " +
                    "JOIN Users U ON P.user_id = U.user_id " +
                    "JOIN Doctor D ON P.doctor_id = D.doctor_id " +
                    "JOIN Medicine M ON P.medicine_id = M.medicine_id " +
                    "WHERE P.status = 'Active'";
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(q5)) {
                while (rs.next()) {
                    sb.append(String.format("   • Patient: %-15s | Doctor: %-15s | Med: %-12s | Dose: %s (%d days)\n",
                            rs.getString("user_name"), rs.getString("doctor_name"), rs.getString("medicine_name"),
                            rs.getString("dosage"), rs.getInt("duration_days")));
                }
            }
            sb.append("\n====================================================================================================\n");

        } catch (SQLException ex) {
            sb.append("Error generating reports: ").append(ex.getMessage()).append("\n");
        }

        txtReportsOutput.setText(sb.toString());
        txtReportsOutput.setCaretPosition(0);
    }

    // =========================================================================
    // UI STYLING & HELPERS
    // =========================================================================
    private JTextField createStyledTextField(boolean editable) {
        JTextField tf = new JTextField();
        tf.setEditable(editable);
        tf.setPreferredSize(new Dimension(0, 32));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.putClientProperty("JComponent.roundRect", true);
        if (!editable) {
            tf.setBackground(new Color(241, 245, 249));
        }
        return tf;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setPreferredSize(new Dimension(0, 34));
        return btn;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.setGridColor(BORDER_COLOR);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(71, 85, 105));
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(field, gbc);
    }

    private void filterTable(JTable table, String text) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        if (text.isEmpty()) {
            table.setRowSorter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private void updateKpis() {
        try (Connection conn = DBConnection.getConnection()) {
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Users")) {
                if (rs.next()) lblKpiUsers.setText(String.valueOf(rs.getInt(1)));
            }
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Doctor")) {
                if (rs.next()) lblKpiDocs.setText(String.valueOf(rs.getInt(1)));
            }
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Medicine")) {
                if (rs.next()) lblKpiMeds.setText(String.valueOf(rs.getInt(1)));
            }
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Prescription WHERE status = 'Active'")) {
                if (rs.next()) lblKpiPresc.setText(String.valueOf(rs.getInt(1)));
            }
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Medicine WHERE stock_quantity < 60")) {
                if (rs.next()) lblKpiLowStock.setText(String.valueOf(rs.getInt(1)));
            }
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Reminder WHERE status = 'Pending'")) {
                if (rs.next()) lblKpiReminders.setText(String.valueOf(rs.getInt(1)));
            }
        } catch (SQLException ignored) {}
    }

    private void loadAllData() {
        loadUsers();
        loadDoctors();
        loadMedicines();
        loadPrescriptions();
        loadReminders();
        refreshPrescriptionDropdowns();
        generateReports();
        updateKpis();
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ComboBox item helper
    public static class ComboItem {
        private final int id;
        private int userId;
        private final String label;

        public ComboItem(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public ComboItem(int id, int userId, String label) {
            this.id = id;
            this.userId = userId;
            this.label = label;
        }

        public int getId() { return id; }
        public int getUserId() { return userId; }

        @Override
        public String toString() { return label; }
    }

    public static void main(String[] args) {
        // Setup modern FlatLaf Look and Feel
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabHeight", 38);
        UIManager.put("TabbedPane.selectedBackground", Color.WHITE);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("TableHeader.height", 34);
        UIManager.put("Table.alternateRowColor", new Color(248, 250, 252));
        UIManager.put("Table.gridColor", new Color(226, 232, 240));

        SwingUtilities.invokeLater(() -> {
            MedicineApp app = new MedicineApp();
            app.setVisible(true);
        });
    }
}

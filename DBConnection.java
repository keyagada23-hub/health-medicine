import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Cloud TiDB MySQL Connection (Free 24/7 Cloud Database)
    private static final String CLOUD_URL = "jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/medicine_management?sslMode=REQUIRED";
    private static final String CLOUD_USER = "3aUx4WGLtufkhHu.root";
    private static final String CLOUD_PASSWORD = "HvpuJSuNr0ntrKVI";

    // Local Fallback Connection
    private static final String LOCAL_URL = "jdbc:mysql://localhost:3306/medicine_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String LOCAL_USER = "root";
    private static final String LOCAL_PASSWORD = "@1Keya2304";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found: " + e.getMessage());
        }

        try {
            // Try 24/7 Cloud TiDB database first
            return DriverManager.getConnection(CLOUD_URL, CLOUD_USER, CLOUD_PASSWORD);
        } catch (SQLException e) {
            // Fallback to local MySQL if offline
            return DriverManager.getConnection(LOCAL_URL, LOCAL_USER, LOCAL_PASSWORD);
        }
    }
}

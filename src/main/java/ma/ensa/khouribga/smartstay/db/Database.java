package ma.ensa.khouribga.smartstay.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {

    private static final String PROPS_FILE = "application.properties";

    private static String url;
    private static String username;
    private static String password;
    private static String driver;

    static {
        loadProperties();
        loadDriver();
    }

    private Database() {
        // Utility class
    }

    private static void loadProperties() {
        try (InputStream input = Database.class.getClassLoader().getResourceAsStream(PROPS_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Could not find " + PROPS_FILE + " in classpath.");
            }

            Properties props = new Properties();
            props.load(input);

            url = props.getProperty("spring.datasource.url");
            username = props.getProperty("spring.datasource.username");
            password = props.getProperty("spring.datasource.password");
            driver = props.getProperty("spring.datasource.driver-class-name");

            if (isBlank(url) || isBlank(username) || isBlank(password) || isBlank(driver)) {
                throw new IllegalStateException(
                        "Missing DB config keys in application.properties: " +
                        "spring.datasource.url, spring.datasource.username, " +
                        "spring.datasource.password, spring.datasource.driver-class-name"
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load database configuration.", e);
        }
    }

    private static void loadDriver() {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC driver not found: " + driver, e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public static boolean testConnection() {
        try (Connection ignored = getConnection()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

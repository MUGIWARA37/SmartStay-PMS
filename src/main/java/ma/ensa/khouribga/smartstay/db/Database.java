package ma.ensa.khouribga.smartstay.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {
    private static final String PROPS_FILE = "application.properties";

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;
    private static final int MAX_RETRIES;
    private static final long RETRY_DELAY_MS;

    static {
        Properties props = new Properties();
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream(PROPS_FILE)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + PROPS_FILE + " in src/main/resources");
            }
            props.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load " + PROPS_FILE + ": " + e.getMessage());
        }

        URL = require(props, "db.url");
        USER = require(props, "db.user");
        PASSWORD = props.getProperty("db.password", "");

        MAX_RETRIES = parseIntOrDefault(props.getProperty("db.pool.maxRetries"), 3);
        RETRY_DELAY_MS = parseLongOrDefault(props.getProperty("db.pool.retryDelayMs"), 1200L);
    }

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        SQLException last = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (SQLException ex) {
                last = ex;
                if (attempt < MAX_RETRIES) {
                    sleepQuietly(RETRY_DELAY_MS);
                }
            }
        }

        throw new SQLException(
                "Unable to connect to database after " + MAX_RETRIES + " attempts. URL=" + URL + ", USER=" + USER,
                last
        );
    }

    private static String require(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return v.trim();
    }

    private static int parseIntOrDefault(String value, int def) {
        if (value == null || value.isBlank()) return def;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static long parseLongOrDefault(String value, long def) {
        if (value == null || value.isBlank()) return def;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
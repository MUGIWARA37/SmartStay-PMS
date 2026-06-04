package ma.ensa.khouribga.smartstay.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Properties;

public final class DatabaseInitializer {
    private static final String PROPS_FILE = "application.properties";
    private static final String SCHEMA_RESOURCE = "/sql/schema.sql";
    private static final String SEED_RESOURCE = "/sql/seed.sql";
    private static final String MINIMAL_SEED_RESOURCE = "/sql/seed_minimal.sql";
    private static final String SEED_MODE_PROPERTY = "db.seed.mode";
    private static final String SEED_MODE_FULL = "full";
    private static final String SEED_MODE_MINIMAL = "minimal";
    private static final String SEED_MODE_NONE = "none";
    private static final String IMAGE_DOUBLE = "/images/rooms/double.jpg";
    private static final String IMAGE_KING = "/images/rooms/king.jpg";
    private static final String IMAGE_TWIN = "/images/rooms/twin.jpg";
    private static final String IMAGE_TWIN_KING = "/images/rooms/twin_king.jpg";
    private static final String IMAGE_PRESIDENTIAL = "/images/rooms/presedantiel.jpg";
    private static volatile boolean initialized = false;

    private DatabaseInitializer() {
    }

    public static void ensureInitialized() throws SQLException {
        if (initialized) return;
        synchronized (DatabaseInitializer.class) {
            if (initialized) return;
            try (Connection conn = Database.getConnection()) {
                int tableCount = countTables(conn);
                if (tableCount == 0) {
                    String seedMode = getSeedMode();
                    System.err.println("[DatabaseInitializer] Database empty; applying schema.");
                    runScript(conn, SCHEMA_RESOURCE);
                    if (!SEED_MODE_NONE.equals(seedMode)) {
                        String seedResource = SEED_MODE_FULL.equals(seedMode) ? SEED_RESOURCE : MINIMAL_SEED_RESOURCE;
                        System.err.println("[DatabaseInitializer] Seeding database (" + seedMode + ").");
                        runScript(conn, seedResource);
                    } else {
                        System.err.println("[DatabaseInitializer] Seeding disabled (db.seed.mode=none).");
                    }
                    ensureRoomImages(conn);
                } else if (!tableExists(conn, "rooms") || !tableExists(conn, "room_types")) {
                    throw new SQLException(
                        "Database schema is missing required tables (rooms/room_types). " +
                        "If using Docker, reset the DB volume: docker compose down -v && docker compose up -d"
                    );
                } else {
                    ensureRoomImages(conn);
                }
                initialized = true;
            }
        }
    }

    private static int countTables(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static boolean tableExists(Connection conn, String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static void runScript(Connection conn, String resourcePath) throws SQLException {
        InputStream in = DatabaseInitializer.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new SQLException("Missing SQL resource: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder statement = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("#") || trimmed.startsWith("/*")) {
                    continue;
                }
                statement.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    executeStatement(conn, resourcePath, statement.toString());
                    statement.setLength(0);
                }
            }
            if (!statement.isEmpty()) {
                executeStatement(conn, resourcePath, statement.toString());
            }
        } catch (IOException e) {
            throw new SQLException("Failed to read SQL resource: " + resourcePath, e);
        }
    }

    private static void executeStatement(Connection conn, String resourcePath, String sql) throws SQLException {
        String cleaned = sql.trim();
        if (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        if (cleaned.isEmpty()) return;
        if (SEED_RESOURCE.equals(resourcePath)) {
            cleaned = normalizeSeedStatement(cleaned);
        }
        try (Statement st = conn.createStatement()) {
            st.execute(cleaned);
        }
    }

    private static String getSeedMode() {
        Properties props = new Properties();
        try (InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream(PROPS_FILE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("[DatabaseInitializer] Unable to read " + PROPS_FILE + ": " + e.getMessage());
        }
        String mode = props.getProperty(SEED_MODE_PROPERTY, SEED_MODE_MINIMAL);
        return mode.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSeedStatement(String sql) {
        return sql.replace("SELECT id FROM guests WHERE email", "SELECT MIN(id) FROM guests WHERE email");
    }

    private static void ensureRoomImages(Connection conn) throws SQLException {
        if (!tableExists(conn, "room_images")) return;
        String allowedList = "'" + IMAGE_DOUBLE + "','" + IMAGE_KING + "','" + IMAGE_TWIN + "','"
                + IMAGE_TWIN_KING + "','" + IMAGE_PRESIDENTIAL + "'";
        String updateSql = """
                UPDATE room_images ri
                JOIN rooms r ON ri.room_id = r.id
                JOIN room_types rt ON r.room_type_id = rt.id
                SET ri.image_path = CASE
                    WHEN LOWER(rt.name) = 'double' THEN '%s'
                    WHEN LOWER(rt.name) = 'twin' THEN '%s'
                    WHEN LOWER(rt.name) = 'deluxe' THEN '%s'
                    WHEN LOWER(rt.name) = 'suite' THEN '%s'
                    WHEN LOWER(rt.name) = 'presidential' THEN '%s'
                    ELSE '%s'
                END
                WHERE ri.is_primary = 1
                  AND (ri.image_path IS NULL OR ri.image_path NOT IN (%s))
                """.formatted(IMAGE_DOUBLE, IMAGE_TWIN, IMAGE_TWIN_KING, IMAGE_KING, IMAGE_PRESIDENTIAL, IMAGE_KING, allowedList);
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(updateSql);
        }

        String insertSql = """
                INSERT INTO room_images (room_id, image_path, is_primary, sort_order)
                SELECT r.id,
                       CASE
                           WHEN LOWER(rt.name) = 'double' THEN '%s'
                           WHEN LOWER(rt.name) = 'twin' THEN '%s'
                           WHEN LOWER(rt.name) = 'deluxe' THEN '%s'
                           WHEN LOWER(rt.name) = 'suite' THEN '%s'
                           WHEN LOWER(rt.name) = 'presidential' THEN '%s'
                           ELSE '%s'
                       END,
                       1, 1
                FROM rooms r
                JOIN room_types rt ON r.room_type_id = rt.id
                LEFT JOIN room_images ri ON ri.room_id = r.id AND ri.is_primary = 1
                WHERE ri.id IS NULL
                """.formatted(IMAGE_DOUBLE, IMAGE_TWIN, IMAGE_TWIN_KING, IMAGE_KING, IMAGE_PRESIDENTIAL, IMAGE_KING);
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(insertSql);
        }
    }
}

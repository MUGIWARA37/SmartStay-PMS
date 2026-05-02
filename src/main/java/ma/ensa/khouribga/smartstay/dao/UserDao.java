package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.User;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access operations for the {@code users} table.
 *
 * <p>All methods open and close their own {@link Connection} via
 * {@link Database#getConnection()}. Callers must handle {@link SQLException}.
 */
public class UserDao {

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Full SELECT including all columns used by the User model. */
    private static final String SELECT_ALL =
            "SELECT id, username, email, password_hash, role, is_active FROM users";

    private static User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(User.Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("is_active"));
        return u;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns all active users ordered by username.
     */
    public static List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = SELECT_ALL + " ORDER BY username";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Finds a user by their primary key.
     *
     * @return {@link Optional#empty()} if not found.
     */
    public static Optional<User> findById(long id) throws SQLException {
        String sql = SELECT_ALL + " WHERE id = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Finds an active user by username (case-insensitive on most MySQL collations).
     *
     * @return {@link Optional#empty()} if not found or account is inactive.
     */
    public static Optional<User> findByUsername(String username) throws SQLException {
        String sql = SELECT_ALL + " WHERE username = ? AND is_active = TRUE LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Finds all active users with the given role.
     */
    public static List<User> findByRole(User.Role role) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = SELECT_ALL + " WHERE role = ? AND is_active = TRUE ORDER BY username";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Stamps {@code last_login_at = NOW()} for the given user id.
     * Silently no-ops if the user id does not exist.
     */
    public static void updateLastLogin(long userId) throws SQLException {
        String sql = "UPDATE users SET last_login_at = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Sets {@code is_active} for the given user.
     *
     * @return {@code true} if a row was updated.
     */
    public static boolean setActive(long userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Inserts a new user row (username, email, BCrypt hash, role).
     *
     * @return the generated {@code id}.
     */
    public static long insert(String username, String email,
                              String passwordHash, User.Role role) throws SQLException {
        String sql = """
                INSERT INTO users (username, email, password_hash, role, is_active, created_at)
                VALUES (?, ?, ?, ?, TRUE, NOW())
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, role.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No generated key returned for new user");
            }
        }
    }

    /**
     * Returns {@code true} if any user (active or inactive) already holds the given username.
     * Use this for registration availability checks.
     */
    public static boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Updates the email address for the given user.
     *
     * @return {@code true} if a row was updated.
     */
    public static boolean updateEmail(long userId, String email) throws SQLException {
        String sql = "UPDATE users SET email = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates the BCrypt password hash for the given user.
     *
     * @return {@code true} if a row was updated.
     */
    public static boolean updatePasswordHash(long userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}

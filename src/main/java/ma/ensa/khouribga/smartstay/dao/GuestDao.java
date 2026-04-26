package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.Guest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access operations for the {@code guests} table.
 */
public class GuestDao {

    // ── Private helpers ───────────────────────────────────────────────────────

    private static final String SELECT_ALL =
            """
            SELECT id, first_name, last_name, email, phone,
                   nationality, id_passport_number, preferences, created_at
            FROM guests
            """;

    private static Guest mapRow(ResultSet rs) throws SQLException {
        Guest g = new Guest();
        g.setId(rs.getInt("id"));
        g.setFirstName(rs.getString("first_name"));
        g.setLastName(rs.getString("last_name"));
        g.setEmail(rs.getString("email"));
        g.setPhone(rs.getString("phone"));
        g.setNationality(rs.getString("nationality"));
        g.setIdPassportNumber(rs.getString("id_passport_number"));
        g.setPreferences(rs.getString("preferences"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) g.setCreatedAt(ts.toLocalDateTime());
        return g;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns all guests ordered by last name, then first name.
     */
    public static List<Guest> findAll() throws SQLException {
        List<Guest> list = new ArrayList<>();
        String sql = SELECT_ALL + " ORDER BY last_name, first_name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Looks up a guest by primary key.
     *
     * @return {@link Optional#empty()} if not found.
     */
    public static Optional<Guest> findById(int id) throws SQLException {
        String sql = SELECT_ALL + " WHERE id = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Looks up a guest by their passport / national-ID number.
     * Used at check-in to auto-fill the guest form for returning guests.
     *
     * @return {@link Optional#empty()} if no matching guest found.
     */
    public static Optional<Guest> findByPassport(String passportNumber) throws SQLException {
        String sql = SELECT_ALL + " WHERE id_passport_number = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passportNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Searches guests by last name fragment (case-insensitive LIKE).
     */
    public static List<Guest> searchByName(String fragment) throws SQLException {
        List<Guest> list = new ArrayList<>();
        String sql = SELECT_ALL
                + " WHERE LOWER(last_name) LIKE LOWER(?) OR LOWER(first_name) LIKE LOWER(?)"
                + " ORDER BY last_name, first_name";
        String pattern = "%" + fragment + "%";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Inserts a new guest and returns the generated primary key.
     *
     * <p>Callers should first call {@link #findByPassport(String)} to avoid
     * creating duplicate guest records for the same person.
     *
     * @return the generated {@code id}.
     */
    public static int create(Guest guest) throws SQLException {
        String sql = """
                INSERT INTO guests
                  (first_name, last_name, email, phone, nationality,
                   id_passport_number, preferences, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, guest.getFirstName());
            ps.setString(2, guest.getLastName());
            ps.setString(3, guest.getEmail());
            ps.setString(4, guest.getPhone());
            ps.setString(5, guest.getNationality());
            ps.setString(6, guest.getIdPassportNumber());
            ps.setString(7, guest.getPreferences());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No generated key returned for new guest");
            }
        }
    }

    /**
     * Updates a guest's contact and preference information.
     *
     * @return {@code true} if a row was updated.
     */
    public static boolean update(Guest guest) throws SQLException {
        String sql = """
                UPDATE guests SET
                  first_name = ?, last_name = ?, email = ?, phone = ?,
                  nationality = ?, id_passport_number = ?, preferences = ?
                WHERE id = ?
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, guest.getFirstName());
            ps.setString(2, guest.getLastName());
            ps.setString(3, guest.getEmail());
            ps.setString(4, guest.getPhone());
            ps.setString(5, guest.getNationality());
            ps.setString(6, guest.getIdPassportNumber());
            ps.setString(7, guest.getPreferences());
            ps.setInt(8, guest.getId());
            return ps.executeUpdate() > 0;
        }
    }
}

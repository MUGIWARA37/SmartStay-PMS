package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.db.TxManager;
import ma.ensa.khouribga.smartstay.model.Reservation;
import ma.ensa.khouribga.smartstay.model.Room;
import ma.ensa.khouribga.smartstay.model.Service;

import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Data-access operations for the {@code reservations} table.
 *
 * <p>All SELECT queries JOIN guests and rooms so every returned
 * {@link Reservation} is fully populated for display in TableViews.
 */
public class ReservationDao {

    // ── Private helpers ───────────────────────────────────────────────────────

    private static final String SELECT_JOINED = """
            SELECT r.id, r.guest_id, r.room_id, r.booked_by_user_id,
                   r.reservation_code, r.check_in_date, r.check_out_date,
                   r.adults_count, r.children_count, r.status,
                   CONCAT(g.first_name, ' ', g.last_name) AS guest_full_name,
                   g.email                                 AS guest_email,
                   g.id_passport_number                   AS guest_passport,
                   rm.room_number,
                   rt.name                                AS room_type_name,
                   rt.price_per_night
            FROM reservations r
            JOIN guests     g  ON r.guest_id = g.id
            JOIN rooms      rm ON r.room_id  = rm.id
            JOIN room_types rt ON rm.room_type_id = rt.id
            """;

    private static Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation res = new Reservation();
        res.setId(rs.getInt("id"));
        res.setGuestId(rs.getInt("guest_id"));
        res.setRoomId(rs.getInt("room_id"));
        res.setBookedByUserId(rs.getInt("booked_by_user_id"));
        res.setReservationCode(rs.getString("reservation_code"));
        Date ci = rs.getDate("check_in_date");
        Date co = rs.getDate("check_out_date");
        res.setCheckInDate(ci != null ? ci.toLocalDate() : null);
        res.setCheckOutDate(co != null ? co.toLocalDate() : null);
        res.setAdultsCount(rs.getInt("adults_count"));
        res.setChildrenCount(rs.getInt("children_count"));
        res.setStatus(Reservation.Status.valueOf(rs.getString("status")));
        res.setGuestFullName(rs.getString("guest_full_name"));
        res.setGuestEmail(rs.getString("guest_email"));
        res.setGuestPassportNumber(rs.getString("guest_passport"));
        res.setRoomNumber(rs.getString("room_number"));
        res.setRoomTypeName(rs.getString("room_type_name"));
        res.setPricePerNight(rs.getDouble("price_per_night"));
        return res;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns all reservations, most recent first.
     */
    public static List<Reservation> findAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = SELECT_JOINED + " ORDER BY r.created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Returns all reservations whose check-in date falls within the given calendar year.
     * Most recent first.
     */
    public static List<Reservation> findByYear(int year) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = SELECT_JOINED
                   + " WHERE YEAR(r.check_in_date) = ? ORDER BY r.check_in_date DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns all reservations for a specific guest (by their user account id).
     */
    public static List<Reservation> findByGuest(int bookedByUserId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = SELECT_JOINED
                   + " WHERE r.booked_by_user_id = ? ORDER BY r.check_in_date DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookedByUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns all reservations for a specific room, most recent first.
     */
    public static List<Reservation> findByRoom(int roomId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = SELECT_JOINED
                   + " WHERE r.room_id = ? ORDER BY r.check_in_date DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns reservations with a given status, most recent first.
     */
    public static List<Reservation> findByStatus(Reservation.Status status) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = SELECT_JOINED
                   + " WHERE r.status = ? ORDER BY r.check_in_date DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns all CONFIRMED or CHECKED_IN reservations (active bookings).
     */
    public static List<Reservation> findActive() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = SELECT_JOINED
                   + " WHERE r.status IN ('CONFIRMED','CHECKED_IN')"
                   + " ORDER BY r.check_in_date";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Finds a reservation by its unique confirmation code.
     *
     * @return {@link Optional#empty()} if not found.
     */
    public static Optional<Reservation> findByCode(String code) throws SQLException {
        String sql = SELECT_JOINED + " WHERE r.reservation_code = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Finds a reservation by primary key.
     *
     * @return {@link Optional#empty()} if not found.
     */
    public static Optional<Reservation> findById(int id) throws SQLException {
        String sql = SELECT_JOINED + " WHERE r.id = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Returns reservations whose check-in date falls within [from, to] (inclusive).
     */
    public static List<Reservation> findByCheckInRange(LocalDate from,
                                                        LocalDate to) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = SELECT_JOINED
                   + " WHERE r.check_in_date BETWEEN ? AND ? ORDER BY r.check_in_date";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Inserts a new reservation and returns the generated primary key.
     *
     * <p>The reservation code is generated here as
     * {@code SS-YYYYMMDD-XXXXX} (today + 5 random hex chars).
     *
     * @return the generated {@code id}.
     */
    public static int create(Reservation res) throws SQLException {
        String code = generateCode();
        String sql = """
                INSERT INTO reservations
                  (guest_id, room_id, booked_by_user_id, reservation_code,
                   check_in_date, check_out_date, adults_count, children_count,
                   status, special_requests, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, res.getGuestId());
            ps.setInt(2, res.getRoomId());
            ps.setInt(3, res.getBookedByUserId());
            ps.setString(4, code);
            ps.setDate(5, Date.valueOf(res.getCheckInDate()));
            ps.setDate(6, Date.valueOf(res.getCheckOutDate()));
            ps.setInt(7, res.getAdultsCount());
            ps.setInt(8, res.getChildrenCount());
            ps.setString(9, Reservation.Status.PENDING.name());
            ps.setString(10, null);   // special_requests — set via updateSpecialRequests if needed
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    res.setId(newId);
                    res.setReservationCode(code);
                    return newId;
                }
                throw new SQLException("No generated key returned for new reservation");
            }
        }
    }

    /**
     * Updates the status of a reservation and, when transitioning to
     * {@link Reservation.Status#CHECKED_IN} or {@link Reservation.Status#CHECKED_OUT},
     * updates the room status accordingly — all in one atomic transaction.
     *
     * <ul>
     *   <li>CHECKED_IN  → room becomes OCCUPIED</li>
     *   <li>CHECKED_OUT → room becomes AVAILABLE</li>
     *   <li>CANCELLED   → room becomes AVAILABLE (if it was OCCUPIED by this reservation)</li>
     * </ul>
     *
     * @return {@code true} if the reservation row was updated.
     */
    public static boolean updateStatus(int reservationId,
                                       int roomId,
                                       Reservation.Status newStatus) throws SQLException {
        return TxManager.runInTransaction(conn -> {
            // 1. Update reservation status
            String updRes = "UPDATE reservations SET status = ?, updated_at = NOW() WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updRes)) {
                ps.setString(1, newStatus.name());
                ps.setInt(2, reservationId);
                if (ps.executeUpdate() == 0) return false;
            }

            // 2. Sync room status based on reservation transition
            Room.Status roomStatus = switch (newStatus) {
                case CHECKED_IN  -> Room.Status.OCCUPIED;
                case CHECKED_OUT -> Room.Status.AVAILABLE;
                case CANCELLED   -> Room.Status.AVAILABLE;
                default          -> null;
            };

            if (roomStatus != null) {
                String updRoom = "UPDATE rooms SET status = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updRoom)) {
                    ps.setString(1, roomStatus.name());
                    ps.setInt(2, roomId);
                    ps.executeUpdate();
                }
            }
            return true;
        });
    }

    // ── Service attachments ───────────────────────────────────────────────────

    /**
     * Bulk-inserts rows into {@code reservation_services} for each service in the list
     * that has {@code quantity > 0}.  Executed as a single JDBC batch.
     *
     * @param reservationId the parent reservation
     * @param services      services to attach (only those with quantity &gt; 0 are inserted)
     */
    public static void addServices(int reservationId, List<Service> services) throws SQLException {
        if (services == null || services.isEmpty()) return;
        String sql = """
                INSERT INTO reservation_services
                  (reservation_id, service_id, quantity, unit_price, requested_at)
                VALUES (?, ?, ?, ?, NOW())
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Service svc : services) {
                if (svc.getQuantity() <= 0) continue;
                ps.setInt(1, reservationId);
                ps.setInt(2, svc.getId());
                ps.setInt(3, svc.getQuantity());
                ps.setDouble(4, svc.getUnitPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ── Analytics / chart queries ─────────────────────────────────────────────

    /**
     * Returns a month-name → count map for check-ins (CHECKED_IN or CHECKED_OUT)
     * in the given calendar year.  Every month is present in the map (count defaults to 0).
     *
     * @param year calendar year (e.g. 2025)
     */
    public static Map<String, Integer> countCheckInsByMonth(int year) throws SQLException {
        String sql = """
                SELECT MONTH(check_in_date) AS m, COUNT(*) AS cnt
                FROM   reservations
                WHERE  YEAR(check_in_date) = ?
                  AND  status IN ('CHECKED_IN','CHECKED_OUT')
                GROUP BY MONTH(check_in_date)
                ORDER BY MONTH(check_in_date)
                """;
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Month m : Month.values()) {
            result.put(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), 0);
        }
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = Month.of(rs.getInt("m"))
                                      .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    result.put(key, rs.getInt("cnt"));
                }
            }
        }
        return result;
    }

    /**
     * Returns a month-name → revenue map (room nights × price) for CHECKED_OUT or CHECKED_IN
     * reservations in the given calendar year.  Every month is present (revenue defaults to 0).
     *
     * @param year calendar year (e.g. 2025)
     */
    public static Map<String, Double> sumRevenueByMonth(int year) throws SQLException {
        String sql = """
                SELECT MONTH(r.check_out_date) AS month,
                       SUM(DATEDIFF(r.check_out_date, r.check_in_date) * rt.price_per_night) AS total
                FROM   reservations r
                JOIN   rooms      rm ON r.room_id        = rm.id
                JOIN   room_types rt ON rm.room_type_id  = rt.id
                WHERE  r.status IN ('CHECKED_OUT','CHECKED_IN')
                  AND  YEAR(r.check_out_date) = ?
                GROUP BY MONTH(r.check_out_date)
                ORDER BY MONTH(r.check_out_date)
                """;
        Map<String, Double> result = new LinkedHashMap<>();
        for (Month m : Month.values()) {
            result.put(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), 0.0);
        }
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = Month.of(rs.getInt("month"))
                                       .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    result.put(key, rs.getDouble("total"));
                }
            }
        }
        return result;
    }

    // ── Private utils ─────────────────────────────────────────────────────────

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /** Upper bound (exclusive) for the 5-hex-digit suffix in a reservation code: 16^5 = 0x100000. */
    private static final int CODE_RAND_BOUND = 0x100000;

    /** Generates a unique reservation code: {@code SS-YYYYMMDD-XXXXX}. */
    private static String generateCode() {
        String date = LocalDate.now().toString().replace("-", "");
        String hex  = String.format("%05X", SECURE_RANDOM.nextInt(CODE_RAND_BOUND));
        return "SS-" + date + "-" + hex;
    }
}
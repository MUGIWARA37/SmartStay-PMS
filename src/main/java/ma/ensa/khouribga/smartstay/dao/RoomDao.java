package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.Room;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access operations for the {@code rooms} table.
 *
 * <p>All queries JOIN {@code room_types} so every returned {@link Room}
 * is fully populated with type name, price, occupancy and amenities.
 */
public class RoomDao {

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Shared SELECT that joins rooms ← room_types.
     * Append WHERE / ORDER BY clauses as needed.
     */
    private static final String SELECT_JOINED = """
            SELECT r.id, r.room_number, r.room_type_id, r.floor, r.status, r.notes,
                   rt.name            AS type_name,
                   rt.description     AS type_description,
                   rt.price_per_night,
                   rt.max_occupancy,
                   rt.amenities,
                   ri.image_path
            FROM rooms r
            JOIN room_types rt ON r.room_type_id = rt.id
            LEFT JOIN room_images ri ON r.id = ri.room_id AND ri.is_primary = 1
            """;

    private static Room mapRow(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setId(rs.getInt("id"));
        room.setRoomNumber(rs.getString("room_number"));
        room.setRoomTypeId(rs.getInt("room_type_id"));
        room.setFloor(rs.getInt("floor"));
        room.setStatus(Room.Status.valueOf(rs.getString("status")));
        room.setNotes(rs.getString("notes"));
        room.setTypeName(rs.getString("type_name"));
        room.setTypeDescription(rs.getString("type_description"));
        room.setPricePerNight(rs.getDouble("price_per_night"));
        room.setMaxOccupancy(rs.getInt("max_occupancy"));
        room.setAmenities(rs.getString("amenities"));
        room.setImagePath(rs.getString("image_path"));
        return room;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns every room in the hotel ordered by room number.
     */
    public static List<Room> findAll() throws SQLException {
        List<Room> list = new ArrayList<>();
        String sql = SELECT_JOINED + " ORDER BY r.room_number";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Returns only rooms with {@code status = 'AVAILABLE'}.
     */
    public static List<Room> findAvailable() throws SQLException {
        List<Room> list = new ArrayList<>();
        String sql = SELECT_JOINED + " WHERE r.status = 'AVAILABLE' ORDER BY r.room_number";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Returns rooms that are available (not occupied or maintenance) for the given date range.
     * A room is considered available if it has no CONFIRMED or CHECKED_IN reservations
     * that overlap with the requested [checkIn, checkOut] period.
     */
    public static List<Room> findAvailable(java.time.LocalDate checkIn, java.time.LocalDate checkOut) throws SQLException {
        List<Room> list = new ArrayList<>();
        String sql = SELECT_JOINED + """
                WHERE r.status != 'MAINTENANCE'
                AND r.id NOT IN (
                    SELECT room_id FROM reservations
                    WHERE status IN ('CONFIRMED', 'CHECKED_IN')
                    AND (check_in_date < ? AND check_out_date > ?)
                )
                ORDER BY r.room_number
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(checkOut));
            ps.setDate(2, Date.valueOf(checkIn));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns rooms with the given status.
     */
    public static List<Room> findByStatus(Room.Status status) throws SQLException {
        List<Room> list = new ArrayList<>();
        String sql = SELECT_JOINED + " WHERE r.status = ? ORDER BY r.room_number";
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
     * Returns rooms whose type matches the given name (case-insensitive).
     */
    public static List<Room> findByType(String typeName) throws SQLException {
        List<Room> list = new ArrayList<>();
        String sql = SELECT_JOINED
                   + " WHERE LOWER(rt.name) = LOWER(?) ORDER BY r.room_number";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, typeName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Finds a single room by its primary key.
     *
     * @return {@link Optional#empty()} if not found.
     */
    public static Optional<Room> findById(int id) throws SQLException {
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
     * Finds a single room by its human-readable room number (e.g. "101").
     *
     * @return {@link Optional#empty()} if not found.
     */
    public static Optional<Room> findByRoomNumber(String roomNumber) throws SQLException {
        String sql = SELECT_JOINED + " WHERE r.room_number = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Updates the status of a room.
     *
     * @return {@code true} if a row was updated.
     */
    public static boolean updateStatus(int roomId, Room.Status newStatus) throws SQLException {
        String sql = "UPDATE rooms SET status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, roomId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates both status and optional notes for a room.
     *
     * @return {@code true} if a row was updated.
     */
    public static boolean updateStatusAndNotes(int roomId, Room.Status newStatus,
                                               String notes) throws SQLException {
        String sql = "UPDATE rooms SET status = ?, notes = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, notes);
            ps.setInt(3, roomId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Counts rooms grouped by status — useful for the admin overview.
     * Returns a 4-element int array: [AVAILABLE, OCCUPIED, MAINTENANCE, CLEANING].
     */
    public static int[] countByStatus() throws SQLException {
        String sql = """
                SELECT status, COUNT(*) AS cnt
                FROM rooms
                GROUP BY status
                """;
        int[] counts = new int[4]; // indexed by ordinal of Room.Status
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Room.Status s = Room.Status.valueOf(rs.getString("status"));
                counts[s.ordinal()] = rs.getInt("cnt");
            }
        }
        return counts;
    }
}

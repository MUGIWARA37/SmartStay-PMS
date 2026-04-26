package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.CleaningRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CleaningDao {

    private static final String SELECT_JOINED = """
            SELECT cr.id, cr.room_id, cr.requested_by_user_id, cr.assigned_to_staff_id,
                   cr.reservation_id, cr.priority, cr.status, cr.request_note,
                   cr.created_at, cr.started_at, cr.completed_at,
                   r.room_number,
                   u.username  AS requester_username,
                   su.username AS assigned_staff_username
            FROM cleaning_requests cr
            JOIN rooms r ON cr.room_id = r.id
            JOIN users u ON cr.requested_by_user_id = u.id
            LEFT JOIN staff_profiles sp ON cr.assigned_to_staff_id = sp.id
            LEFT JOIN users su ON sp.user_id = su.id
            """;

    private static CleaningRequest mapRow(ResultSet rs) throws SQLException {
        CleaningRequest c = new CleaningRequest();
        c.setId(rs.getInt("id"));
        c.setRoomId(rs.getInt("room_id"));
        c.setRequestedByUserId(rs.getInt("requested_by_user_id"));
        int assignedId = rs.getInt("assigned_to_staff_id");
        c.setAssignedToStaffId(rs.wasNull() ? null : assignedId);
        int resId = rs.getInt("reservation_id");
        c.setReservationId(rs.wasNull() ? null : resId);
        c.setPriority(CleaningRequest.Priority.valueOf(rs.getString("priority")));
        c.setStatus(CleaningRequest.Status.valueOf(rs.getString("status")));
        c.setRequestNote(rs.getString("request_note"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) c.setCreatedAt(created.toLocalDateTime());
        Timestamp started = rs.getTimestamp("started_at");
        if (started != null) c.setStartedAt(started.toLocalDateTime());
        Timestamp completed = rs.getTimestamp("completed_at");
        if (completed != null) c.setCompletedAt(completed.toLocalDateTime());
        c.setRoomNumber(rs.getString("room_number"));
        c.setRequesterUsername(rs.getString("requester_username"));
        c.setAssignedStaffUsername(rs.getString("assigned_staff_username"));
        return c;
    }

    public static List<CleaningRequest> findAll() throws SQLException {
        List<CleaningRequest> list = new ArrayList<>();
        String sql = SELECT_JOINED + " ORDER BY cr.created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public static List<CleaningRequest> findByStaff(int staffProfileId) throws SQLException {
        List<CleaningRequest> list = new ArrayList<>();
        String sql = SELECT_JOINED + " WHERE cr.assigned_to_staff_id = ? ORDER BY cr.created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public static List<CleaningRequest> findByRoom(int roomId) throws SQLException {
        List<CleaningRequest> list = new ArrayList<>();
        String sql = SELECT_JOINED + " WHERE cr.room_id = ? ORDER BY cr.created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public static List<CleaningRequest> findByStatus(CleaningRequest.Status status) throws SQLException {
        List<CleaningRequest> list = new ArrayList<>();
        String sql = SELECT_JOINED + " WHERE cr.status = ? ORDER BY cr.created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public static int create(CleaningRequest req) throws SQLException {
        String sql = """
                INSERT INTO cleaning_requests
                  (room_id, requested_by_user_id, assigned_to_staff_id, reservation_id,
                   priority, status, request_note, created_at)
                VALUES (?, ?, ?, ?, ?, 'NEW', ?, NOW())
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, req.getRoomId());
            ps.setInt(2, req.getRequestedByUserId());
            if (req.getAssignedToStaffId() != null) ps.setInt(3, req.getAssignedToStaffId());
            else ps.setNull(3, Types.INTEGER);
            if (req.getReservationId() != null) ps.setInt(4, req.getReservationId());
            else ps.setNull(4, Types.INTEGER);
            ps.setString(5, req.getPriority().name());
            ps.setString(6, req.getRequestNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No generated key for cleaning_requests insert");
            }
        }
    }

    public static boolean updateStatus(int id, CleaningRequest.Status newStatus) throws SQLException {
        String sql = switch (newStatus) {
            case IN_PROGRESS -> "UPDATE cleaning_requests SET status=?, started_at=NOW() WHERE id=?";
            case DONE        -> "UPDATE cleaning_requests SET status=?, completed_at=NOW() WHERE id=?";
            default          -> "UPDATE cleaning_requests SET status=? WHERE id=?";
        };
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Returns the staff_profile id for a given user_id, or -1 if not found */
    public static int findStaffProfileId(int userId) throws SQLException {
        String sql = "SELECT id FROM staff_profiles WHERE user_id = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }
}

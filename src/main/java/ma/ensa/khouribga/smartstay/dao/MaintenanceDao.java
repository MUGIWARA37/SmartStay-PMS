package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.MaintenanceRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceDao {

    private static final String SELECT_JOINED = """
            SELECT mr.id, mr.room_id, mr.reported_by_user_id, mr.assigned_to_staff_id,
                   mr.priority, mr.status, mr.title, mr.description,
                   mr.created_at, mr.resolved_at,
                   r.room_number,
                   u.username  AS reporter_username,
                   su.username AS assigned_staff_username
            FROM maintenance_requests mr
            JOIN rooms r ON mr.room_id = r.id
            JOIN users u ON mr.reported_by_user_id = u.id
            LEFT JOIN staff_profiles sp ON mr.assigned_to_staff_id = sp.id
            LEFT JOIN users su ON sp.user_id = su.id
            """;

    private static MaintenanceRequest mapRow(ResultSet rs) throws SQLException {
        MaintenanceRequest m = new MaintenanceRequest();
        m.setId(rs.getInt("id"));
        m.setRoomId(rs.getInt("room_id"));
        m.setReportedByUserId(rs.getInt("reported_by_user_id"));
        int assignedId = rs.getInt("assigned_to_staff_id");
        m.setAssignedToStaffId(rs.wasNull() ? null : assignedId);
        m.setPriority(MaintenanceRequest.Priority.valueOf(rs.getString("priority")));
        m.setStatus(MaintenanceRequest.Status.valueOf(rs.getString("status")));
        m.setTitle(rs.getString("title"));
        m.setDescription(rs.getString("description"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) m.setCreatedAt(created.toLocalDateTime());
        Timestamp resolved = rs.getTimestamp("resolved_at");
        if (resolved != null) m.setResolvedAt(resolved.toLocalDateTime());
        m.setRoomNumber(rs.getString("room_number"));
        m.setReporterUsername(rs.getString("reporter_username"));
        m.setAssignedStaffUsername(rs.getString("assigned_staff_username"));
        return m;
    }

    /** Admin view — all requests, all statuses. */
    public static List<MaintenanceRequest> findAll() throws SQLException {
        List<MaintenanceRequest> list = new ArrayList<>();
        String sql = SELECT_JOINED + " ORDER BY mr.created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Staff view — shows:
     *   (a) tasks explicitly assigned to this staff member, AND
     *   (b) unassigned tasks (assigned_to_staff_id IS NULL) that are still active.
     *
     * FIX: Previously used WHERE assigned_to_staff_id = ? only.
     * Reception dispatches are inserted with assigned_to_staff_id = NULL,
     * so they were invisible to every maintenance staff member.
     * Sorted by priority (URGENT first) then creation time (oldest first).
     */
    public static List<MaintenanceRequest> findByStaffOrUnassigned(int staffProfileId) throws SQLException {
        List<MaintenanceRequest> list = new ArrayList<>();
        String sql = SELECT_JOINED + """
                WHERE (mr.assigned_to_staff_id = ?
                       OR mr.assigned_to_staff_id IS NULL)
                  AND mr.status NOT IN ('RESOLVED', 'CANCELLED')
                ORDER BY
                  FIELD(mr.priority,'URGENT','HIGH','MEDIUM','LOW'),
                  mr.created_at ASC
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Filtering by status — keeps the same OR logic for staff. */
    public static List<MaintenanceRequest> findByStaffOrUnassignedAndStatus(
            int staffProfileId, MaintenanceRequest.Status status) throws SQLException {
        List<MaintenanceRequest> list = new ArrayList<>();
        String sql = SELECT_JOINED + """
                WHERE (mr.assigned_to_staff_id = ?
                       OR mr.assigned_to_staff_id IS NULL)
                  AND mr.status = ?
                ORDER BY mr.created_at ASC
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffProfileId);
            ps.setString(2, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public static List<MaintenanceRequest> findByRoom(int roomId) throws SQLException {
        List<MaintenanceRequest> list = new ArrayList<>();
        String sql = SELECT_JOINED + " WHERE mr.room_id = ? ORDER BY mr.created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public static List<MaintenanceRequest> findByStatus(MaintenanceRequest.Status status) throws SQLException {
        List<MaintenanceRequest> list = new ArrayList<>();
        String sql = SELECT_JOINED + " WHERE mr.status = ? ORDER BY mr.created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public static int create(MaintenanceRequest req) throws SQLException {
        String sql = """
                INSERT INTO maintenance_requests
                  (room_id, reported_by_user_id, assigned_to_staff_id,
                   priority, status, title, description, created_at)
                VALUES (?, ?, ?, ?, 'NEW', ?, ?, NOW())
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, req.getRoomId());
            ps.setInt(2, req.getReportedByUserId());
            if (req.getAssignedToStaffId() != null) ps.setInt(3, req.getAssignedToStaffId());
            else ps.setNull(3, Types.INTEGER);
            ps.setString(4, req.getPriority().name());
            ps.setString(5, req.getTitle());
            ps.setString(6, req.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No generated key for maintenance_requests insert");
            }
        }
    }

    public static boolean updateStatus(int id, MaintenanceRequest.Status newStatus) throws SQLException {
        String sql = (newStatus == MaintenanceRequest.Status.RESOLVED)
                ? "UPDATE maintenance_requests SET status=?, resolved_at=NOW() WHERE id=?"
                : "UPDATE maintenance_requests SET status=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }
}
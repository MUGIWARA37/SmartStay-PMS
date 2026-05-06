package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class StaffDao {

    public record StaffEnrichment(String position, String department, String employeeCode, 
                                 LocalDate hireDate, double salaryBase, 
                                 String shiftName, String startTime, String endTime) {}

    public static StaffEnrichment getStaffEnrichment(long userId) throws SQLException {
        String sql = """
            SELECT sp.position, sp.department, sp.employee_code,
                   sp.hire_date, sp.salary_base,
                   s.shift_name, s.start_time, s.end_time
            FROM staff_profiles sp
            LEFT JOIN staff_shift_assignments ssa
                   ON ssa.staff_profile_id = sp.id
                   AND ssa.assigned_date = CURDATE()
            LEFT JOIN shifts s ON s.id = ssa.shift_id
            WHERE sp.user_id = ?
            LIMIT 1
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date hireDate = rs.getDate("hire_date");
                    return new StaffEnrichment(
                        rs.getString("position"),
                        rs.getString("department"),
                        rs.getString("employee_code"),
                        hireDate != null ? hireDate.toLocalDate() : null,
                        rs.getDouble("salary_base"),
                        rs.getString("shift_name"),
                        rs.getString("start_time"),
                        rs.getString("end_time")
                    );
                }
            }
        }
        return null;
    }

    public static long findProfileIdByUserId(long userId) throws SQLException {
        String sql = "SELECT id FROM staff_profiles WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : -1;
            }
        }
    }

    public static long findShiftIdByName(String shiftName) throws SQLException {
        String sql = "SELECT id FROM shifts WHERE shift_name = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shiftName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : -1;
            }
        }
    }

    public static void assignShift(long staffProfileId, long shiftId, LocalDate date, long adminId, String notes) throws SQLException {
        String sql = """
            INSERT INTO staff_shift_assignments (staff_profile_id, shift_id, assigned_date, assigned_by_user_id, notes)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE shift_id = VALUES(shift_id), assigned_by_user_id = VALUES(assigned_by_user_id), notes = VALUES(notes)
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffProfileId);
            ps.setLong(2, shiftId);
            ps.setDate(3, Date.valueOf(date));
            ps.setLong(4, adminId);
            ps.setString(5, notes);
            ps.executeUpdate();
        }
    }

    public static List<String> getAllShiftDetails() throws SQLException {
        List<String> list = new java.util.ArrayList<>();
        String sql = "SELECT shift_name, start_time, end_time FROM shifts ORDER BY start_time";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("shift_name") + "  (" + rs.getString("start_time").substring(0,5) + " – " + rs.getString("end_time").substring(0,5) + ")");
            }
        }
        return list;
    }
}

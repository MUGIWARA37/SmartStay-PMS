package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.Payroll;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PayrollDao {

    private static final String SELECT_JOINED = """
            SELECT p.id, p.staff_profile_id, p.period_start, p.period_end,
                   p.base_salary, p.bonuses, p.deductions, p.net_salary,
                   p.status, p.generated_at, p.paid_at,
                   u.username       AS staff_username,
                   sp.position      AS staff_position,
                   sp.employee_code AS staff_employee_code
            FROM payroll p
            JOIN staff_profiles sp ON p.staff_profile_id = sp.id
            JOIN users u ON sp.user_id = u.id
            """;

    private static Payroll mapRow(ResultSet rs) throws SQLException {
        Payroll p = new Payroll();
        p.setId(rs.getInt("id"));
        p.setStaffProfileId(rs.getInt("staff_profile_id"));
        Date ps = rs.getDate("period_start");
        Date pe = rs.getDate("period_end");
        if (ps != null) p.setPeriodStart(ps.toLocalDate());
        if (pe != null) p.setPeriodEnd(pe.toLocalDate());
        p.setBaseSalary(rs.getDouble("base_salary"));
        p.setBonuses(rs.getDouble("bonuses"));
        p.setDeductions(rs.getDouble("deductions"));
        p.setNetSalary(rs.getDouble("net_salary"));
        p.setStatus(Payroll.Status.valueOf(rs.getString("status")));
        Timestamp gen = rs.getTimestamp("generated_at");
        Timestamp paid = rs.getTimestamp("paid_at");
        if (gen  != null) p.setGeneratedAt(gen.toLocalDateTime());
        if (paid != null) p.setPaidAt(paid.toLocalDateTime());
        p.setStaffUsername(rs.getString("staff_username"));
        p.setStaffPosition(rs.getString("staff_position"));
        p.setStaffEmployeeCode(rs.getString("staff_employee_code"));
        return p;
    }

    public static List<Payroll> findAll() throws SQLException {
        List<Payroll> list = new ArrayList<>();
        String sql = SELECT_JOINED + " ORDER BY p.period_start DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public static List<Payroll> findByStaff(int staffProfileId) throws SQLException {
        List<Payroll> list = new ArrayList<>();
        String sql = SELECT_JOINED + " WHERE p.staff_profile_id = ? ORDER BY p.period_start DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, staffProfileId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Generates payroll for all active staff for the given period.
     * Uses base_salary from staff_profiles. Bonuses and deductions default to 0.
     */
    public static void generateForPeriod(LocalDate periodStart, LocalDate periodEnd) throws SQLException {
        String staffSql = "SELECT id, salary_base FROM staff_profiles WHERE is_on_duty = TRUE OR is_on_duty = FALSE";
        String insertSql = """
                INSERT INTO payroll
                  (staff_profile_id, period_start, period_end, base_salary,
                   bonuses, deductions, net_salary, status, generated_at)
                VALUES (?, ?, ?, ?, 0, 0, ?, 'GENERATED', NOW())
                """;
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement staffPs = conn.prepareStatement(staffSql);
                     ResultSet rs = staffPs.executeQuery()) {
                    try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                        while (rs.next()) {
                            int staffId = rs.getInt("id");
                            double base = rs.getDouble("salary_base");
                            insertPs.setInt(1, staffId);
                            insertPs.setDate(2, Date.valueOf(periodStart));
                            insertPs.setDate(3, Date.valueOf(periodEnd));
                            insertPs.setDouble(4, base);
                            insertPs.setDouble(5, base); // net = base initially
                            insertPs.addBatch();
                        }
                        insertPs.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static boolean markPaid(int payrollId) throws SQLException {
        String sql = "UPDATE payroll SET status='PAID', paid_at=NOW() WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, payrollId);
            return ps.executeUpdate() > 0;
        }
    }
}

package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceDao {

    private static final String SELECT_ALL =
            "SELECT id, code, name, description, unit_price, is_active FROM services";

    private static Service mapRow(ResultSet rs) throws SQLException {
        Service s = new Service();
        s.setId(rs.getInt("id"));
        s.setCode(rs.getString("code"));
        s.setName(rs.getString("name"));
        s.setDescription(rs.getString("description"));
        s.setUnitPrice(rs.getDouble("unit_price"));
        s.setActive(rs.getBoolean("is_active"));
        return s;
    }

    public static List<Service> findAll() throws SQLException {
        List<Service> list = new ArrayList<>();
        String sql = SELECT_ALL + " ORDER BY name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public static List<Service> findAllActive() throws SQLException {
        List<Service> list = new ArrayList<>();
        String sql = SELECT_ALL + " WHERE is_active = TRUE ORDER BY name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public static Optional<Service> findById(int id) throws SQLException {
        String sql = SELECT_ALL + " WHERE id = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public static int insert(Service service) throws SQLException {
        String sql = """
                INSERT INTO services (code, name, description, unit_price, is_active)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, service.getCode());
            ps.setString(2, service.getName());
            ps.setString(3, service.getDescription());
            ps.setDouble(4, service.getUnitPrice());
            ps.setBoolean(5, service.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No generated key for service insert");
            }
        }
    }

    public static boolean update(Service service) throws SQLException {
        String sql = """
                UPDATE services SET code=?, name=?, description=?, unit_price=?, is_active=?
                WHERE id=?
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, service.getCode());
            ps.setString(2, service.getName());
            ps.setString(3, service.getDescription());
            ps.setDouble(4, service.getUnitPrice());
            ps.setBoolean(5, service.isActive());
            ps.setInt(6, service.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean setActive(int id, boolean active) throws SQLException {
        String sql = "UPDATE services SET is_active=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }
}
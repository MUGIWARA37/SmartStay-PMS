package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceDao {

    private static Service map(ResultSet rs) throws SQLException {
        Service s = new Service();
        s.setId((int) rs.getLong("id"));
        s.setCode(rs.getString("code"));
        s.setName(rs.getString("name"));
        s.setUnitPrice(rs.getDouble("unit_price"));
        s.setActive(rs.getBoolean("is_active"));
        return s;
    }

    public static List<Service> findAll() {
        String sql = "SELECT * FROM services ORDER BY name";
        List<Service> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("ServiceDao.findAll failed", e);
        }
        return list;
    }

    public static List<Service> findActive() {
        String sql = "SELECT * FROM services WHERE is_active = TRUE ORDER BY name";
        List<Service> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("ServiceDao.findActive failed", e);
        }
        return list;
    }

    public static Optional<Service> findById(long id) {
        String sql = "SELECT * FROM services WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("ServiceDao.findById failed", e);
        }
    }

    public static Optional<Service> findByCode(String code) {
        String sql = "SELECT * FROM services WHERE code = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("ServiceDao.findByCode failed", e);
        }
    }

    public static long create(Service s) {
        String sql = "INSERT INTO services (code, name, unit_price, is_active) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getCode());
            ps.setString(2, s.getName());
            ps.setDouble(3, s.getUnitPrice());
            ps.setBoolean(4, s.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No generated key returned");
            }
        } catch (SQLException e) {
            throw new RuntimeException("ServiceDao.create failed", e);
        }
    }

    public static boolean update(Service s) {
        String sql = "UPDATE services SET name = ?, unit_price = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setDouble(2, s.getUnitPrice());
            ps.setInt(3, s.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("ServiceDao.update failed", e);
        }
    }

    public static boolean setActive(long id, boolean active) {
        String sql = "UPDATE services SET is_active = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("ServiceDao.setActive failed", e);
        }
    }
}

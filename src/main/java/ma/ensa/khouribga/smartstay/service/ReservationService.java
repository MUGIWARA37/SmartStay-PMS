package ma.ensa.khouribga.smartstay.service;

import ma.ensa.khouribga.smartstay.dao.ReservationDao;
import ma.ensa.khouribga.smartstay.model.Reservation;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {

    public List<Reservation> getAllReservations() throws SQLException {
        return ReservationDao.findAll();
    }

    public List<Reservation> getReservationsByYear(int year) throws SQLException {
        return ReservationDao.findByYear(year);
    }

    public List<Reservation> getReservationsByGuest(int bookedByUserId) throws SQLException {
        return ReservationDao.findByGuest(bookedByUserId);
    }

    public List<Reservation> getReservationsByRoom(int roomId) throws SQLException {
        return ReservationDao.findByRoom(roomId);
    }

    public List<Reservation> getReservationsByStatus(Reservation.Status status) throws SQLException {
        return ReservationDao.findByStatus(status);
    }

    public List<Reservation> getActiveReservations() throws SQLException {
        return ReservationDao.findActive();
    }

    public Optional<Reservation> getReservationByCode(String code) throws SQLException {
        return ReservationDao.findByCode(code);
    }

    public Optional<Reservation> getReservationById(int id) throws SQLException {
        return ReservationDao.findById(id);
    }

    public List<Reservation> getReservationsByCheckInRange(LocalDate from, LocalDate to) throws SQLException {
        return ReservationDao.findByCheckInRange(from, to);
    }

    public int createReservation(Reservation res) throws SQLException {
        return ReservationDao.create(res);
    }

    public boolean updateReservationStatus(int reservationId, int roomId, Reservation.Status newStatus) throws SQLException {
        return ReservationDao.updateStatus(reservationId, roomId, newStatus);
    }

    public java.util.Map<String, Integer> getMonthlyCheckIns(int year) throws SQLException {
        String sql = "SELECT MONTH(check_in_date) AS m, COUNT(*) AS cnt FROM reservations WHERE YEAR(check_in_date) = ? AND status IN ('CHECKED_IN','CHECKED_OUT') GROUP BY MONTH(check_in_date) ORDER BY m";
        java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
        for (java.time.Month m : java.time.Month.values()) result.put(m.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH), 0);
        try (java.sql.Connection conn = ma.ensa.khouribga.smartstay.db.Database.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.put(java.time.Month.of(rs.getInt("m")).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH), rs.getInt("cnt"));
            }
        }
        return result;
    }

    public java.util.Map<String, Double> getMonthlyRevenue(int year) throws SQLException {
        String sql = "SELECT MONTH(r.check_out_date) AS month, SUM(DATEDIFF(r.check_out_date, r.check_in_date) * rt.price_per_night) AS total FROM reservations r JOIN rooms rm ON r.room_id = rm.id JOIN room_types rt ON rm.room_type_id = rt.id WHERE YEAR(r.check_out_date) = ? GROUP BY MONTH(r.check_out_date) ORDER BY month";
        java.util.Map<String, Double> result = new java.util.LinkedHashMap<>();
        for (java.time.Month m : java.time.Month.values()) result.put(m.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH), 0.0);
        try (java.sql.Connection conn = ma.ensa.khouribga.smartstay.db.Database.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (java.sql.ResultSet rs = ps.executeQuery()) { while (rs.next()) result.put(java.time.Month.of(rs.getInt("month")).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH), rs.getDouble("total")); }
        }
        return result;
    }
}

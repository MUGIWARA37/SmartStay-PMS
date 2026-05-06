package ma.ensa.khouribga.smartstay.service;

import ma.ensa.khouribga.smartstay.dao.RoomDao;
import ma.ensa.khouribga.smartstay.model.Room;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class RoomService {

    public List<Room> getAllRooms() throws SQLException {
        return RoomDao.findAll();
    }

    public List<Room> getAvailableRooms() throws SQLException {
        return RoomDao.findAvailable();
    }

    public List<Room> getRoomsByStatus(Room.Status status) throws SQLException {
        return RoomDao.findByStatus(status);
    }

    public List<Room> getRoomsByType(String typeName) throws SQLException {
        return RoomDao.findByType(typeName);
    }

    public Optional<Room> getRoomById(int id) throws SQLException {
        return RoomDao.findById(id);
    }

    public Optional<Room> getRoomByNumber(String roomNumber) throws SQLException {
        return RoomDao.findByRoomNumber(roomNumber);
    }

    public boolean updateRoomStatus(int roomId, Room.Status newStatus) throws SQLException {
        return RoomDao.updateStatus(roomId, newStatus);
    }

    public boolean updateRoomStatusAndNotes(int roomId, Room.Status newStatus, String notes) throws SQLException {
        return RoomDao.updateStatusAndNotes(roomId, newStatus, notes);
    }

    public int[] getRoomCountsByStatus() throws SQLException {
        return RoomDao.countByStatus();
    }
}

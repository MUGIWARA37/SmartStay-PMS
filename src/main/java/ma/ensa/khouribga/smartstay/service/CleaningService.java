package ma.ensa.khouribga.smartstay.service;

import ma.ensa.khouribga.smartstay.dao.CleaningDao;
import ma.ensa.khouribga.smartstay.model.CleaningRequest;
import java.sql.SQLException;
import java.util.List;

public class CleaningService {

    public List<CleaningRequest> getAllCleaningRequests() throws SQLException {
        return CleaningDao.findAll();
    }

    public List<CleaningRequest> getCleaningRequestsForStaff(int staffProfileId) throws SQLException {
        return CleaningDao.findByStaffOrUnassigned(staffProfileId);
    }

    public List<CleaningRequest> getCleaningRequestsForStaffByStatus(int staffProfileId, CleaningRequest.Status status) throws SQLException {
        return CleaningDao.findByStaffOrUnassignedAndStatus(staffProfileId, status);
    }

    public List<CleaningRequest> getCleaningRequestsByRoom(int roomId) throws SQLException {
        return CleaningDao.findByRoom(roomId);
    }

    public List<CleaningRequest> getCleaningRequestsByStatus(CleaningRequest.Status status) throws SQLException {
        return CleaningDao.findByStatus(status);
    }

    public int createCleaningRequest(CleaningRequest req) throws SQLException {
        return CleaningDao.create(req);
    }

    public boolean updateCleaningStatus(int id, CleaningRequest.Status newStatus) throws SQLException {
        return CleaningDao.updateStatus(id, newStatus);
    }

    public int getStaffProfileId(int userId) throws SQLException {
        return CleaningDao.findStaffProfileId(userId);
    }
}

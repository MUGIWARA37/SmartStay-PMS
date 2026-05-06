package ma.ensa.khouribga.smartstay.service;

import ma.ensa.khouribga.smartstay.dao.MaintenanceDao;
import ma.ensa.khouribga.smartstay.model.MaintenanceRequest;
import java.sql.SQLException;
import java.util.List;

public class MaintenanceService {

    public List<MaintenanceRequest> getAllMaintenanceRequests() throws SQLException {
        return MaintenanceDao.findAll();
    }

    public List<MaintenanceRequest> getMaintenanceRequestsForStaff(int staffProfileId) throws SQLException {
        return MaintenanceDao.findByStaffOrUnassigned(staffProfileId);
    }

    public List<MaintenanceRequest> getMaintenanceRequestsForStaffByStatus(int staffProfileId, MaintenanceRequest.Status status) throws SQLException {
        return MaintenanceDao.findByStaffOrUnassignedAndStatus(staffProfileId, status);
    }

    public List<MaintenanceRequest> getMaintenanceRequestsByRoom(int roomId) throws SQLException {
        return MaintenanceDao.findByRoom(roomId);
    }

    public List<MaintenanceRequest> getMaintenanceRequestsByStatus(MaintenanceRequest.Status status) throws SQLException {
        return MaintenanceDao.findByStatus(status);
    }

    public int createMaintenanceRequest(MaintenanceRequest req) throws SQLException {
        return MaintenanceDao.create(req);
    }

    public boolean updateMaintenanceStatus(int id, MaintenanceRequest.Status newStatus) throws SQLException {
        return MaintenanceDao.updateStatus(id, newStatus);
    }
}

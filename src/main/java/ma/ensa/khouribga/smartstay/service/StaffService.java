package ma.ensa.khouribga.smartstay.service;

import ma.ensa.khouribga.smartstay.dao.StaffDao;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class StaffService {

    public StaffDao.StaffEnrichment getStaffEnrichment(long userId) throws SQLException {
        return StaffDao.getStaffEnrichment(userId);
    }

    public void assignShift(long userId, String shiftName, LocalDate date, long adminId, String notes) throws SQLException {
        long profileId = StaffDao.findProfileIdByUserId(userId);
        if (profileId < 0) throw new SQLException("Staff profile not found.");
        
        long shiftId = StaffDao.findShiftIdByName(shiftName);
        if (shiftId < 0) throw new SQLException("Shift not found.");
        
        StaffDao.assignShift(profileId, shiftId, date, adminId, notes);
    }

    public List<String> getShiftOptions() throws SQLException {
        return StaffDao.getAllShiftDetails();
    }
}

package ma.ensa.khouribga.smartstay.service;

import ma.ensa.khouribga.smartstay.dao.PayrollDao;
import ma.ensa.khouribga.smartstay.model.Payroll;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PayrollService {

    public List<Payroll> getAllPayroll() throws SQLException {
        return PayrollDao.findAll();
    }

    public List<Payroll> getPayrollByStaff(int staffProfileId) throws SQLException {
        return PayrollDao.findByStaff(staffProfileId);
    }

    public void generatePayrollForPeriod(LocalDate periodStart, LocalDate periodEnd) throws SQLException {
        PayrollDao.generateForPeriod(periodStart, periodEnd);
    }

    public boolean markPayrollAsPaid(int payrollId) throws SQLException {
        return PayrollDao.markPaid(payrollId);
    }
}

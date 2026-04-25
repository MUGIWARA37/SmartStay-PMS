package ma.ensa.khouribga.smartstay.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps to the `payroll` table.
 * Carries staff name for display (joined from staff_profiles → users).
 */
public class Payroll {

    public enum Status {
        GENERATED, PAID, CANCELLED
    }

    private int            id;
    private int            staffProfileId;
    private LocalDate      periodStart;
    private LocalDate      periodEnd;
    private double         baseSalary;
    private double         bonuses;
    private double         deductions;
    private double         netSalary;
    private Status         status;
    private LocalDateTime  generatedAt;
    private LocalDateTime  paidAt;

    // Joined display fields
    private String staffUsername;
    private String staffPosition;
    private String staffEmployeeCode;

    // Constructors ────────────────────────────────────────────

    public Payroll() {}

    // Getters & Setters ───────────────────────────────────────

    public int getId()                             { return id; }
    public void setId(int v)                       { this.id = v; }

    public int getStaffProfileId()                 { return staffProfileId; }
    public void setStaffProfileId(int v)           { this.staffProfileId = v; }

    public LocalDate getPeriodStart()              { return periodStart; }
    public void setPeriodStart(LocalDate v)        { this.periodStart = v; }

    public LocalDate getPeriodEnd()                { return periodEnd; }
    public void setPeriodEnd(LocalDate v)          { this.periodEnd = v; }

    public double getBaseSalary()                  { return baseSalary; }
    public void setBaseSalary(double v)            { this.baseSalary = v; }

    public double getBonuses()                     { return bonuses; }
    public void setBonuses(double v)               { this.bonuses = v; }

    public double getDeductions()                  { return deductions; }
    public void setDeductions(double v)            { this.deductions = v; }

    public double getNetSalary()                   { return netSalary; }
    public void setNetSalary(double v)             { this.netSalary = v; }

    public Status getStatus()                      { return status; }
    public void setStatus(Status v)                { this.status = v; }

    public LocalDateTime getGeneratedAt()          { return generatedAt; }
    public void setGeneratedAt(LocalDateTime v)    { this.generatedAt = v; }

    public LocalDateTime getPaidAt()               { return paidAt; }
    public void setPaidAt(LocalDateTime v)         { this.paidAt = v; }

    // Joined fields
    public String getStaffUsername()               { return staffUsername; }
    public void setStaffUsername(String v)         { this.staffUsername = v; }

    public String getStaffPosition()               { return staffPosition; }
    public void setStaffPosition(String v)         { this.staffPosition = v; }

    public String getStaffEmployeeCode()           { return staffEmployeeCode; }
    public void setStaffEmployeeCode(String v)     { this.staffEmployeeCode = v; }

    // Derived ─────────────────────────────────────────────────

    /**
     * Recalculate netSalary = baseSalary + bonuses - deductions.
     * Call this before inserting/updating a payroll record.
     */
    public void recalculate() {
        this.netSalary = baseSalary + bonuses - deductions;
    }

    @Override
    public String toString() {
        return "Payroll{" + staffUsername + ", " + periodStart + "→" + periodEnd
                + ", net=" + netSalary + ", " + status + "}";
    }
}
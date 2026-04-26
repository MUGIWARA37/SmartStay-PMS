package ma.ensa.khouribga.smartstay.model;

import java.time.LocalDateTime;

/**
 * Maps to the {@code maintenance_requests} table.
 * Carries joined display fields from rooms and users.
 */
public class MaintenanceRequest {

    public enum Priority { LOW, MEDIUM, HIGH, URGENT }

    public enum Status { NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CANCELLED }

    // maintenance_requests columns ────────────────────────────
    private int           id;
    private int           roomId;
    private int           reportedByUserId;
    private Integer       assignedToStaffId;    // nullable
    private Priority      priority;
    private Status        status;
    private String        title;
    private String        description;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;           // nullable

    // Joined display fields ───────────────────────────────────
    private String roomNumber;
    private String reporterUsername;
    private String assignedStaffUsername;       // nullable

    // Constructors ────────────────────────────────────────────

    public MaintenanceRequest() {}

    // Getters & Setters ───────────────────────────────────────

    public int     getId()                              { return id; }
    public void    setId(int v)                         { this.id = v; }

    public int     getRoomId()                          { return roomId; }
    public void    setRoomId(int v)                     { this.roomId = v; }

    public int     getReportedByUserId()                { return reportedByUserId; }
    public void    setReportedByUserId(int v)           { this.reportedByUserId = v; }

    public Integer getAssignedToStaffId()               { return assignedToStaffId; }
    public void    setAssignedToStaffId(Integer v)      { this.assignedToStaffId = v; }

    public Priority getPriority()                       { return priority; }
    public void     setPriority(Priority v)             { this.priority = v; }

    public Status  getStatus()                          { return status; }
    public void    setStatus(Status v)                  { this.status = v; }

    public String  getTitle()                           { return title; }
    public void    setTitle(String v)                   { this.title = v; }

    public String  getDescription()                     { return description; }
    public void    setDescription(String v)             { this.description = v; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void          setCreatedAt(LocalDateTime v)  { this.createdAt = v; }

    public LocalDateTime getResolvedAt()                { return resolvedAt; }
    public void          setResolvedAt(LocalDateTime v) { this.resolvedAt = v; }

    // Joined fields
    public String  getRoomNumber()                      { return roomNumber; }
    public void    setRoomNumber(String v)              { this.roomNumber = v; }

    public String  getReporterUsername()                { return reporterUsername; }
    public void    setReporterUsername(String v)        { this.reporterUsername = v; }

    public String  getAssignedStaffUsername()           { return assignedStaffUsername; }
    public void    setAssignedStaffUsername(String v)   { this.assignedStaffUsername = v; }

    @Override
    public String toString() {
        return "MaintenanceRequest{" + id + ", room=" + roomNumber
                + ", \"" + title + "\", " + priority + ", " + status + "}";
    }
}

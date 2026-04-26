package ma.ensa.khouribga.smartstay.model;

import java.time.LocalDateTime;

/**
 * Maps to the {@code cleaning_requests} table.
 * Carries joined display fields from rooms and users.
 */
public class CleaningRequest {

    public enum Priority { LOW, MEDIUM, HIGH, URGENT }

    public enum Status { NEW, ASSIGNED, IN_PROGRESS, DONE, CANCELLED }

    // cleaning_requests columns ───────────────────────────────
    private int           id;
    private int           roomId;
    private int           requestedByUserId;
    private Integer       assignedToStaffId;   // nullable
    private Integer       reservationId;        // nullable
    private Priority      priority;
    private Status        status;
    private String        requestNote;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;            // nullable
    private LocalDateTime completedAt;          // nullable

    // Joined display fields ───────────────────────────────────
    private String roomNumber;
    private String requesterUsername;
    private String assignedStaffUsername;       // nullable

    // Constructors ────────────────────────────────────────────

    public CleaningRequest() {}

    // Getters & Setters ───────────────────────────────────────

    public int     getId()                              { return id; }
    public void    setId(int v)                         { this.id = v; }

    public int     getRoomId()                          { return roomId; }
    public void    setRoomId(int v)                     { this.roomId = v; }

    public int     getRequestedByUserId()               { return requestedByUserId; }
    public void    setRequestedByUserId(int v)          { this.requestedByUserId = v; }

    public Integer getAssignedToStaffId()               { return assignedToStaffId; }
    public void    setAssignedToStaffId(Integer v)      { this.assignedToStaffId = v; }

    public Integer getReservationId()                   { return reservationId; }
    public void    setReservationId(Integer v)          { this.reservationId = v; }

    public Priority getPriority()                       { return priority; }
    public void     setPriority(Priority v)             { this.priority = v; }

    public Status  getStatus()                          { return status; }
    public void    setStatus(Status v)                  { this.status = v; }

    public String  getRequestNote()                     { return requestNote; }
    public void    setRequestNote(String v)             { this.requestNote = v; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void          setCreatedAt(LocalDateTime v)  { this.createdAt = v; }

    public LocalDateTime getStartedAt()                 { return startedAt; }
    public void          setStartedAt(LocalDateTime v)  { this.startedAt = v; }

    public LocalDateTime getCompletedAt()               { return completedAt; }
    public void          setCompletedAt(LocalDateTime v){ this.completedAt = v; }

    // Joined fields
    public String  getRoomNumber()                      { return roomNumber; }
    public void    setRoomNumber(String v)              { this.roomNumber = v; }

    public String  getRequesterUsername()               { return requesterUsername; }
    public void    setRequesterUsername(String v)       { this.requesterUsername = v; }

    public String  getAssignedStaffUsername()           { return assignedStaffUsername; }
    public void    setAssignedStaffUsername(String v)   { this.assignedStaffUsername = v; }

    @Override
    public String toString() {
        return "CleaningRequest{" + id + ", room=" + roomNumber
                + ", " + priority + ", " + status + "}";
    }
}
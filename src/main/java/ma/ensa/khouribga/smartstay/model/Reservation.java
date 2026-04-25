package ma.ensa.khouribga.smartstay.model;

import java.time.LocalDate;

/**
 * Maps to the `reservations` table.
 * Also carries guest + room info for display purposes (populated via JOIN queries).
 */
public class Reservation {

    public enum Status {
        PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
    }

    // reservations columns
    private int       id;
    private int       guestId;
    private int       roomId;
    private int       bookedByUserId;
    private String    reservationCode;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int       adultsCount;
    private int       childrenCount;
    private Status    status;

    // Joined display fields ───────────────────────────────────
    /** Full name from guests table */
    private String guestFullName;
    /** email from guests table */
    private String guestEmail;
    /** id_passport_number from guests */
    private String guestPassportNumber;
    /** room_number from rooms */
    private String roomNumber;
    /** type name from room_types */
    private String roomTypeName;
    /** price_per_night from room_types */
    private double pricePerNight;

    // Constructors ────────────────────────────────────────────

    public Reservation() {}

    // Getters & Setters ───────────────────────────────────────

    public int getId()                            { return id; }
    public void setId(int v)                      { this.id = v; }

    public int getGuestId()                       { return guestId; }
    public void setGuestId(int v)                 { this.guestId = v; }

    public int getRoomId()                        { return roomId; }
    public void setRoomId(int v)                  { this.roomId = v; }

    public int getBookedByUserId()                { return bookedByUserId; }
    public void setBookedByUserId(int v)          { this.bookedByUserId = v; }

    public String getReservationCode()            { return reservationCode; }
    public void setReservationCode(String v)      { this.reservationCode = v; }

    public LocalDate getCheckInDate()             { return checkInDate; }
    public void setCheckInDate(LocalDate v)       { this.checkInDate = v; }

    public LocalDate getCheckOutDate()            { return checkOutDate; }
    public void setCheckOutDate(LocalDate v)      { this.checkOutDate = v; }

    public int getAdultsCount()                   { return adultsCount; }
    public void setAdultsCount(int v)             { this.adultsCount = v; }

    public int getChildrenCount()                 { return childrenCount; }
    public void setChildrenCount(int v)           { this.childrenCount = v; }

    public Status getStatus()                     { return status; }
    public void setStatus(Status v)               { this.status = v; }

    // Joined fields
    public String getGuestFullName()              { return guestFullName; }
    public void setGuestFullName(String v)        { this.guestFullName = v; }

    public String getGuestEmail()                 { return guestEmail; }
    public void setGuestEmail(String v)           { this.guestEmail = v; }

    public String getGuestPassportNumber()        { return guestPassportNumber; }
    public void setGuestPassportNumber(String v)  { this.guestPassportNumber = v; }

    public String getRoomNumber()                 { return roomNumber; }
    public void setRoomNumber(String v)           { this.roomNumber = v; }

    public String getRoomTypeName()               { return roomTypeName; }
    public void setRoomTypeName(String v)         { this.roomTypeName = v; }

    public double getPricePerNight()              { return pricePerNight; }
    public void setPricePerNight(double v)        { this.pricePerNight = v; }

    // Derived helpers ─────────────────────────────────────────

    /** Number of nights between check-in and check-out. */
    public long getNights() {
        if (checkInDate == null || checkOutDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    /** Base price = nights × pricePerNight (no services). */
    public double getBaseTotal() {
        return getNights() * pricePerNight;
    }

    @Override
    public String toString() {
        return "Reservation{" + reservationCode + ", " + guestFullName + ", " + status + "}";
    }
}
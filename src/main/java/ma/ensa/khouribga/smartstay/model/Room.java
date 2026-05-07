package ma.ensa.khouribga.smartstay.model;

/**
 * Maps to the `rooms` table joined with `room_types`.
 */
public class Room {

    public enum Status {
        AVAILABLE, OCCUPIED, MAINTENANCE, CLEANING
    }

    // rooms columns
    private int    id;
    private String roomNumber;
    private int    roomTypeId;
    private int    floor;
    private Status status;
    private String notes;

    // room_types columns (joined)
    private String typeName;
    private String typeDescription;
    private double pricePerNight;
    private int    maxOccupancy;
    private String amenities;
    private String imagePath;

    // Constructors ────────────────────────────────────────────

    public Room() {}

    public Room(int id, String roomNumber, int roomTypeId, int floor,
                Status status, String notes,
                String typeName, String typeDescription,
                double pricePerNight, int maxOccupancy, String amenities) {
        this.id              = id;
        this.roomNumber      = roomNumber;
        this.roomTypeId      = roomTypeId;
        this.floor           = floor;
        this.status          = status;
        this.notes           = notes;
        this.typeName        = typeName;
        this.typeDescription = typeDescription;
        this.pricePerNight   = pricePerNight;
        this.maxOccupancy    = maxOccupancy;
        this.amenities       = amenities;
    }

    public Room(int id, String roomNumber, int roomTypeId, int floor,
                Status status, String notes,
                String typeName, String typeDescription,
                double pricePerNight, int maxOccupancy, String amenities,
                String imagePath) {
        this(id, roomNumber, roomTypeId, floor, status, notes, typeName, typeDescription, pricePerNight, maxOccupancy, amenities);
        this.imagePath = imagePath;
    }

    // Getters & Setters ───────────────────────────────────────

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public String getRoomNumber()             { return roomNumber; }
    public void setRoomNumber(String v)       { this.roomNumber = v; }

    public int getRoomTypeId()                { return roomTypeId; }
    public void setRoomTypeId(int v)          { this.roomTypeId = v; }

    public int getFloor()                     { return floor; }
    public void setFloor(int v)               { this.floor = v; }

    public Status getStatus()                 { return status; }
    public void setStatus(Status v)           { this.status = v; }

    public String getNotes()                  { return notes; }
    public void setNotes(String v)            { this.notes = v; }

    public String getTypeName()               { return typeName; }
    public void setTypeName(String v)         { this.typeName = v; }

    public String getTypeDescription()        { return typeDescription; }
    public void setTypeDescription(String v)  { this.typeDescription = v; }

    public double getPricePerNight()          { return pricePerNight; }
    public void setPricePerNight(double v)    { this.pricePerNight = v; }

    public int getMaxOccupancy()              { return maxOccupancy; }
    public void setMaxOccupancy(int v)        { this.maxOccupancy = v; }

    public String getAmenities()              { return amenities; }
    public void setAmenities(String v)        { this.amenities = v; }

    public String getImagePath()              { return imagePath; }
    public void setImagePath(String v)        { this.imagePath = v; }

    @Override
    public String toString() {
        return "Room{" + roomNumber + ", " + typeName + ", " + status + "}";
    }
}

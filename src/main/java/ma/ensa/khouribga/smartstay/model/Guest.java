package ma.ensa.khouribga.smartstay.model;

import java.time.LocalDateTime;

/**
 * Maps to the {@code guests} table.
 */
public class Guest {

    private int           id;
    private String        firstName;
    private String        lastName;
    private String        email;
    private String        phone;
    private String        nationality;
    private String        idPassportNumber;
    private String        preferences;
    private LocalDateTime createdAt;

    // Constructors ────────────────────────────────────────────

    public Guest() {}

    public Guest(String firstName, String lastName, String email,
                 String phone, String nationality, String idPassportNumber,
                 String preferences) {
        this.firstName        = firstName;
        this.lastName         = lastName;
        this.email            = email;
        this.phone            = phone;
        this.nationality      = nationality;
        this.idPassportNumber = idPassportNumber;
        this.preferences      = preferences;
    }

    // Derived ─────────────────────────────────────────────────

    /** Convenience: {@code firstName + " " + lastName}. */
    public String getFullName() {
        return (firstName == null ? "" : firstName)
             + " "
             + (lastName  == null ? "" : lastName);
    }

    // Getters & Setters ───────────────────────────────────────

    public int    getId()                          { return id; }
    public void   setId(int v)                     { this.id = v; }

    public String getFirstName()                   { return firstName; }
    public void   setFirstName(String v)           { this.firstName = v; }

    public String getLastName()                    { return lastName; }
    public void   setLastName(String v)            { this.lastName = v; }

    public String getEmail()                       { return email; }
    public void   setEmail(String v)               { this.email = v; }

    public String getPhone()                       { return phone; }
    public void   setPhone(String v)               { this.phone = v; }

    public String getNationality()                 { return nationality; }
    public void   setNationality(String v)         { this.nationality = v; }

    public String getIdPassportNumber()            { return idPassportNumber; }
    public void   setIdPassportNumber(String v)    { this.idPassportNumber = v; }

    public String getPreferences()                 { return preferences; }
    public void   setPreferences(String v)         { this.preferences = v; }

    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void          setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    @Override
    public String toString() {
        return "Guest{" + id + ", " + getFullName() + ", " + idPassportNumber + "}";
    }
}
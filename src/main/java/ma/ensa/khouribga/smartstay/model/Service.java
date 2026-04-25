package ma.ensa.khouribga.smartstay.model;

/**
 * Maps to the `services` table.
 * Also used as a line-item in the payment screen (carries quantity).
 */
public class Service {

    private int     id;
    private String  code;
    private String  name;
    private String  description;
    private double  unitPrice;
    private boolean isActive;

    // Transient — used in payment/booking flow, not persisted here
    private int quantity = 0;

    // Constructors ────────────────────────────────────────────

    public Service() {}

    public Service(int id, String code, String name,
                   String description, double unitPrice, boolean isActive) {
        this.id          = id;
        this.code        = code;
        this.name        = name;
        this.description = description;
        this.unitPrice   = unitPrice;
        this.isActive    = isActive;
    }

    // Getters & Setters ───────────────────────────────────────

    public int getId()                    { return id; }
    public void setId(int v)              { this.id = v; }

    public String getCode()               { return code; }
    public void setCode(String v)         { this.code = v; }

    public String getName()               { return name; }
    public void setName(String v)         { this.name = v; }

    public String getDescription()        { return description; }
    public void setDescription(String v)  { this.description = v; }

    public double getUnitPrice()          { return unitPrice; }
    public void setUnitPrice(double v)    { this.unitPrice = v; }

    public boolean isActive()             { return isActive; }
    public void setActive(boolean v)      { this.isActive = v; }

    public int getQuantity()              { return quantity; }
    public void setQuantity(int v)        { this.quantity = v; }

    // Derived ─────────────────────────────────────────────────

    /** Subtotal for this service line: unitPrice × quantity */
    public double getLineTotal() {
        return unitPrice * quantity;
    }

    @Override
    public String toString() {
        return name + " (" + code + ") — " + unitPrice + "/unit";
    }
}
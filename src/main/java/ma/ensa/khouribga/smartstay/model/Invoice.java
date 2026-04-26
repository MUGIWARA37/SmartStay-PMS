package ma.ensa.khouribga.smartstay.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps to the {@code invoices} table.
 * Optionally carries a list of {@link InvoiceLine} objects for full invoice display.
 */
public class Invoice {

    public enum Status { DRAFT, ISSUED, PAID, CANCELLED }

    // invoices columns ────────────────────────────────────────
    private int           id;
    private int           reservationId;
    private String        invoiceNumber;
    private LocalDateTime issuedAt;
    private double        subtotalAmount;
    private double        taxAmount;
    private double        totalAmount;
    private Status        status;
    private String        notes;

    // Eagerly-loaded lines (optional — populated by InvoiceDao.findByReservation) ──
    private List<InvoiceLine> lines = new ArrayList<>();

    // Constructors ────────────────────────────────────────────

    public Invoice() {}

    // Getters & Setters ───────────────────────────────────────

    public int    getId()                          { return id; }
    public void   setId(int v)                     { this.id = v; }

    public int    getReservationId()               { return reservationId; }
    public void   setReservationId(int v)          { this.reservationId = v; }

    public String getInvoiceNumber()               { return invoiceNumber; }
    public void   setInvoiceNumber(String v)       { this.invoiceNumber = v; }

    public LocalDateTime getIssuedAt()             { return issuedAt; }
    public void          setIssuedAt(LocalDateTime v) { this.issuedAt = v; }

    public double getSubtotalAmount()              { return subtotalAmount; }
    public void   setSubtotalAmount(double v)      { this.subtotalAmount = v; }

    public double getTaxAmount()                   { return taxAmount; }
    public void   setTaxAmount(double v)           { this.taxAmount = v; }

    public double getTotalAmount()                 { return totalAmount; }
    public void   setTotalAmount(double v)         { this.totalAmount = v; }

    public Status getStatus()                      { return status; }
    public void   setStatus(Status v)              { this.status = v; }

    public String getNotes()                       { return notes; }
    public void   setNotes(String v)               { this.notes = v; }

    public List<InvoiceLine> getLines()            { return lines; }
    public void setLines(List<InvoiceLine> v)      { this.lines = v; }

    @Override
    public String toString() {
        return "Invoice{" + invoiceNumber + ", total=" + totalAmount + ", " + status + "}";
    }

    // ── Nested: InvoiceLine ───────────────────────────────────────────────────

    /**
     * Maps to the {@code invoice_lines} table.
     * Each line is either a ROOM charge, a SERVICE charge, or OTHER.
     */
    public static class InvoiceLine {

        public enum LineType { ROOM, SERVICE, OTHER }

        private int      id;
        private int      invoiceId;
        private LineType lineType;
        private Integer  referenceId;   // nullable: room_id or service_id
        private String   description;
        private int      quantity;
        private double   unitPrice;
        private double   lineTotal;

        public InvoiceLine() {}

        public InvoiceLine(LineType lineType, Integer referenceId,
                           String description, int quantity, double unitPrice) {
            this.lineType    = lineType;
            this.referenceId = referenceId;
            this.description = description;
            this.quantity    = quantity;
            this.unitPrice   = unitPrice;
            this.lineTotal   = quantity * unitPrice;
        }

        // Derived
        public void recalculate() { this.lineTotal = quantity * unitPrice; }

        // Getters & Setters
        public int      getId()                       { return id; }
        public void     setId(int v)                  { this.id = v; }

        public int      getInvoiceId()                { return invoiceId; }
        public void     setInvoiceId(int v)           { this.invoiceId = v; }

        public LineType getLineType()                 { return lineType; }
        public void     setLineType(LineType v)       { this.lineType = v; }

        public Integer  getReferenceId()              { return referenceId; }
        public void     setReferenceId(Integer v)     { this.referenceId = v; }

        public String   getDescription()              { return description; }
        public void     setDescription(String v)      { this.description = v; }

        public int      getQuantity()                 { return quantity; }
        public void     setQuantity(int v)            { this.quantity = v; }

        public double   getUnitPrice()                { return unitPrice; }
        public void     setUnitPrice(double v)        { this.unitPrice = v; }

        public double   getLineTotal()                { return lineTotal; }
        public void     setLineTotal(double v)        { this.lineTotal = v; }

        @Override
        public String toString() {
            return "InvoiceLine{" + lineType + ", " + description
                    + ", qty=" + quantity + ", total=" + lineTotal + "}";
        }
    }
}

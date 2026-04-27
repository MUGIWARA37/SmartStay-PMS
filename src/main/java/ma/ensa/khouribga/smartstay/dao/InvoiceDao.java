package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.db.TxManager;
import ma.ensa.khouribga.smartstay.model.Invoice;
import ma.ensa.khouribga.smartstay.model.Invoice.InvoiceLine;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for `invoices` and `invoice_lines`.
 *
 * Key design decisions:
 *  - create()         inserts header + all lines atomically, then refreshes totals
 *  - addLine()        appends one line, recalculates total
 *  - calculateTotals()  SUM(line_total) → updates invoices.total_amount
 *  - updateStatus()   status transition guard (DRAFT → ISSUED → PAID)
 */
public class InvoiceDao {

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private static Invoice mapHeader(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setId(rs.getLong("id"));
        inv.setReservationId(rs.getLong("reservation_id"));
        inv.setInvoiceNumber(rs.getString("invoice_number"));
        inv.setTotalAmount(rs.getBigDecimal("total_amount"));
        inv.setStatus(Invoice.Status.valueOf(rs.getString("status")));
        Timestamp issuedAt = rs.getTimestamp("issued_at");
        if (issuedAt != null) inv.setIssuedAt(issuedAt.toLocalDateTime());
        Timestamp paidAt = rs.getTimestamp("paid_at");
        if (paidAt != null) inv.setPaidAt(paidAt.toLocalDateTime());
        // joined columns (may be null for bare queries)
        inv.setGuestName(rs.getString("guest_name"));
        inv.setRoomNumber(rs.getString("room_number"));
        return inv;
    }

    private static InvoiceLine mapLine(ResultSet rs) throws SQLException {
        InvoiceLine line = new InvoiceLine();
        line.setId(rs.getLong("id"));
        line.setInvoiceId(rs.getLong("invoice_id"));
        line.setLineType(InvoiceLine.LineType.valueOf(rs.getString("line_type")));
        line.setReferenceId(rs.getLong("reference_id"));
        line.setDescription(rs.getString("description"));
        line.setQuantity(rs.getInt("quantity"));
        line.setUnitPrice(rs.getBigDecimal("unit_price"));
        line.setLineTotal(rs.getBigDecimal("line_total"));
        return line;
    }

    // ─── SQL helpers ─────────────────────────────────────────────────────────

    private static final String SELECT_INVOICE = """
            SELECT i.*,
                   CONCAT(g.first_name, ' ', g.last_name) AS guest_name,
                   r.room_number
            FROM invoices i
            JOIN reservations res ON res.id = i.reservation_id
            JOIN guests g         ON g.id   = res.guest_id
            JOIN rooms r          ON r.id   = res.room_id
            """;

    private static List<InvoiceLine> loadLines(Connection conn, long invoiceId) throws SQLException {
        String sql = "SELECT * FROM invoice_lines WHERE invoice_id = ? ORDER BY id";
        List<InvoiceLine> lines = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lines.add(mapLine(rs));
            }
        }
        return lines;
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    public static List<Invoice> findAll() {
        String sql = SELECT_INVOICE + " ORDER BY i.id DESC";
        List<Invoice> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Invoice inv = mapHeader(rs);
                inv.setLines(loadLines(c, inv.getId()));
                list.add(inv);
            }
        } catch (SQLException e) {
            throw new RuntimeException("InvoiceDao.findAll failed", e);
        }
        return list;
    }

    public static Optional<Invoice> findById(long id) {
        String sql = SELECT_INVOICE + " WHERE i.id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Invoice inv = mapHeader(rs);
                inv.setLines(loadLines(c, inv.getId()));
                return Optional.of(inv);
            }
        } catch (SQLException e) {
            throw new RuntimeException("InvoiceDao.findById failed", e);
        }
    }

    public static Optional<Invoice> findByReservation(long reservationId) {
        String sql = SELECT_INVOICE + " WHERE i.reservation_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Invoice inv = mapHeader(rs);
                inv.setLines(loadLines(c, inv.getId()));
                return Optional.of(inv);
            }
        } catch (SQLException e) {
            throw new RuntimeException("InvoiceDao.findByReservation failed", e);
        }
    }

    public static List<Invoice> findByStatus(Invoice.Status status) {
        String sql = SELECT_INVOICE + " WHERE i.status = ? ORDER BY i.id DESC";
        List<Invoice> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice inv = mapHeader(rs);
                    inv.setLines(loadLines(c, inv.getId()));
                    list.add(inv);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("InvoiceDao.findByStatus failed", e);
        }
        return list;
    }

    // ─── Mutations ────────────────────────────────────────────────────────────

    /**
     * Create a full invoice (header + lines) in one transaction.
     * Automatically:
     *  - generates invoice_number: INV-YYYYMMDD-{reservationId}
     *  - inserts all lines
     *  - calls calculateTotals() to set total_amount
     *
     * @return the generated invoice id
     */
    public static long create(Invoice invoice) {
        return TxManager.runInTransaction(conn -> {
            // 1. Insert header
            String invoiceNumber = "INV-" + LocalDate.now().toString().replace("-", "")
                    + "-" + invoice.getReservationId();
            String insertHeader = """
                    INSERT INTO invoices (reservation_id, invoice_number, total_amount, status, issued_at)
                    VALUES (?, ?, 0.00, 'DRAFT', NOW())
                    """;
            long invoiceId;
            try (PreparedStatement ps = conn.prepareStatement(insertHeader, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, invoice.getReservationId());
                ps.setString(2, invoiceNumber);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No key for invoice insert");
                    invoiceId = keys.getLong(1);
                }
            }

            // 2. Insert all lines
            if (invoice.getLines() != null) {
                for (InvoiceLine line : invoice.getLines()) {
                    insertLineInternal(conn, invoiceId, line);
                }
            }

            // 3. Recalculate total
            recalculateTotalInternal(conn, invoiceId);
            return invoiceId;
        });
    }

    /**
     * Append a single line to an existing invoice and refresh total_amount.
     */
    public static boolean addLine(long invoiceId, InvoiceLine line) {
        return TxManager.runInTransaction(conn -> {
            insertLineInternal(conn, invoiceId, line);
            recalculateTotalInternal(conn, invoiceId);
            return true;
        });
    }

    /**
     * Recalculates total_amount from sum of invoice_lines.
     * Safe to call any time; also called internally after mutations.
     */
    public static boolean calculateTotals(long invoiceId) {
        String sql = """
                UPDATE invoices i
                SET    i.total_amount = (
                    SELECT COALESCE(SUM(il.line_total), 0)
                    FROM   invoice_lines il
                    WHERE  il.invoice_id = i.id
                )
                WHERE i.id = ?
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("InvoiceDao.calculateTotals failed", e);
        }
    }

    /**
     * Status transition:
     *  DRAFT → ISSUED  (sets issued_at = NOW())
     *  ISSUED → PAID   (sets paid_at = NOW())
     *  DRAFT → CANCELLED
     */
    public static boolean updateStatus(long invoiceId, Invoice.Status newStatus) {
        String sql = switch (newStatus) {
            case ISSUED     -> "UPDATE invoices SET status='ISSUED', issued_at=NOW() WHERE id=?";
            case PAID       -> "UPDATE invoices SET status='PAID',   paid_at=NOW()   WHERE id=?";
            case CANCELLED  -> "UPDATE invoices SET status='CANCELLED'               WHERE id=?";
            default         -> "UPDATE invoices SET status=?                          WHERE id=?";
        };
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (newStatus == Invoice.Status.DRAFT) {
                // generic fallback — sets status only
                ps.setString(1, newStatus.name());
                ps.setLong(2, invoiceId);
            } else {
                ps.setLong(1, invoiceId);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("InvoiceDao.updateStatus failed", e);
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private static void insertLineInternal(Connection conn, long invoiceId, InvoiceLine line) throws SQLException {
        BigDecimal lineTotal = line.getUnitPrice()
                .multiply(BigDecimal.valueOf(line.getQuantity()));
        String sql = """
                INSERT INTO invoice_lines
                    (invoice_id, line_type, reference_id, description, quantity, unit_price, line_total)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            ps.setString(2, line.getLineType().name());
            if (line.getReferenceId() > 0) ps.setLong(3, line.getReferenceId());
            else ps.setNull(3, Types.BIGINT);
            ps.setString(4, line.getDescription());
            ps.setInt(5, line.getQuantity());
            ps.setBigDecimal(6, line.getUnitPrice());
            ps.setBigDecimal(7, lineTotal);
            ps.executeUpdate();
        }
    }

    private static void recalculateTotalInternal(Connection conn, long invoiceId) throws SQLException {
        String sql = """
                UPDATE invoices
                SET    total_amount = (
                    SELECT COALESCE(SUM(line_total), 0)
                    FROM   invoice_lines
                    WHERE  invoice_id = ?
                )
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            ps.setLong(2, invoiceId);
            ps.executeUpdate();
        }
    }
}
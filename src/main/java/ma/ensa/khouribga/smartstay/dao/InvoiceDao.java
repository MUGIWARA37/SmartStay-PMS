package ma.ensa.khouribga.smartstay.dao;

import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.db.TxManager;
import ma.ensa.khouribga.smartstay.model.Invoice;
import ma.ensa.khouribga.smartstay.model.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvoiceDao {

    private static Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setId(rs.getInt("id"));
        inv.setReservationId(rs.getInt("reservation_id"));
        inv.setInvoiceNumber(rs.getString("invoice_number"));
        Timestamp issued = rs.getTimestamp("issued_at");
        if (issued != null) inv.setIssuedAt(issued.toLocalDateTime());
        inv.setSubtotalAmount(rs.getDouble("subtotal_amount"));
        inv.setTaxAmount(rs.getDouble("tax_amount"));
        inv.setTotalAmount(rs.getDouble("total_amount"));
        inv.setStatus(Invoice.Status.valueOf(rs.getString("status")));
        inv.setNotes(rs.getString("notes"));
        return inv;
    }

    public static Optional<Invoice> findByReservation(int reservationId) throws SQLException {
        String sql = """
                SELECT id, reservation_id, invoice_number, issued_at,
                       subtotal_amount, tax_amount, total_amount, status, notes
                FROM invoices WHERE reservation_id = ? LIMIT 1
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Invoice inv = mapRow(rs);
                // load lines
                inv.getLines().addAll(findLines(conn, inv.getId()));
                return Optional.of(inv);
            }
        }
    }

    private static List<Invoice.InvoiceLine> findLines(Connection conn, int invoiceId) throws SQLException {
        List<Invoice.InvoiceLine> lines = new ArrayList<>();
        String sql = """
                SELECT id, invoice_id, line_type, reference_id, description, quantity, unit_price, line_total
                FROM invoice_lines WHERE invoice_id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice.InvoiceLine line = new Invoice.InvoiceLine();
                    line.setId(rs.getInt("id"));
                    line.setInvoiceId(rs.getInt("invoice_id"));
                    line.setLineType(Invoice.InvoiceLine.LineType.valueOf(rs.getString("line_type")));
                    int refId = rs.getInt("reference_id");
                    line.setReferenceId(rs.wasNull() ? null : refId);
                    line.setDescription(rs.getString("description"));
                    line.setQuantity(rs.getInt("quantity"));
                    line.setUnitPrice(rs.getDouble("unit_price"));
                    line.setLineTotal(rs.getDouble("line_total"));
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    /**
     * Creates an invoice with its lines and a payment record atomically.
     * @param reservationId  the reservation being paid
     * @param roomDescription  e.g. "Room 101 – 3 nights"
     * @param roomSubtotal   nights × pricePerNight
     * @param services       list of selected services (quantity > 0)
     * @param paymentMethod  CASH / CARD / TRANSFER
     * @param transactionRef optional transaction reference string
     * @return the generated invoice id
     */
    public static int createWithPayment(int reservationId,
                                        String roomDescription,
                                        double roomSubtotal,
                                        List<Service> services,
                                        String paymentMethod,
                                        String transactionRef) throws SQLException {
        return TxManager.runInTransaction(conn -> {
            // 1. Calculate totals
            double serviceSubtotal = services.stream().mapToDouble(Service::getLineTotal).sum();
            double subtotal = roomSubtotal + serviceSubtotal;
            double tax      = subtotal * 0.10;
            double total    = subtotal + tax;
            String invNumber = "INV-" + System.currentTimeMillis();

            // 2. Insert invoice
            String invSql = """
                    INSERT INTO invoices
                      (reservation_id, invoice_number, issued_at,
                       subtotal_amount, tax_amount, total_amount, status)
                    VALUES (?, ?, NOW(), ?, ?, ?, 'PAID')
                    """;
            int invoiceId;
            try (PreparedStatement ps = conn.prepareStatement(invSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, reservationId);
                ps.setString(2, invNumber);
                ps.setDouble(3, subtotal);
                ps.setDouble(4, tax);
                ps.setDouble(5, total);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No key for invoice insert");
                    invoiceId = keys.getInt(1);
                }
            }

            // 3. Insert room line
            insertLine(conn, invoiceId, Invoice.InvoiceLine.LineType.ROOM,
                    reservationId, roomDescription, 1, roomSubtotal);

            // 4. Insert service lines
            for (Service s : services) {
                if (s.getQuantity() <= 0) continue;
                insertLine(conn, invoiceId, Invoice.InvoiceLine.LineType.SERVICE,
                        s.getId(), s.getName(), s.getQuantity(), s.getUnitPrice());
            }

            // 5. Insert payment record
            String paySQL = """
                    INSERT INTO payments
                      (invoice_id, amount, method, paid_at, transaction_ref, status)
                    VALUES (?, ?, ?, NOW(), ?, 'SUCCESS')
                    """;
            try (PreparedStatement ps = conn.prepareStatement(paySQL)) {
                ps.setInt(1, invoiceId);
                ps.setDouble(2, total);
                ps.setString(3, paymentMethod);
                ps.setString(4, transactionRef == null || transactionRef.isBlank() ? null : transactionRef);
                ps.executeUpdate();
            }

            // 6. Mark reservation as CONFIRMED
            String updRes = "UPDATE reservations SET status='CONFIRMED', updated_at=NOW() WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(updRes)) {
                ps.setInt(1, reservationId);
                ps.executeUpdate();
            }

            return invoiceId;
        });
    }

    private static void insertLine(Connection conn, int invoiceId,
                                   Invoice.InvoiceLine.LineType type,
                                   int referenceId, String description,
                                   int quantity, double unitPrice) throws SQLException {
        String sql = """
                INSERT INTO invoice_lines
                  (invoice_id, line_type, reference_id, description, quantity, unit_price, line_total)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setString(2, type.name());
            ps.setInt(3, referenceId);
            ps.setString(4, description);
            ps.setInt(5, quantity);
            ps.setDouble(6, unitPrice);
            ps.setDouble(7, unitPrice * quantity);
            ps.executeUpdate();
        }
    }
}

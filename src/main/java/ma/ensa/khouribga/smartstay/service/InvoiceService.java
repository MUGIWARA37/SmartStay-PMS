package ma.ensa.khouribga.smartstay.service;

import ma.ensa.khouribga.smartstay.dao.InvoiceDao;
import ma.ensa.khouribga.smartstay.model.Invoice;
import java.util.List;
import java.util.Optional;

public class InvoiceService {

    public List<Invoice> getAllInvoices() {
        return InvoiceDao.findAll();
    }

    public Optional<Invoice> getInvoiceById(long id) {
        return InvoiceDao.findById(id);
    }

    public Optional<Invoice> getInvoiceByReservation(long reservationId) {
        return InvoiceDao.findByReservation(reservationId);
    }

    public List<Invoice> getInvoicesByStatus(Invoice.Status status) {
        return InvoiceDao.findByStatus(status);
    }

    public long createInvoice(Invoice invoice) {
        return InvoiceDao.create(invoice);
    }

    public boolean addInvoiceLine(long invoiceId, Invoice.InvoiceLine line) {
        return InvoiceDao.addLine(invoiceId, line);
    }

    public boolean calculateInvoiceTotals(long invoiceId) {
        return InvoiceDao.calculateTotals(invoiceId);
    }

    public boolean updateInvoiceStatus(long invoiceId, Invoice.Status newStatus) {
        return InvoiceDao.updateStatus(invoiceId, newStatus);
    }
}

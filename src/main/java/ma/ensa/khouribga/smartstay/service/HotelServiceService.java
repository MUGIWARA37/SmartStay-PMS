package ma.ensa.khouribga.smartstay.service;

import ma.ensa.khouribga.smartstay.dao.ServiceDao;
import ma.ensa.khouribga.smartstay.model.Service;
import java.util.List;
import java.util.Optional;

public class HotelServiceService {

    public List<Service> getAllServices() {
        return ServiceDao.findAll();
    }

    public List<Service> getActiveServices() {
        return ServiceDao.findActive();
    }

    public Optional<Service> getServiceById(long id) {
        return ServiceDao.findById(id);
    }

    public Optional<Service> getServiceByCode(String code) {
        return ServiceDao.findByCode(code);
    }

    public long createService(Service s) {
        return ServiceDao.create(s);
    }

    public boolean updateService(Service s) {
        return ServiceDao.update(s);
    }

    public boolean setServiceActive(long id, boolean active) {
        return ServiceDao.setActive(id, active);
    }
}

package ma.ensa.khouribga.smartstay.service;

import ma.ensa.khouribga.smartstay.dao.GuestDao;
import ma.ensa.khouribga.smartstay.model.Guest;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class GuestService {

    public List<Guest> getAllGuests() throws SQLException {
        return GuestDao.findAll();
    }

    public Optional<Guest> getGuestById(int id) throws SQLException {
        return GuestDao.findById(id);
    }

    public Optional<Guest> getGuestByPassport(String passportNumber) throws SQLException {
        return GuestDao.findByPassport(passportNumber);
    }

    public List<Guest> searchGuestsByName(String fragment) throws SQLException {
        return GuestDao.searchByName(fragment);
    }

    public int createGuest(Guest guest) throws SQLException {
        return GuestDao.create(guest);
    }

    public boolean updateGuest(Guest guest) throws SQLException {
        return GuestDao.update(guest);
    }
}

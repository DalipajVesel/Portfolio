import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class THImpl extends UnicastRemoteObject implements THInterface {

    private final String[] seatTypes = {"ΠΑ", "ΠΒ", "ΠΓ", "ΚΕ", "ΠΘ"};

    private final int[] availableSeats = {100, 200, 300, 250, 50};

    private final int[] seatPrice = {50, 40, 30, 35, 25};

    // Δημιουργούμε την κλάση Book για να κρατάμε τις κρατήσεις
    private class Book {
        String name;
        String type;
        int number;

        Book(String name, String type, int number) {
            this.name = name;
            this.type = type;
            this.number = number;
        }
    }

    private final List<Book> bookings = new ArrayList<>();

    private final List<CBInterface> paSubscribers = new ArrayList<>();
    private final List<CBInterface> pbSubscribers = new ArrayList<>();
    private final List<CBInterface> pgSubscribers = new ArrayList<>();
    private final List<CBInterface> keSubscribers = new ArrayList<>();
    private final List<CBInterface> pthSubscribers = new ArrayList<>();

    public THImpl() throws RemoteException {
        super();
    }


    // Μέθοδος για να βρούμε το νούμερο του τύπου θέσης
    @Override
    public int getSeatsByType(String type) {

        for (int i = 0; i < seatTypes.length; i++)
            if (seatTypes[i].equals(type))
                return i;

        return -1;
    }

    // Μέθοδος για να πάρουμε τον αριθμό των θέσεων που υπάρχουν για κάθε τύπο
    @Override
    public synchronized int[] getAvailableSeats() throws RemoteException {
        int[] seats = new int[seatTypes.length];

        for (int i = 0; i < seatTypes.length; i++)
            seats[i] = availableSeats[i];

        return seats;
    }

    // Με αυτή τη μέθοδο κάνουμε μία κράτηση
    @Override
    public synchronized int bookSeat(String type, int number, String name) throws RemoteException {
        int seatTypeIndex = getSeatsByType(type);

        if (seatTypeIndex < 0 || number <= 0 || availableSeats[seatTypeIndex] < number)
            return -1;

        // Αφαιρούμε τις θέσεις που έγινε η κράτηση από τις διαθέσιμες
        availableSeats[seatTypeIndex] -= number;

        // Ελέγχουμε αν υπάρχει ήδη κράτηση για τον ίδιο τύπο και όνομα 
        for (Book booking : bookings)
            if (booking.name.equals(name) && booking.type.equals(type)) {
                booking.number += number;
                return seatPrice[seatTypeIndex] * number;
            }

        bookings.add(new Book(name, type, number));

        return seatPrice[seatTypeIndex] * number;
    }

    // Μέθοδος για να πάρουμε όλες τις κρατήσεις
    @Override
    public synchronized List<String> getGuests() throws RemoteException {
        List<String> guests = new ArrayList<>();
        List<String> booked = new ArrayList<>();

        for (Book booking1 : bookings) {
            // Ελέγχουμε αν το όνομα έχει ήδη προστεθεί για να μην το προσθέσουμε ξανά
            if (booked.contains(booking1.name))
                continue;

            booked.add(booking1.name);

            int total = 0;
            StringBuilder sb = new StringBuilder();
            boolean firstItem = true;

            // Για κάθε κράτηση του ίδιου ονόματος, προσθέτουμε τις θέσεις
            for (Book booking2 : bookings) {
                if (!booking2.name.equals(booking1.name))
                    continue;

                int seatTypeIndex = getSeatsByType(booking2.type);
                int cost = seatPrice[seatTypeIndex] * booking2.number;
                total += cost;

                if (!firstItem) {
                    sb.append(", ");
                }
                sb.append(booking2.number).append(" ").append(booking2.type);
                firstItem = false;
            }

            guests.add(booking1.name);
            guests.add(sb.toString());
            guests.add("Συνολικό κόστος: " + total + "€");
            guests.add("");
        }

        return guests;
    }

    @Override
    public List<String> cancelSeat(String type, int number, String name) throws RemoteException {
        List<String> remainingGuests = new ArrayList<>();
        List<CBInterface> toNotify = null;
        int nowAvailable = 0;

        synchronized (this) {
            int seatTypeIndex = getSeatsByType(type);

            if (seatTypeIndex < 0 || number <= 0)
                return remainingGuests;

            Book bookingToCancel = null;
            boolean cancelled = false;

            for (Book booking : bookings) {
                if (booking.name.equals(name) && booking.type.equals(type)) {
                    if (booking.number < number)
                        return remainingGuests;

                    booking.number -= number;
                    availableSeats[seatTypeIndex] += number;

                    if (booking.number == 0)
                        bookingToCancel = booking;

                    cancelled = true;
                    break;
                }
            }
            if (bookingToCancel != null)
                bookings.remove(bookingToCancel);

            if (cancelled) {
                nowAvailable = availableSeats[seatTypeIndex];

                switch (seatTypeIndex) {
                    case 0:
                        toNotify = paSubscribers;
                        break;
                    case 1:
                        toNotify = pbSubscribers;
                        break;
                    case 2:
                        toNotify = pgSubscribers;
                        break;
                    case 3:
                        toNotify = keSubscribers;
                        break;
                    case 4:
                        toNotify = pthSubscribers;
                        break;
                    default:
                        return remainingGuests;
                }
            }

            for (Book booking : bookings) {
                if (booking.name.equals(name)) {
                    remainingGuests.add(
                            booking.name + " έχει " +
                                    booking.number + " θέσεις τύπου " +
                                    booking.type
                    );
                }
            }

            if (cancelled && remainingGuests.isEmpty())
                remainingGuests.add(name + " δεν έχει άλλες κρατήσεις");
        }

        // Στέλνουμε ειδοποίηση στους συνδρομητές έξω από το synchronized,
        // αλλιώς ο server και ο client περιμένουν ο ένας τον άλλον
        if (toNotify != null) {
            for (CBInterface subscriber : new ArrayList<>(toNotify)) {
                try {
                    subscriber.notification(type, nowAvailable);
                } catch (RemoteException e) {
                    synchronized (this) {
                        toNotify.remove(subscriber);
                    }
                }
            }
        }

        return remainingGuests;
    }

    @Override
    public synchronized void registerNotification(String type, CBInterface callback) throws RemoteException {
        int seatTypeIndex = getSeatsByType(type);
        switch (seatTypeIndex) {
            case 0:
                if (!paSubscribers.contains(callback)) {
                    paSubscribers.add(callback);
                }
                break;

            case 1:
                if (!pbSubscribers.contains(callback)) {
                    pbSubscribers.add(callback);
                }
                break;

            case 2:
                if (!pgSubscribers.contains(callback)) {
                    pgSubscribers.add(callback);
                }
                break;

            case 3:
                if (!keSubscribers.contains(callback)) {
                    keSubscribers.add(callback);
                }
                break;

            case 4:
                if (!pthSubscribers.contains(callback)) {
                    pthSubscribers.add(callback);
                }
                break;

            default:
                throw new RemoteException("Λάθος τύπος θέσεων: " + type);
        }
    }

    @Override
    public synchronized void unregisterNotification(String type, CBInterface callback) throws RemoteException {
        int seatTypeIndex = getSeatsByType(type);
        switch (seatTypeIndex) {
            case 0:
                paSubscribers.remove(callback);
                break;

            case 1:
                pbSubscribers.remove(callback);
                break;

            case 2:
                pgSubscribers.remove(callback);
                break;

            case 3:
                keSubscribers.remove(callback);
                break;

            case 4:
                pthSubscribers.remove(callback);
                break;

            default:
                throw new RemoteException("Λάθος τύπος θέσεων: " + type);
        }
    }
}
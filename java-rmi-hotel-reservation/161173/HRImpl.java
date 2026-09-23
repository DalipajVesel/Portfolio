import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class HRImpl extends UnicastRemoteObject implements HRInterface {
    private Map<String, Integer> rooms;
    private Map<String, Integer> roomPrices;
    private Map<String, Map<String, Integer>> bookings;
    private Map<String, List<String>> notificationLists;

    protected HRImpl() throws RemoteException {
        super();

        // Initializing room types
        rooms = new HashMap<>();
        rooms.put("A", 40);
        rooms.put("B", 35);
        rooms.put("C", 25);
        rooms.put("D", 30);
        rooms.put("E", 20);

        // Initialize room prices
        roomPrices = new HashMap<>();
        roomPrices.put("A", 75);
        roomPrices.put("B", 110);
        roomPrices.put("C", 120);
        roomPrices.put("D", 150);
        roomPrices.put("E", 200);

        bookings = new HashMap<>();
        notificationLists = new HashMap<>();
        for (String type : rooms.keySet()) {
            bookings.put(type, new HashMap<>());
            notificationLists.put(type, new ArrayList<>());
        }
    }

    @Override
    public synchronized Map<String, Integer> listRooms() throws RemoteException {
        return new HashMap<>(rooms);
    }

    @Override
    public synchronized String bookRooms(String type, int number, String name) throws RemoteException {
        if (!rooms.containsKey(type) || number <= 0) {
            return "Wrong room type or number of rooms.";
        }
        if (rooms.containsKey(type) && rooms.get(type) >= number) {
            rooms.put(type, rooms.get(type) - number);
            bookings.get(type).put(name, bookings.get(type).getOrDefault(name, 0) + number);
            int totalCost = number * roomPrices.get(type);
            return "Booking successful. Total cost: " + totalCost + " Euros.";
        } else {
            if (rooms.get(type) > 0) {
                return "Not enough rooms available. Only " + rooms.get(type) + " rooms left.";
            } else {
                return "No rooms available.";
            }
        }
    }

    @Override
    public synchronized List<String> listGuests() throws RemoteException {
        List<String> guestList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : bookings.entrySet()) {
            for (Map.Entry<String, Integer> booking : entry.getValue().entrySet()) {
                guestList.add("Guest: " + booking.getKey() + ", Type: " + entry.getKey() + ", Rooms: " + booking.getValue());
            }
        }
        return guestList;
    }

    @Override
    public synchronized String cancelBooking(String type, int number, String name) throws RemoteException {
        if (number <= 0) {
            return "Wrong number of rooms.";
        }
        if (bookings.containsKey(type) && bookings.get(type).containsKey(name)) {
            int bookedRooms = bookings.get(type).get(name);
            if (bookedRooms >= number) {
                bookings.get(type).put(name, bookedRooms - number);
                rooms.put(type, rooms.get(type) + number);
                if (bookedRooms == number) {
                    bookings.get(type).remove(name);
                }
                notifyClients(type);
                return "Cancellation successful.";
            } else {
                return "Not enough rooms booked to cancel.";
            }
        } else {
            return "No bookings found for cancellation.";
        }
    }

    @Override
    public synchronized void registerForNotification(String type, String name) throws RemoteException {
        if (notificationLists.containsKey(type)) {
            notificationLists.get(type).add(name);
        }
    }

    // Notifies registered clients when rooms are available
    private synchronized void notifyClients(String type) throws RemoteException {
        List<String> clientsToNotify = notificationLists.get(type);
        for (String client : clientsToNotify) {
            System.out.println("Notification: Room type " + type + " is now available. Notifying " + client);
        }
        notificationLists.get(type).clear();
    }
}

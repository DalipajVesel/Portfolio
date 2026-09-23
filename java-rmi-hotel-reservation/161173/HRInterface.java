import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface HRInterface extends Remote {
    // List all available rooms
    Map<String, Integer> listRooms() throws RemoteException;

    // Books a specified number of rooms of a given type
    String bookRooms(String type, int number, String name) throws RemoteException;

    // Lists all guests currently booked in the hotel
    List<String> listGuests() throws RemoteException;

    // Cancels a booking
    String cancelBooking(String type, int number, String name) throws RemoteException;

    // Registers a guest for notification when rooms become available again
    void registerForNotification(String type, String name) throws RemoteException;
}

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface THInterface extends Remote {

    int getSeatsByType(String type) throws RemoteException;

    int[] getAvailableSeats() throws RemoteException;

    int bookSeat(String type, int number, String name) throws RemoteException;

    List<String> getGuests() throws RemoteException;

    List<String> cancelSeat(String type, int number, String name) throws RemoteException;

    void registerNotification(String type, CBInterface callback) throws RemoteException;

    void unregisterNotification(String type, CBInterface callback) throws RemoteException;
}
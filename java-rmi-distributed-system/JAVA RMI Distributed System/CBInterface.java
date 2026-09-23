import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CBInterface extends Remote {

    void notification(String type, int number) throws RemoteException;

}

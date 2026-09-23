import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class CBImpl extends UnicastRemoteObject implements CBInterface {

    private final THInterface th;

    public CBImpl(String clientName, THInterface th) throws RemoteException {
        super();
        this.th = th;
    }

    @Override
    public void notification(String type, int number) throws RemoteException {
        System.out.println("Υπάρχουν πλέον " + number + " διαθέσιμες θέσεις για τον τύπο " + type + ".");

        th.unregisterNotification(type, this);
    }
}

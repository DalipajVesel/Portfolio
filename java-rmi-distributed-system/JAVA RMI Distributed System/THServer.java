import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class THServer {

    public static void main(String[] args) {

        try {

            THImpl th = new THImpl();

            LocateRegistry.createRegistry(1099);

            Naming.rebind("rmi://localhost:1099/TH", th);
            System.out.println("Server is ready");

        } catch (Exception e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

}

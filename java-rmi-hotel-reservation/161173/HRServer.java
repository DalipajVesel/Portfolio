import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class HRServer {
    public static void main(String[] args) {

        try {
            // Create an RMI registry on port 1099 which is the default
            LocateRegistry.createRegistry(1099);

            HRImpl impl = new HRImpl();

            // Bind the interface implementation to the RMI registry
            Naming.rebind("//localhost/HRService", impl);
        } catch (Exception e) {
            // Print any errors
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

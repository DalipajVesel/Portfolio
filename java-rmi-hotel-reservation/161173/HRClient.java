import java.rmi.Naming;
import java.util.Map;
import java.util.Scanner;

public class HRClient {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("java HRClient list <hostname>");
            System.out.println("java HRClient book <hostname> <type> <number> <name>");
            System.out.println("java HRClient guests <hostname>");
            System.out.println("java HRClient cancel <hostname> <type> <number> <name>");
            System.exit(0);
        }

        try {
            // Look for the RMI implementation in the RMI registry
            HRInterface inter = (HRInterface) Naming.lookup("//" + args[1] + "/HRService");
            Scanner scanner = new Scanner(System.in);

            // Switch to determine what the user asked
            switch (args[0]) {
                case "list":
                    Map<String, Integer> rooms = inter.listRooms();
                    for (Map.Entry<String, Integer> entry : rooms.entrySet()) {
                        String roomType = entry.getKey();
                        int quantity = entry.getValue();
                        int price = 0;
                        switch (roomType) {
                            case "A":
                                price = 75;
                                break;
                            case "B":
                                price = 110;
                                break;
                            case "C":
                                price = 120;
                                break;
                            case "D":
                                price = 150;
                                break;
                            case "E":
                                price = 200;
                                break;
                        }
                        System.out.println(quantity + " rooms of type " + roomType + " - price: " + price + " Euros per night");
                    }
                    break;
                case "book":
                    if (args.length < 5) {
                        System.out.println("Usage: java HRClient book <hostname> <type> <number> <name>");
                    } else {
                        String result = inter.bookRooms(args[2], Integer.parseInt(args[3]), args[4]);
                        System.out.println(result);
                        if (result.contains("Not enough rooms available") || result.contains("No rooms available")) {
                            System.out.println("Do you want to register for notification when rooms become available? (yes/no)");
                            String response = scanner.nextLine();
                            if (response.equalsIgnoreCase("yes")) {
                                inter.registerForNotification(args[2], args[4]);
                                System.out.println("You have been registered for notifications.");
                            }
                        }
                    }
                    break;
                case "guests":
                    System.out.println(inter.listGuests());
                    break;
                case "cancel":
                    if (args.length < 5) {
                        System.out.println("Usage: java HRClient cancel <hostname> <type> <number> <name>");
                    } else {
                        System.out.println(inter.cancelBooking(args[2], Integer.parseInt(args[3]), args[4]));
                    }
                    break;
                default:
                    System.out.println("Unknown command");
                    break;
            }
        } catch (Exception e) {
            // Print any errors
            System.err.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

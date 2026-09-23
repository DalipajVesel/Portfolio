// THClient.java

import java.rmi.Naming;
import java.util.List;
import java.util.Scanner;

public class THClient {

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("java THClient list <hostname>");
                System.out.println("java THClient book <hostname> <type> <number> <name>");
                System.out.println("java THClient guests <hostname>");
                System.out.println("java THClient cancel <hostname> <type> <number> <name>");
                return;
            }

            String command = args[0];
            String hostname = args[1];
            THInterface th = (THInterface) Naming.lookup("rmi://" + hostname + "/TH");
            Scanner scanner = new Scanner(System.in);

            switch (command) {
                case "list": {
                    if (args.length != 2) {
                        System.out.println("java THClient list <hostname>");
                        return;
                    }
                    int[] available = th.getAvailableSeats();
                    System.out.println(available[0] + " θέσεις Πλατεία - Ζώνη Α (κωδικός: ΠΑ) - τιμή: 50 Ευρώ");
                    System.out.println(available[1] + " θέσεις Πλατεία - Ζώνη Β (κωδικός: ΠΒ) - τιμή: 40 Ευρώ");
                    System.out.println(available[2] + " θέσεις Πλατεία - Ζώνη Γ (κωδικός: ΠΓ) - τιμή: 30 Ευρώ");
                    System.out.println(available[3] + " θέσεις Κεντρικός Εξώστης (κωδικός: ΚΕ) - τιμή: 35 Ευρώ");
                    System.out.println(available[4] + " θέσεις Πλαϊνά Θεωρεία (κωδικός: ΠΘ) - τιμή: 25 Ευρώ");
                    break;
                }

                case "book": {
                    if (args.length != 5) {
                        System.out.println("java THClient book <hostname> <type> <number> <name>");
                        return;
                    }

                    String type = args[2];
                    int number = Integer.parseInt(args[3]);
                    String name = args[4];
                    int cost = th.bookSeat(type, number, name);

                    if (cost < 0) {
                        System.out.println("Δεν υπάρχουν αρκετές διαθέσιμες θέσεις για τον τύπο " + type + ".");

                        int[] available = th.getAvailableSeats();
                        int seatTypeIndex = th.getSeatsByType(type);
                        if (seatTypeIndex < 0) {
                            System.out.println("Ο τύπος " + type + " δεν είναι έγκυρος.");
                            return;
                        }

                        int remainingSeats = available[seatTypeIndex];
                        if (remainingSeats > 0) {
                            System.out.println("Υπάρχουν " + remainingSeats + " διαθέσιμες θέσεις για τον τύπο " + type + ".");
                            System.out.print("Θέλετε να κλείσετε αυτές τις " + remainingSeats + " θέσεις; (y/n): ");
                            String response1 = scanner.nextLine();
                            if (response1.equalsIgnoreCase("y")) {
                                int cost2 = th.bookSeat(type, remainingSeats, name);
                                if (cost2 < 0) {
                                    System.out.println("Αδυναμία κράτησης. Κάποιος πρόλαβε.");
                                } else {
                                    System.out.println("Η κράτηση για " + remainingSeats + " θέσεις τύπου " + type + " στο όνομα " + name + " έγινε με επιτυχία. Κόστος: " + cost2 + "€");
                                }
                                break;
                            }
                        } else {
                            System.out.println("Δεν υπάρχουν διαθέσιμες θέσεις για τον τύπο " + type + ".");
                        }
                        System.out.print("Θέλετε να μπείτε στη λίστα αναμονής για ειδοποίηση; (y/n): ");
                        String response2 = scanner.nextLine();
                        if (response2.equalsIgnoreCase("y")) {
                            CBImpl callback = new CBImpl(name, th);
                            th.registerNotification(type, callback);
                            System.out.println("Έγινε εγγραφή στη λίστα αναμονής για τύπο " + type + ".");
                        } else {
                            System.out.println("Δεν θα λάβετε ειδοποίηση.");
                        }
                    } else {
                        System.out.println("Η κράτηση για " + number + " θέσεις τύπου " +
                                type + " στο όνομα " + name + " έγινε με επιτυχία. Κόστος: " + cost + "€");
                    }
                    break;
                }

                case "guests": {
                    if (args.length != 2) {
                        System.out.println("java THClient guests <hostname>");
                        return;
                    }
                    List<String> guests = th.getGuests();
                    System.out.println("Λίστα Καλεσμένων:");
                    for (String line : guests) {
                        System.out.println(line);
                    }
                    break;
                }

                case "cancel": {
                    if (args.length != 5) {
                        System.out.println("java THClient cancel <hostname> <type> <number> <name>");
                        return;
                    }
                    String type = args[2];
                    int number = Integer.parseInt(args[3]);
                    String name = args[4];
                    List<String> remaining = th.cancelSeat(type, number, name);

                    if (remaining.isEmpty()) {
                        System.out.println("Ακύρωση αποτυχημένη ή δεν υπήρχαν θέσεις για " + name + ".");
                    } else {
                        System.out.println("Μετά την ακύρωση, ο/η " + name + " έχει τις εξής κρατήσεις:");
                        for (String line : remaining) {
                            System.out.println("  " + line);
                        }
                    }
                    break;
                }

                default:
                    System.out.println("Άγνωστη εντολή: " + command);
                    break;
            }

        } catch (Exception e) {
            System.err.println("THClient exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

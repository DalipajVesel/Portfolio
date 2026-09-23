# Java RMI Hotel Reservation

Hotel booking over Java RMI. The server keeps the rooms per type, the prices
and the bookings in memory. Clients list the rooms, book, cancel and see the
guest list. When a booking fails, the client can leave its name on a waiting
list, and when rooms of that type are cancelled the server prints who is
waiting for them.

Lab assignment 2, Distributed Systems, 2023-24.

Compile with javac *.java inside 161173, start HRServer, then run the client:

    java HRClient list   <hostname>
    java HRClient book   <hostname> <type> <number> <name>
    java HRClient guests <hostname>
    java HRClient cancel <hostname> <type> <number> <name>

Room types are A to E. HRServer starts the registry on port 1099.

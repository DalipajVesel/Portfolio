# Java RMI Distributed System

Seat reservations for a theatre over Java RMI. Clients list the seats, book,
cancel and see the guest list. When fewer seats are left than the client
asked for, it can take the ones that are left or join a waitlist: the server
keeps an RMI reference to it and calls it back when seats free up, so the
client is a remote object too.

Lab assignment 2, Distributed Systems, 2024-25.

## Build and run

    javac *.java

    java THServer
    java THClient list   localhost
    java THClient book   localhost ΠΑ 4 Vesel
    java THClient guests localhost
    java THClient cancel localhost ΠΑ 2 Vesel

THServer starts the RMI registry on port 1099. The seat codes are ΠΑ, ΠΒ,
ΠΓ, ΚΕ and ΠΘ.

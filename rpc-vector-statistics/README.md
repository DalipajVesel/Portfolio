# RPC Vector Statistics

Three programs in C. The client sends a vector of integers over a TCP socket
to the server, which is also an RPC client and passes the vector to the RPC
server. Operations: the mean, the min and max, and the vector multiplied by a
number. The vector is sent once and can be used for many operations.

Lab assignment 1, Distributed Systems, 2023-24.

## Build and run

Needs rpcgen, libtirpc-dev and the rpcbind portmapper running.

    make
    gcc -o client client.c
    sudo rpcbind

    ./rpc_option_server &
    ./server 5555
    ./client localhost 5555

server takes the port, and the RPC host as an optional second argument.

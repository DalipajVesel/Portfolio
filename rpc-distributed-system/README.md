# RPC Distributed System

Three programs in C. A socket client talks over TCP to a proxy server, which
forwards every request over ONC RPC to the computation server. The proxy
forks one child per client, so several clients can be served at the same
time. Operations: dot product, mean values and scalar-vector product.

    ds_socket_client  --TCP-->  ds_client (proxy)  --ONC RPC-->  ds_server

Lab assignment 1, Distributed Systems, 2024-25.

## Build and run

Needs rpcgen, libtirpc-dev and the rpcbind portmapper running.

    make -f Makefile.ds
    gcc -o ds_socket_client ds_socket_client.c
    sudo rpcbind

    ./ds_server &
    ./ds_client &
    ./ds_socket_client

ds_client is the proxy and listens on port 12345. Makefile.ds runs rpcgen
on ds.x first.

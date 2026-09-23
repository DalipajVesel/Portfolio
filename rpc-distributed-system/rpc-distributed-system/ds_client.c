#include "ds.h"
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>

#define PORT 12345
#define RPC_HOST "localhost"

 
//Handles a connected client by processing requests and forwarding them to the RPC server
static void handle_client(int fd) {
    CLIENT *clnt;
    
    while (1) {
        // Read operation code from client
        uint32_t net_op;
        if (read(fd, &net_op, sizeof(net_op)) != sizeof(net_op))
            break;
        
        int op = ntohl(net_op);
        if (op == 0)
            break; 
        
        // Create connection to RPC server
        clnt = clnt_create(RPC_HOST, DS_PROG, DS_VERS, "udp");
        if (!clnt) {
            clnt_pcreateerror(RPC_HOST);
            break;
        }
        
        // Handle dot product or mean calculation
        if (op == 1 || op == 2) {
            // Read vector size
            uint32_t net_n;
            read(fd, &net_n, sizeof(net_n));
            u_int n = ntohl(net_n);
            
            vectors args;
            args.n = n;
            
            args.x.x_len = n; args.x.x_val = malloc(n * sizeof(int));
            args.y.y_len = n; args.y.y_val = malloc(n * sizeof(int));
            
            for (u_int i = 0; i < n; i++) {
                uint32_t tmp; 
                read(fd, &tmp, sizeof(tmp));
                args.x.x_val[i] = ntohl(tmp);
            }
            
            for (u_int i = 0; i < n; i++) {
                uint32_t tmp; 
                read(fd, &tmp, sizeof(tmp));
                args.y.y_val[i] = ntohl(tmp);
            }
            
            if (op == 1) {
                // Dot Product operation
                int *res = dot_product_calculator_1(&args, clnt);
                uint32_t out = htonl(res ? *res : 0);
                write(fd, &out, sizeof(out));
            } else {
                // Mean calculation operation
                mean_result *mr = mean_calculator_1(&args, clnt);
                write(fd, &mr->mean_x, sizeof(double));
                write(fd, &mr->mean_y, sizeof(double));
            }
            
            free(args.x.x_val);
            free(args.y.y_val);
        } 
        // Handle scalar product
        else if (op == 3) {
            uint32_t net_n;
            read(fd, &net_n, sizeof(net_n));
            u_int n = ntohl(net_n);

            double r;
            read(fd, &r, sizeof(r));

            product_vector args;
            args.n = n; 
            args.r = r;

            args.x.x_len = n; 
            args.x.x_val = malloc(n * sizeof(int));

            for (u_int i = 0; i < n; i++) {
                uint32_t tmp; 
                read(fd, &tmp, sizeof(tmp));
                args.x.x_val[i] = ntohl(tmp);
            }
            
            // Call RPC function
            product_result *pr = product_calculator_1(&args, clnt);
            
            // Send result size
            uint32_t out_n = htonl(pr ? pr->n : 0);
            write(fd, &out_n, sizeof(out_n));
            
            // Send result values
            for (u_int i = 0; i < (pr ? pr->values.values_len : 0); i++)
                write(fd, &pr->values.values_val[i], sizeof(double));

            free(args.x.x_val);
        }
        
        // Cleanup RPC client
        clnt_destroy(clnt);
    }
    
    // Close client connection
    close(fd);
}

//Creates socket and handles client connections
int main(void) {
    int listen_fd = socket(AF_INET, SOCK_STREAM, 0);
    
    // Setup server address
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family      = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port        = htons(PORT);
    
    // Bind socket to address
    bind(listen_fd, (struct sockaddr *)&addr, sizeof(addr));
    
    // Listen for connections
    listen(listen_fd, 5);
    
    printf("RPC Proxy Server started on port %d\n", PORT);
    printf("Forwarding requests to RPC server at %s\n", RPC_HOST);
    
    // Main server loop
    while (1) {
        // Accept client connection
        int fd = accept(listen_fd, NULL, NULL);
        if (fd < 0)
            continue;
        
        // Fork child process to handle client
        if (fork() == 0) {
            close(listen_fd);  // Close listening socket in child
            handle_client(fd);
            exit(0);
        }
        
        // Close client socket in parent
        close(fd);
    }
    
    return 0;
}
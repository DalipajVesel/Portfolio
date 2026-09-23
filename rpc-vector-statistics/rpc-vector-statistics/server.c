#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include "rpc_option.h"

// Function to handle errors
void error(const char *msg) {
    perror(msg);
    exit(1);
}

int main(int argc, char *argv[]) {
    int server_socket, client_socket, port_number, n, i, option;
    int min_max[2];
    float a;
    int *Y;
    struct sockaddr_in server_address, client_address;
    socklen_t clilen;
    CLIENT *clnt;
    char *host = "localhost";
    data rpc_data;
    product_data rpc_product_data;
    float *result;
    int_array *result_array;
    float_array *result_farray;

    // Check if the port number is provided, the RPC host is optional
    if (argc < 2) {
        fprintf(stderr, "Usage: %s port [rpc_host]\n", argv[0]);
        exit(1);
    }
    if (argc >= 3)
        host = argv[2];

    // Convert port number from string to integer
    port_number = atoi(argv[1]);

    // Create a socket
    server_socket = socket(AF_INET, SOCK_STREAM, 0);
    if (server_socket < 0) {
        error("Socket creation failed");
    }

    // Initialize server address structure
    bzero((char *) &server_address, sizeof(server_address));
    server_address.sin_family = AF_INET;
    server_address.sin_port = htons(port_number);
    server_address.sin_addr.s_addr = INADDR_ANY;

    // Bind the socket to the address
    if (bind(server_socket, (struct sockaddr *)&server_address, sizeof(server_address)) < 0) {
        error("Bind failed");
    }

    // Listen for incoming connections
    listen(server_socket, 10);

    // Create the RPC client
    clnt = clnt_create(host, OPTION, OPTION_VERSION, "udp");
    if (clnt == NULL) {
        clnt_pcreateerror(host);
        exit(1);
    }

    // Server loop to handle multiple clients
    for (;;) {
        clilen = sizeof(client_address);
        client_socket = accept(server_socket, (struct sockaddr *)&client_address, &clilen);
        if (client_socket < 0) {
            perror("Accept failed");
            continue;
        }

        // Read n from the client
        if (read(client_socket, &n, sizeof(int)) < 0) {
            perror("Failed to read integer length");
            close(client_socket);
            continue;
        }

        // Read a from the client
        if (read(client_socket, &a, sizeof(float)) < 0) {
            perror("Failed to read float");
            close(client_socket);
            continue;
        }

        // Allocate memory for Y array
        Y = (int *)malloc(sizeof(int) * n);
        if (Y == NULL) {
            perror("Memory allocation failed");
            close(client_socket);
            continue;
        }

        // Read Y array from the client
        if (read(client_socket, Y, sizeof(int) * n) < 0) {
            perror("Failed to read integer array");
            free(Y);
            close(client_socket);
            continue;
        }

        // Initialize RPC data structures
        rpc_data.n = n;
        rpc_data.Y.Y_len = n;
        rpc_data.Y.Y_val = Y;

        rpc_product_data.n = n;
        rpc_product_data.Y.Y_len = n;
        rpc_product_data.Y.Y_val = Y;
        rpc_product_data.a = a;

        // Read the options from the client until it sends 4
        while (read(client_socket, &option, sizeof(int)) > 0 && option != 4) {
            // Handle the client request based on the option
            switch (option) {
                case 1:
                    result = option_middle_1(&rpc_data, clnt);
                    if (result == NULL) {
                        clnt_perror(clnt, "Call failed");
                    } else {
                        write(client_socket, result, sizeof(float));
                    }
                    break;
                case 2:
                    result_array = option_min_max_1(&rpc_data, clnt);
                    if (result_array == NULL) {
                        clnt_perror(clnt, "Call failed");
                    } else {
                        min_max[0] = result_array->array.array_val[0];
                        min_max[1] = result_array->array.array_val[1];
                        write(client_socket, min_max, 2 * sizeof(int));
                    }
                    break;
                case 3:
                    result_farray = option_product_1(&rpc_product_data, clnt);
                    if (result_farray == NULL) {
                        clnt_perror(clnt, "Call failed");
                    } else {
                        write(client_socket, result_farray->array.array_val, n * sizeof(float));
                    }
                    break;
            }
        }

        // Free allocated memory and close client socket
        free(Y);
        close(client_socket);
    }

    // Destroy RPC client and close server socket
    clnt_destroy(clnt);
    close(server_socket);

    return 0;
}

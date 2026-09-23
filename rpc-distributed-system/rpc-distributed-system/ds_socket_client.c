#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>


// Configuration values
#define SERVER_IP "127.0.0.1"
#define SERVER_PORT 12345


// Sends an array of integers to the server 
int send_int_array(int sockfd, int *arr, int count) {
    for (int i = 0; i < count; i++) {
        uint32_t val = htonl(arr[i]);
        if (write(sockfd, &val, sizeof(val)) < 0) return -1;
    }
    return 0;
}

int main() {
    int sockfd;
    struct sockaddr_in server_addr;
    
    // Create socket
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        perror("Socket error");
        exit(1);
    }
    
    // Setup server address 
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(SERVER_PORT);
    inet_pton(AF_INET, SERVER_IP, &server_addr.sin_addr);
    
    // Connect to server
    if (connect(sockfd, (struct sockaddr *)&server_addr, sizeof(server_addr)) < 0) {
        perror("Connect error");
        close(sockfd);
        exit(1);
    }
    
    // Main menu loop
    while (1) {
        int option;
        printf("\n-- Menu --\n");
        printf("1: Dot Product\n");
        printf("2: Mean Values\n");
        printf("3: Scalar Product\n");
        printf("0: Exit\n");
        printf("Choice: ");
        if (scanf("%d", &option) != 1 || option == 0) break;
        
        // Send choice to server
        uint32_t net_opt = htonl(option);
        write(sockfd, &net_opt, sizeof(net_opt));
        
        // Handle dot product or mean values operations
        if (option == 1 || option == 2) {
            int n;
            printf("Vector size: ");
            scanf("%d", &n);
            
            // Send vector size to server
            uint32_t net_n = htonl(n);
            write(sockfd, &net_n, sizeof(net_n));
            
            // Allocate memory for vectors
            int *X = malloc(n * sizeof(int));
            int *Y = malloc(n * sizeof(int));
            if (!X || !Y) {
                printf("Memory error\n");
                break;
            }
            
            // Get and send vector X
            printf("Enter %d values for X: ", n);
            for (int i = 0; i < n; i++) scanf("%d", &X[i]);
            send_int_array(sockfd, X, n);
            
            // Get and send vector Y
            printf("Enter %d values for Y: ", n);
            for (int i = 0; i < n; i++) scanf("%d", &Y[i]);
            send_int_array(sockfd, Y, n);
            
            // Process result based on choice
            if (option == 1) {

                uint32_t result_net;
                read(sockfd, &result_net, sizeof(result_net));
                printf("Dot Product = %d\n", ntohl(result_net));
            } else {
                // Mean Values result
                double mx, my;
                read(sockfd, &mx, sizeof(mx));
                read(sockfd, &my, sizeof(my));
                printf("Mean X = %.2f, Mean Y = %.2f\n", mx, my);
            }
            
            free(X);
            free(Y);
        } 
        // Handle scalar product operation
        else if (option == 3) {
            int n;
            double r;
            
            // Get vector size and scalar value
            printf("Vector size: ");
            scanf("%d", &n);
            printf("Scalar value: ");
            scanf("%lf", &r);
            
            // Send vector size and scalar value to server
            uint32_t net_n = htonl(n);
            write(sockfd, &net_n, sizeof(net_n));
            write(sockfd, &r, sizeof(r));
            
            // Allocate memory for vector
            int *X = malloc(n * sizeof(int));
            if (!X) {
                printf("Memory error\n");
                break;
            }
            
            // Get and send vector X
            printf("Enter %d values for X: ", n);
            for (int i = 0; i < n; i++) scanf("%d", &X[i]);
            send_int_array(sockfd, X, n);
            
            // Read result length
            uint32_t net_len;
            read(sockfd, &net_len, sizeof(net_len));
            int len = ntohl(net_len);
            
            // Print results
            printf("Result:");
            for (int i = 0; i < len; i++) {
                double val;
                read(sockfd, &val, sizeof(val));
                printf(" %.2f", val);
            }
            printf("\n");
            
            free(X);
        } else {
            printf("Invalid choice.\n");
        }
    }
    
    // Close socket 
    close(sockfd);
    return 0;
}
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>

// Function to handle errors
void error(const char *msg) {
    perror(msg);
    exit(1);
}

int main(int argc, char *argv[]) {
    int sockfd, portno, n, i, check, option;
    int *Y;
    float a;
    struct sockaddr_in serv_addr;
    struct hostent *server;
    char *option_1_result;
    float result_1;
    int result_2[2];
    float *result_3;

    // Check if the port and hostname are provided
    if (argc < 3) {
        fprintf(stderr, "Usage: %s hostname port\n", argv[0]);
        exit(1);
    }

    // Convert port number from string to integer
    portno = atoi(argv[2]);

    // Create a socket
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0)
        error("Error opening socket");

    // Get the server host
    server = gethostbyname(argv[1]);
    if (server == NULL)
        error("No such host");

    // Initialize server address structure
    bzero((char *) &serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(portno);
    bcopy((char *) server->h_addr, (char *) &serv_addr.sin_addr.s_addr, server->h_length);

    // Connect to the server
    if (connect(sockfd, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) < 0)
        error("Error connecting");

    // Get input from the user
    printf("Give n: ");
    scanf("%d", &n);

    printf("Give a: ");
    scanf("%f", &a);

    Y = (int *)malloc(sizeof(int) * n);
    if (Y == NULL) {
        error("Memory allocation failed");
    }

    printf("Give Y array:\n");
    for (i = 0; i < n; i++)
        scanf("%d", &Y[i]);

    // Write n, a, and Y array to the server
    check = write(sockfd, &n, sizeof(int));
    if (check < 0)
        error("n write failed");

    check = write(sockfd, &a, sizeof(float));
    if (check < 0)
        error("a write failed");

    check = write(sockfd, Y, sizeof(int) * n);
    if (check < 0)
        error("Y write failed");

    option_1_result = (char *)malloc(sizeof(float));
    result_3 = (float *)malloc(sizeof(float) * n);

    if (option_1_result == NULL || result_3 == NULL) {
        error("Memory allocation failed");
    }

    // Menu loop for user options
    do {
        printf("\n1. Mean of Y\n");
        printf("2. Min and max of Y\n");
        printf("3. a * Y\n");
        printf("4. Exit\n");
        printf("Choose an option: ");
        scanf("%d", &option);

        if (option < 1 || option > 4) {
            printf("Wrong input\n");
        } else {
            check = write(sockfd, &option, sizeof(int));
            if (check < 0)
                error("Option write failed");

            switch (option) {
                case 1:
                    check = read(sockfd, option_1_result, sizeof(float));
                    if (check < 0)
                        error("Option 1 read failed");
                    memcpy(&result_1, option_1_result, sizeof(float));
                    printf("\nThe mean of Y is %f\n", result_1);
                    break;
                case 2:
                    check = read(sockfd, result_2, sizeof(int) * 2);
                    if (check < 0)
                        error("Option 2 read failed");
                    printf("\nThe min of Y is %d and the max is %d\n", result_2[0], result_2[1]);
                    break;
                case 3:
                    check = read(sockfd, result_3, sizeof(float) * n);
                    if (check < 0)
                        error("Option 3 read failed");
                    printf("\nThe array a * Y is:\n");
                    for (i = 0; i < n; i++)
                        printf("%f ", result_3[i]);
                    printf("\n");
                    break;
            }
        }
    } while (option != 4);

    // Free allocated memory and close socket
    free(Y);
    free(option_1_result);
    free(result_3);
    close(sockfd);

    return 0;
}

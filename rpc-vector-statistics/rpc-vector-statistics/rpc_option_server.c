#include "rpc_option.h"

// Function to calculate the middle value of an array Y
float *
option_middle_1_svc(data *argp, struct svc_req *rqstp) {
    static float result; // Static to persist after the function returns
    int i, sum;

    sum = 0;
    for (i = 0; i < argp->n; i++)
        sum += argp->Y.Y_val[i]; // Sum all elements of the array

    result = sum / (float)argp->n; // Calculate the middle value (average)

    return &result; // Return the result
}

// Function to find the minimum and maximum values in an array Y
int_array *
option_min_max_1_svc(data *argp, struct svc_req *rqstp) {
    static int_array result; // Static to persist after the function returns
    int i;
    int min, max;

    // Allocate memory for the result array
    result.array.array_val = (int *)malloc(2 * sizeof(int));
    if (result.array.array_val == NULL) {
        perror("Memory allocation failed");
        exit(1);
    }
    result.array.array_len = 2;

    // Initialize min and max with the first element of the array
    min = argp->Y.Y_val[0];
    max = argp->Y.Y_val[0];

    // Find the min value in the array
    for (i = 1; i < argp->n; i++) {
        if (min > argp->Y.Y_val[i])
            min = argp->Y.Y_val[i];
    }

    // Find the max value in the array
    for (i = 1; i < argp->n; i++) {
        if (max < argp->Y.Y_val[i])
            max = argp->Y.Y_val[i];
    }

    // Store min and max in the result array
    result.array.array_val[0] = min;
    result.array.array_val[1] = max;

    return &result; // Return the result array
}

// Function to calculate the product of each element in array Y with a scalar a
float_array *
option_product_1_svc(product_data *argp, struct svc_req *rqstp) {
    static float_array result; // Static to persist after the function returns
    int i;

    // Allocate memory for the result array
    result.array.array_val = (float *)malloc(argp->n * sizeof(float));
    if (result.array.array_val == NULL) {
        perror("Memory allocation failed");
        exit(1);
    }
    result.array.array_len = argp->n;

    // Calculate the product for each element in the array
    for (i = 0; i < argp->n; i++)
        result.array.array_val[i] = argp->a * argp->Y.Y_val[i];

    return &result; // Return the result array
}

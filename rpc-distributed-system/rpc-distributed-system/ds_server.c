#include "ds.h"
#include <stdlib.h>

 // Calculate the dot product of two vectors
int *
dot_product_calculator_1_svc(vectors *argp, struct svc_req *rqstp)
{
    static int result;
    result = 0;
    
    // Calculate dot product
    for (int i = 0; i < argp->x.x_len && i < argp->y.y_len; i++)
        result += argp->x.x_val[i] * argp->y.y_val[i];
    
    return &result;
}

 // Calculate the mean values
mean_result *
mean_calculator_1_svc(vectors *argp, struct svc_req *rqstp)
{
    static mean_result result;
    
    double sum_x = 0;
    double sum_y = 0;
    
    int n = argp->n;
    
    for(int i = 0; i < n && i < argp->x.x_len && i < argp->y.y_len; i++) {
        sum_x += argp->x.x_val[i];
        sum_y += argp->y.y_val[i];
    }
    
    result.mean_x = sum_x / n;
    result.mean_y = sum_y / n;
    
    return &result;
}

 // Calculate the scalar product of a vector and a constant
product_result *
product_calculator_1_svc(product_vector *argp, struct svc_req *rqstp)
{
    static product_result result;
    int n = argp->n;
    
    result.n = n;

    result.values.values_val = malloc(n * sizeof(double));
    result.values.values_len = n;
    
    if(!result.values.values_val) {
        result.values.values_len = 0;
        return &result;
    }
    
    for (int i = 0; i < n && i < argp->x.x_len; i++)
        result.values.values_val[i] = argp->r * argp->x.x_val[i];
    
    return &result;
}
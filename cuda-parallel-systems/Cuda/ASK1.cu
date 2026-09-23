// ASK1.cu
#include <cuda.h>
#include "cuda_check.h"
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

// Kernel to compute sum and max of A 
__global__ void sum_max(const int *A, float *sum, int *max, int N) {
    // Each thread processes one matrix element
    int column = blockIdx.x * blockDim.x + threadIdx.x;
    int row = blockIdx.y * blockDim.y + threadIdx.y;

    if (row < N && column < N) {
        // Access the element
        int value = A[row * N + column];
        // Use atomic to update sum and max
        atomicAdd(sum, (float) value);
        atomicMax(max, value);
    }
}

// Kernel to build B
__global__ void build_B(const int *A, float *B, int N, float mean, int amax) {
    // Each thread processes one matrix element
    int column = blockIdx.x * blockDim.x + threadIdx.x;
    int row = blockIdx.y * blockDim.y + threadIdx.y;

    if (row < N && column < N) {
        float value = 0.0;
        if (amax != 0)
            // Compute B[row][column]
            value = (float) ((mean - (float) A[row * N + column]) / (float) amax);
        B[row * N + column] = value;
    }
}

// Kernel to build C
__global__ void build_C(const int *A, float *C, int N) {
    // Each thread processes one matrix element
    int column = blockIdx.x * blockDim.x + threadIdx.x;
    int row = blockIdx.y * blockDim.y + threadIdx.y;
    if (row < N && column < N) {
        int jplus, jminus;

        if (column + 1 == N)
            jplus = 0;
        else
            jplus = column + 1;

        if (column == 0)
            jminus = N - 1;
        else
            jminus = column - 1;

        int aij = A[row * N + column];
        int aplus = A[row * N + jplus];
        int aminus = A[row * N + jminus];
        C[row * N + column] = (float) (aij + aplus + aminus) / 3.0;
    }
}

// Print B or C
static void print_matrix(const float *X, int N, const char name) {
    printf("\n%c matrix:\n", name);
    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++)
            printf("%8.4f ", X[i * N + j]);
        printf("\n");
    }
    printf("\n");
}


int main() {
    FILE *f = fopen("args", "r");
    if (!f) {
        printf("error: cannot open args\n");
        exit(1);
    }

    int N, threads;
    if (fscanf(f, "%d", &N) != 1) exit(1);
    if (fscanf(f, "%d", &threads) != 1) exit(1);

    if (N <= 0 || threads <= 0) exit(1);
    // Ensure that the number of threads does not exceed N
    if (threads * threads > N) exit(1);

    int bytesA = N * N * (int) sizeof(int);

    // Read matrix A from file
    int *hA = (int *) malloc(bytesA);
    if (hA == NULL) exit(1);
    for (int i = 0; i < N * N; i++)
        if (fscanf(f, "%d", &hA[i]) != 1) exit(1);
    fclose(f);

    int *dA, *dMax;
    float *dOut, *dSum;

    // Size of the output matrix B or C in bytes
    int bytesFloat = N * N * (int) sizeof(float);

    // Calculate grid dimensions
    int blocksPer = (N - 1) / threads + 1;

    // Define block and grid dimensions
    dim3 dimBlock(threads, threads);
    dim3 dimGrid(blocksPer, blocksPer);

    // Calculate total number of threads
    int threadsPerBlock = threads * threads;
    // Calculate total number of blocks
    int numBlocks = dimGrid.x * dimGrid.y;
    // Calculate total number of threads in the grid
    int totalThreads = threadsPerBlock * numBlocks;

    // Create CUDA events for timing

    cudaEvent_t start_total, end_total;
    cudaEvent_t start_sum_max, end_sum_max;
    cudaEvent_t start_BC, end_BC;
    float ms_total, ms_sum_max, ms_BC;

    // Create events
    HANDLE_ERROR(cudaEventCreate(&start_total));
    HANDLE_ERROR(cudaEventCreate(&end_total));
    HANDLE_ERROR(cudaEventCreate(&start_sum_max));
    HANDLE_ERROR(cudaEventCreate(&end_sum_max));
    HANDLE_ERROR(cudaEventCreate(&start_BC));
    HANDLE_ERROR(cudaEventCreate(&end_BC));

    // Allocate device memory
    HANDLE_ERROR(cudaMalloc((void **) &dA, bytesA));
    HANDLE_ERROR(cudaMalloc((void **) &dOut, bytesFloat));
    HANDLE_ERROR(cudaMalloc((void **) &dSum, (int) sizeof(float)));
    HANDLE_ERROR(cudaMalloc((void **) &dMax, (int) sizeof(int)));

    // Copy data from host to device while initializing sum and max
    float zero = 0.0;
    int imax = 0;
    HANDLE_ERROR(cudaMemcpy(dA, hA, bytesA, cudaMemcpyHostToDevice));
    HANDLE_ERROR(cudaMemcpy(dSum, &zero, (int) sizeof(float), cudaMemcpyHostToDevice));
    HANDLE_ERROR(cudaMemcpy(dMax, &imax, (int) sizeof(int), cudaMemcpyHostToDevice));

    HANDLE_ERROR(cudaEventRecord(start_total, 0));
    HANDLE_ERROR(cudaEventRecord(start_sum_max, 0));
    // Compute sum and max of A
    sum_max<<<dimGrid, dimBlock>>>(dA, dSum, dMax, N);

    // Copy sum/max back
    float hSum = 0.0;
    int hMax = 0;
    HANDLE_ERROR(cudaMemcpy(&hSum, dSum, (int) sizeof(float), cudaMemcpyDeviceToHost));
    HANDLE_ERROR(cudaMemcpy(&hMax, dMax, (int) sizeof(int), cudaMemcpyDeviceToHost));
    // Compute mean
    float mean = hSum / (float) (N * N);
    HANDLE_ERROR(cudaGetLastError());
    HANDLE_ERROR(cudaEventRecord(end_sum_max, 0));
    HANDLE_ERROR(cudaEventSynchronize(end_sum_max));
    HANDLE_ERROR(cudaEventElapsedTime(&ms_sum_max, start_sum_max, end_sum_max));

    int condition;
    // Determine whether to build B or C
    if (hMax > N * mean)
        condition = 1;
    else
        condition = 0;

    // Allocate host memory for output
    float *hOut = (float *) malloc(bytesFloat);
    if (hOut == NULL) exit(1);

    HANDLE_ERROR(cudaEventRecord(start_BC, 0));
    // Build B or C based on condition
    float minB;
    if (condition) {
        build_B<<<dimGrid, dimBlock>>>(dA, dOut, N, mean, hMax);
        // Copy output to host
        HANDLE_ERROR(cudaMemcpy(hOut, dOut, bytesFloat, cudaMemcpyDeviceToHost));
        // Find minimum value in B
        minB = hOut[0];
        for (int i = 0; i < N * N; i++)
            if (hOut[i] < minB) minB = hOut[i];
    } else {
        build_C<<<dimGrid, dimBlock>>>(dA, dOut, N);
        HANDLE_ERROR(cudaMemcpy(hOut, dOut, bytesFloat, cudaMemcpyDeviceToHost));
    }
    HANDLE_ERROR(cudaGetLastError());
    HANDLE_ERROR(cudaEventRecord(end_BC, 0));
    HANDLE_ERROR(cudaEventSynchronize(end_BC));
    HANDLE_ERROR(cudaEventElapsedTime(&ms_BC, start_BC, end_BC));

    HANDLE_ERROR(cudaEventRecord(end_total, 0));
    HANDLE_ERROR(cudaEventSynchronize(end_total));
    HANDLE_ERROR(cudaEventElapsedTime(&ms_total, start_total, end_total));

    printf("Matrix size: N = %d\n", N);
    printf("Total elements: %d\n", (N * N));

    printf("Threads: %d\n", threads);
    printf("Threads per block: %d\n", threadsPerBlock);
    printf("Total blocks: %d\n", numBlocks);
    printf("Total threads in grid: %d\n", totalThreads);

    printf("Mean of matrix A: %.6f\n", mean);
    printf("Maximum element of matrix A: %d\n", hMax);
    printf("Matrix build: %c\n", condition ? 'B' : 'C');

    // Printing B or C is commented out, for a big N the output is huge
    /*
    if (condition) {
        print_matrix(hOut, N, 'B');
        printf("Minimum value of matrix B (amin(B)): %.6f\n", minB);
    } else print_matrix(hOut, N, 'C');
     */

    printf("Sum,max and mean time: %.3f ms\n", ms_sum_max);
    printf("Build %c: %.3f ms\n", condition ? 'B' : 'C', ms_BC);
    printf("Total execution time: %.3f ms\n", ms_total);

    cudaFree(dA);
    cudaFree(dOut);
    cudaFree(dSum);
    cudaFree(dMax);

    cudaEventDestroy(start_sum_max);
    cudaEventDestroy(end_sum_max);
    cudaEventDestroy(start_BC);
    cudaEventDestroy(end_BC);
    cudaEventDestroy(start_total);
    cudaEventDestroy(end_total);

    return 0;
}

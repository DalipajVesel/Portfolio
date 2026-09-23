#include <cuda.h>
#include "cuda_check.h"
#include <stdio.h>
#include <stdlib.h>

// Kernel to compute the sum of each column
__global__ void sum_columns(const int *A, float *sums, int N) {
    // Each thread processes one matrix element
    int column = blockIdx.x * blockDim.x + threadIdx.x;
    int row = blockIdx.y * blockDim.y + threadIdx.y;
    if (row < N && column < N)
        // add the element to the column sum using atomic add
        atomicAdd(&sums[column], (float) A[row * N + column]);
}

// Kernel to compute the mean of each column
__global__ void compute_means(float *means, const float *sums, int N) {
    // Each thread computes the mean for one column
    int column = blockIdx.x * blockDim.x + threadIdx.x;
    if (column < N)
        means[column] = sums[column] / (float) N;
}

// Kernel to subtract the mean of each column
__global__ void substract_mean(const int *A, const float *means, float *B, int N) {
    // Each thread processes one matrix element
    int column = blockIdx.x * blockDim.x + threadIdx.x;
    int row = blockIdx.y * blockDim.y + threadIdx.y;
    if (row < N && column < N)
        // Subtract the mean from the element
        B[row * N + column] = (float) A[row * N + column] - means[column];
}

// Kernel to compute the upper triangular part
__global__ void cov_upper(const float *B, float *C, int N) {
    // Each thread processes one matrix element
    int column = blockIdx.x * blockDim.x + threadIdx.x;
    int row = blockIdx.y * blockDim.y + threadIdx.y;
    // Compute only for upper triangular part
    if (row < N && column < N && row <= column) {
        float s = 0.0;
        // Dot product of row and column
        for (int k = 0; k < N; k++)
            s += B[k * N + row] * B[k * N + column];
        // Store the result in the covariance matrix
        C[row * N + column] = s;
    }
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

    // Allocate host memory for matrix A
    int bytesA = N * N * sizeof(int);

    // Read matrix A from file
    int *hA = (int *) malloc(bytesA);
    if (hA == NULL) exit(1);
    for (int i = 0; i < N * N; i++)
        if (fscanf(f, "%d", &hA[i]) != 1) exit(1);
    fclose(f);

    // Allocate memory for sums, means, B and C matrix
    int bytesBC = N * N * sizeof(float);
    int bytesSM = N * sizeof(float);

    // Allocate host memory for sums and C matrix
    float *hSums = (float *) malloc(bytesSM);
    float *hC = (float *) malloc(bytesBC);

    // Initialize sums and C matrix to zero
    for (int i = 0; i < N; i++)
        hSums[i] = 0.0;
    for (int i = 0; i < N * N; i++)
        hC[i] = 0.0;

    int *dA;
    float *dSums, *dMeans, *dB, *dC;

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
    cudaEvent_t start_mean, end_mean;
    cudaEvent_t start_substract_mean, end_substract_mean;
    cudaEvent_t start_cov, end_cov;

    float ms_total, ms_mean, ms_substract_mean, ms_cov;

    // Create events
    HANDLE_ERROR(cudaEventCreate(&start_total));
    HANDLE_ERROR(cudaEventCreate(&end_total));

    HANDLE_ERROR(cudaEventCreate(&start_mean));
    HANDLE_ERROR(cudaEventCreate(&end_mean));

    HANDLE_ERROR(cudaEventCreate(&start_substract_mean));
    HANDLE_ERROR(cudaEventCreate(&end_substract_mean));

    HANDLE_ERROR(cudaEventCreate(&start_cov));
    HANDLE_ERROR(cudaEventCreate(&end_cov));

    // Allocate device memory
    HANDLE_ERROR(cudaMalloc((void **) &dA, bytesA));
    HANDLE_ERROR(cudaMalloc((void **) &dB, bytesBC));
    HANDLE_ERROR(cudaMalloc((void **) &dC, bytesBC));
    HANDLE_ERROR(cudaMalloc((void **) &dSums, bytesSM));
    HANDLE_ERROR(cudaMalloc((void **) &dMeans, bytesSM));

    // Copy data from host to device
    HANDLE_ERROR(cudaMemcpy(dA, hA, bytesA, cudaMemcpyHostToDevice));
    HANDLE_ERROR(cudaMemcpy(dSums, hSums, bytesSM, cudaMemcpyHostToDevice));
    HANDLE_ERROR(cudaMemcpy(dC, hC, bytesBC, cudaMemcpyHostToDevice));

    // Start total timer
    HANDLE_ERROR(cudaEventRecord(start_total, 0));

    HANDLE_ERROR(cudaEventRecord(start_mean, 0));
    // Compute sums and means
    sum_columns<<<dimGrid, dimBlock>>>(dA, dSums, N);
    compute_means<<<blocksPer, threads>>>(dMeans, dSums, N);
    HANDLE_ERROR(cudaGetLastError());
    HANDLE_ERROR(cudaEventRecord(end_mean, 0));
    HANDLE_ERROR(cudaEventSynchronize(end_mean));
    HANDLE_ERROR(cudaEventElapsedTime(&ms_mean, start_mean, end_mean));

    HANDLE_ERROR(cudaEventRecord(start_substract_mean, 0));
    // Subtract means from A to get B
    substract_mean<<<dimGrid, dimBlock>>>(dA, dMeans, dB, N);
    HANDLE_ERROR(cudaGetLastError());
    HANDLE_ERROR(cudaEventRecord(end_substract_mean, 0));
    HANDLE_ERROR(cudaEventSynchronize(end_substract_mean));
    HANDLE_ERROR(cudaEventElapsedTime(&ms_substract_mean, start_substract_mean, end_substract_mean));

    HANDLE_ERROR(cudaEventRecord(start_cov, 0));
    // Compute covariance upper triangular
    cov_upper<<<dimGrid, dimBlock>>>(dB, dC, N);
    HANDLE_ERROR(cudaMemcpy(hC, dC, bytesBC, cudaMemcpyDeviceToHost));
    HANDLE_ERROR(cudaGetLastError());
    HANDLE_ERROR(cudaEventRecord(end_cov, 0));
    HANDLE_ERROR(cudaEventSynchronize(end_cov));
    HANDLE_ERROR(cudaEventElapsedTime(&ms_cov, start_cov, end_cov));

    // Stop total timer
    HANDLE_ERROR(cudaEventRecord(end_total, 0));
    HANDLE_ERROR(cudaEventSynchronize(end_total));
    HANDLE_ERROR(cudaEventElapsedTime(&ms_total, start_total, end_total));

    printf("Matrix size: N = %d\n", N);
    printf("Total elements: %d\n", (N * N));

    printf("Threads: %d\n", threads);
    printf("Threads per block: %d\n", threadsPerBlock);
    printf("Total blocks: %d\n", numBlocks);
    printf("Total threads in grid: %d\n", totalThreads);

    printf("Column means computation: %.3f ms\n", ms_mean);
    printf("Subtracting means : %.3f ms\n", ms_substract_mean);
    printf("Covariance upper computation: %.3f ms\n", ms_cov);
    printf("Total time: %.3f ms\n", ms_total);

    // Printing the full C is commented out, for a big N the output is huge
    /*
    for (int i = 0; i < N; i++)
        for (int j = 0; j < i; j++)
            hC[i * N + j] = hC[j * N + i];

    printf("\nCovariance matrix C (N=%d):\n", N);
    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            printf("%10.3f ", hC[i * N + j]);
        }
        printf("\n");
    }
    printf("\n");
    */


    cudaFree(dA);
    cudaFree(dB);
    cudaFree(dC);
    cudaFree(dSums);
    cudaFree(dMeans);

    cudaEventDestroy(start_total);
    cudaEventDestroy(end_total);

    cudaEventDestroy(start_mean);
    cudaEventDestroy(end_mean);

    cudaEventDestroy(start_substract_mean);
    cudaEventDestroy(end_substract_mean);

    cudaEventDestroy(start_cov);
    cudaEventDestroy(end_cov);

    return 0;
}

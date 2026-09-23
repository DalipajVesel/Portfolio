#include <omp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define limit 1000

//  Generate a random list of size n
void generate_list(int * x, int n) {
   int i,j,t;
   for (i = 0; i < n; i++)
     x[i] = i;
   for (i = 0; i < n; i++) {
     j = rand() % n;
     t = x[i];
     x[i] = x[j];
     x[j] = t;
   }
}

// Merge two sorted halves
void merge(int * X, int n, int * tmp) {
   int i = 0;
   int j = n/2;
   int ti = 0;

   while (i<n/2 && j<n) {
      if (X[i] < X[j]) {
         tmp[ti] = X[i];
         ti++; i++;
      } else {
         tmp[ti] = X[j];
         ti++; j++;
      }
   }
   while (i<n/2) {
      tmp[ti] = X[i];
      ti++; i++;
   }
   while (j<n) {
      tmp[ti] = X[j];
      ti++; j++;
   }
   memcpy(X, tmp, n*sizeof(int));
}

// Recursive mergesort which  splits the array into two parts sort both parts and merge them
void mergesort(int * X, int n, int * tmp)
{
   if (n < 2) return;

   #pragma omp task firstprivate (X, n, tmp)
   mergesort(X, n/2, tmp);

   #pragma omp task firstprivate (X, n, tmp)
   mergesort(X+(n/2), n-(n/2), tmp+(n/2));

   #pragma omp taskwait

   merge(X, n, tmp);
}

// Merge two sorted ranges
static void merge_halves(int *A1, int *A1_end, int *A2, int *A2_end, int *dest)
{
    int *i  = A1;
    int *j  = A2;
    int *ti = dest;

    while (i <= A1_end && j <= A2_end) {
        if (*i < *j) {
            *ti = *i;
            ti++; i++;
        } else {
            *ti = *j;
            ti++; j++;
        }
    }

    while (i <= A1_end) {
        *ti = *i;
        ti++; i++;
    }

    while (j <= A2_end) {
        *ti = *j;
        ti++; j++;
    }
}

// Multisort function that splits the array into four parts
static void multisort(int *start, int *space, int size) {

    // Small array, use standard mergesort
    if (size < limit) {
        mergesort(start, size, space);
        return;
    }

    int quarter = size / 4;
    if (quarter == 0) {
        mergesort(start, size, space);
        return;
    }

    // Split into four parts
    int *startA = start;
    int *spaceA = space;

    int *startB = startA + quarter;
    int *spaceB = spaceA + quarter;

    int *startC = startB + quarter;
    int *spaceC = spaceB + quarter;

    int *startD = startC + quarter;
    int *spaceD = spaceC + quarter;

    int sizeA = quarter;
    int sizeB = quarter;
    int sizeC = quarter;
    int sizeD = size - 3 * quarter;

    // Sort each part in parallel
    #pragma omp task
    multisort(startA, spaceA, sizeA);

    #pragma omp task
    multisort(startB, spaceB, sizeB);

    #pragma omp task
    multisort(startC, spaceC, sizeC);

    #pragma omp task
    multisort(startD, spaceD, sizeD);

    #pragma omp taskwait

    // Merge sorted parts
    #pragma omp task
    merge_halves(startA, startA + quarter - 1,
                 startB, startB + quarter - 1, spaceA);

    #pragma omp task
    merge_halves(startC, startC + quarter - 1,
                 startD, start + size - 1, spaceC);

    #pragma omp taskwait

    // Final merge
    merge_halves(spaceA, spaceC - 1,
                 spaceC, spaceA + size - 1, startA);
}

int main(int argc, char *argv[]) {

    int *A, *tmp;
    int N, threads;

    double start_time_total, end_time_total;

    // Read N and threads
    if (argc < 3) {
        printf("Error while reading arguments!\n");
        return -1;
    }

    N = atoi(argv[1]);
    if (N <= 0) {
        printf("Error while reading N!\n");
        return -1;
    }

    threads = atoi(argv[2]);
    if (threads <= 0) {
        printf("Invalid number of threads!\n");
        return -1;
    }

    A  = (int *)malloc(N * sizeof(int));
    tmp = (int *)malloc(N * sizeof(int));

    if (!A || !tmp) {
        printf("Malloc error!\n");
        return -1;
    }

    // Generate random list
    srand(1);
    generate_list(A, N);

    /*
    printf("\nA array before sorting:\n");
    for (int i = 0; i < N; i++) {
        printf("%d ", A[i]);
        if ((i + 1) % 10 == 0) printf("\n");  
    }
    */

    // Set number of threads
    omp_set_num_threads(threads);

    start_time_total = omp_get_wtime();
    #pragma omp parallel
    {
        // Single thread to initiate multisort
        #pragma omp single
        multisort(A, tmp, N);
    }
    end_time_total = omp_get_wtime();

    /*
    printf("\nSorted array:\n");
    for (int i = 0; i < N; i++) {
        printf("%d ", A[i]);
        if ((i + 1) % 10 == 0) printf("\n");  
    }
    */

    printf("\n");

    printf("Array size N: %d\n", N);
    printf("Number of threads: %d\n", threads);
    printf("Execution time: %.9f seconds\n", end_time_total - start_time_total);


    return 0;
}

#include <omp.h>
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char *argv[]) {

    int **A;
    int N;
    FILE *args = stdin;
    int i, j;
    int t;

    double start_time_total, end_time_total;
    double a_start_time, a_end_time;
    double b_start_time, b_end_time;
    double c_start_time, c_end_time;
    double d1_start_time, d1_end_time;
    double d2_start_time, d2_end_time;

    // Check for input file
    if(argc >= 2){
        args = fopen(argv[1], "r");
        if(!args){
            printf("Error opening file!\n");
            return -1;
        }
    }

    printf("Give number of threads: ");
    if (scanf("%d", &t) != 1 || t <= 0 || t > 8) {
        printf("Invalid number of threads!\n");
        return -1;
    }

    // Read matrix size
    if(fscanf(args, "%d", &N) != 1 || N <= 0){
        printf("Error while reading matrix size!\n");
        return -1;
    }

    // Allocate memory for matrix A
    A = (int **)malloc(N * sizeof(int *));
    if(!A){
        printf("Malloc error!\n");
        return -1;
    }

    // Allocate memory for each row of matrix A
    for (i=0; i<N; i++){
        A[i] = (int *)malloc(N * sizeof(int));
        if(!A[i]){
            printf("Malloc error!\n");
            return -1;
        }
    }

    // Read matrix A from input
    for (i = 0; i < N; i++) {
        for (j = 0; j < N; j++) {
            if (fscanf(args, "%d", &A[i][j]) != 1) {
                printf("Error while reading matrix\n");
                return -1;
            }
        }
    }

    omp_set_num_threads(t);
    start_time_total = omp_get_wtime();
    // Check for strictly diagonal dominance
    int sdd = 1;

    a_start_time = omp_get_wtime();

    // Parallel check of strictly diagonal dominance
    #pragma omp parallel for private(j) reduction(&&:sdd)

    for (i=0;i<N;i++){
        int sum = 0;
        int diag = A[i][i];

        for(j=0;j<N;j++){
            if (i != j){
                sum += A[i][j];
            }
        }

        if (diag <= sum){
            sdd = 0;
        }

    }
    a_end_time = omp_get_wtime();

    
    // If not strictly diagonally dominant, exit
    if (sdd!=1){
        printf("The matrix is not strictly diagonally dominant\n");

        return 0;
    }

    int m=0;
    b_start_time = omp_get_wtime();
    // Find maximum absolute value on the main diagonal
    #pragma omp parallel for reduction(max:m)

    for(i=0;i<N;i++){
        int value = A[i][i];
        if (value > m){
            m = value;
        }

    }
    b_end_time = omp_get_wtime();

    int **B;
    // Allocate memory for matrix B
    B = (int **)malloc(N * sizeof(int *));
    if(!B){
        printf("Malloc error!\n");
        return -1;
    }

    // Allocate memory for each row of matrix B
    for (i=0; i<N; i++){
        B[i] = (int *)malloc(N * sizeof(int));
        if(!B[i]){
            printf("Malloc error!\n");
            return -1;
        }
    }

    c_start_time = omp_get_wtime();
    // Construct matrix B
    #pragma omp parallel for private(j)
    for(i=0;i<N;i++){
        for(j=0;j<N;j++){
            if (i==j){
                B[i][j] = m;
            } else {
                B[i][j] = m-A[i][j];
            }
        }
    }

    /** Print matrix B for verification, is commented because of large N
    printf("Matrix B:\n");
    for (i = 0; i < N; i++) {
        for (j = 0; j < N; j++) {
            printf("%d ", B[i][j]);
        }
        printf("\n");
    } **/
    c_end_time = omp_get_wtime();

    int min = B[0][0];
    d1_start_time = omp_get_wtime();
    // Find minimum value in matrix B using reduction
    #pragma omp parallel for private(j) reduction(min:min)
    for(i=0;i<N;i++){
        for(j=0;j<N;j++){
            if (B[i][j] < min){
                min = B[i][j];
            }
        }
    }
    d1_end_time = omp_get_wtime();

    printf("The minimum value in matrix B is: %d\n", min);

    int min_critical = B[0][0];

    d2_start_time = omp_get_wtime();
    // Find minimum value in matrix B using critical
    #pragma omp parallel for private(j)
    for(i=0;i<N;i++){
        int local_min = B[i][0];
        for(j=1;j<N;j++){
            if (B[i][j] < local_min){
                local_min = B[i][j];
            }
        }

        #pragma omp critical
        {
            if (local_min < min_critical){
                min_critical = local_min;
            }
        }

    }
    d2_end_time = omp_get_wtime();

    printf("The minimum value in matrix B using critical is: %d\n", min_critical);

    end_time_total = omp_get_wtime();

    // Print timing information
    printf("\nTiming Information (in seconds):\n");
    printf("Total Time: %.9f\n", end_time_total - start_time_total);
    printf("Time for A: %.9f\n", a_end_time - a_start_time);
    printf("Time for B: %.9f\n", b_end_time - b_start_time);
    printf("Time for C: %.9f\n", c_end_time - c_start_time);
    printf("Time for D1: %.9f\n", d1_end_time - d1_start_time);
    printf("Time for D2: %.9f\n", d2_end_time - d2_start_time);

    return 1;
}
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

int main(void) {
    int N, threads;

    FILE *f = fopen("args", "w");
    if (!f) return 1;

    if (scanf("%d", &N) != 1) return 1;
    if (scanf("%d", &threads) != 1) return 1;

    fprintf(f, "%d\n", N);
    fprintf(f, "%d\n", threads);

    // different numbers on every run
    srand((unsigned) time(NULL) ^ (unsigned) clock());

    // with N = 1 only C can happen
    int makeB = (N > 1) ? (rand() & 1) : 0;

    // make A here and then write it to the file
    int *A = (int *) malloc(N * N * sizeof(int));
    if (!A) return 1;

    if (makeB) {
        // B: small values and one big value, so max > N * mean
        int sum = 0;
        int mx = 0;

        // random position for the big value
        int pi = rand() % N;
        int pj = rand() % N;
        int p = pi * N + pj;

        for (int i = 0; i < N * N; i++) {
            int v = rand() % 10; // 0..9
            A[i] = v;
            sum += v;
            if (v > mx) mx = v;
        }

        int old = A[p];
        int sum_excl = sum - old;

        // the big value M has to be bigger than the max and M * (N - 1) > sum of the rest
        int M = (sum_excl / (N - 1)) + 1;
        if (M <= mx) M = mx + 1;

        A[p] = M;
    } else {
        // C: values from 10 to 12, so max <= N * mean
        for (int i = 0; i < N * N; i++) {
            A[i] = 10 + (rand() % 3); // 10..12
        }
    }

    // Write matrix to args file
    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            fprintf(f, "%d", A[i * N + j]);
            if (j < N - 1) fprintf(f, " ");
        }
        fprintf(f, "\n");
    }

    free(A);
    fclose(f);
    return 0;
}

#include <stdio.h>

int main(void) {
    int N;
    int i, j;
    int diag, off = 1;

    FILE *f = fopen("args", "w");

    scanf("%d", &N);     
    diag = N;            

    fprintf(f, "%d\n", N);

    for (i = 0; i < N; i++) {
        for (j = 0; j < N; j++) {
            if (i == j) {
                fprintf(f, "%d", diag);
            } else {
                fprintf(f, "%d", off);
            }
            if (j < N - 1) {
                fprintf(f, " ");
            }
        }
        fprintf(f, "\n");
    }

    fclose(f);
    return 1;
}

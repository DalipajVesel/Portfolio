# CUDA Parallel Systems

Two CUDA exercises on an N x N integer matrix. ASK1.cu finds the sum and the
maximum with atomics and the mean from the sum. If the maximum is bigger than
N times the mean it builds B from the mean and the maximum, otherwise it
builds C, where every element is the average of itself and its two neighbours
in the row. ASk2.cu subtracts the mean of every column and computes only the
upper triangle of the covariance matrix, since it is symmetric. Every step is
timed with CUDA events.

Parallel Systems course, assignment 2B.

## Build and run

I ran them on Windows 11 with an RTX 3070.

    nvcc -O2 -o ASK1 ASK1.cu
    nvcc -O2 -o ASK2 ASk2.cu
    gcc  -O2 -o generator generator.c

    ./generator
    ./ASK1
    ./ASK2

generator asks for N and the threads per block side, which can be at most the
square root of N, and writes args. The matrix is random but made so that ASK1
builds B about half of the time.

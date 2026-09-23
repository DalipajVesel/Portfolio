# OpenMP Parallel Systems

Two OpenMP exercises in C. mp.c checks if a matrix is strictly diagonally
dominant, builds a second matrix and finds its minimum in two ways, with a
reduction and with a critical section. oMP.c sorts an array with multisort:
it splits it in four parts that are sorted as OpenMP tasks and then merged.
Both print timings so the speed-up can be checked.

Parallel Systems course, assignments 1 and 2A.

## Build and run

I ran them on Windows 11 with an AMD Ryzen 7 7700.

    gcc -fopenmp -O2 -o mp  mp.c
    gcc -fopenmp -O2 -o oMP oMP.c
    gcc -O2 -o generator generator.c

    echo 100 | ./generator
    ./mp args
    ./oMP 1000000 4

generator takes the matrix size and writes args, mp asks for the number of
threads (1 to 8), and oMP takes the array size and the threads as arguments.

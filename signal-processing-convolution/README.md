# Signal Processing Convolution

A 50 Hz cosine with gaussian noise at an input SNR of 5 dB, filtered with two
FIR filters written as convolutions: a moving average and a windowed-sinc
low-pass. The script prints the output SNR and the MSE of each and sweeps the
filter length from 3 to 81.

## Build and run

    pip install numpy matplotlib
    python signal_processing.py

The moving average gives 11.06 dB and the windowed-sinc FIR 12.93 dB, from
5.36 dB at the input. The figures are saved in figures/ and results.txt is
the output of one run.

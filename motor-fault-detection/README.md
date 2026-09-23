# Motor Fault Detection

An op-amp circuit that checks an electric motor from its vibrations and
lights a LED when a bearing is wearing out. A healthy motor vibrates under
250 Hz, while a worn bearing adds vibrations up to 3000 Hz. The chain is a
250 to 3000 Hz band-pass filter, a precision rectifier with an RC filter and
a comparator that drives the LED.

The filter is a 4th order Butterworth with two Sallen-Key stages, designed
with TI WebENCH. The circuit was simulated in TINA-TI with TL081 op-amps.

Microelectronics course, lab assignment.

motor1.TSC opens in TINA-TI. The switch SW-SPDT1 picks the Motor_OK or the
Motor_fault source, both given with the assignment. The report (in Greek)
has the component values and the transient results.

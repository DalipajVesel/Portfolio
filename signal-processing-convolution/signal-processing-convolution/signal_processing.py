import numpy as np
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import os

# παραμετροι του σηματος
fs = 1000.0
f0 = 50.0
amplitude = 1.0
duration = 0.5
snr_in = 5.0

# παραμετροι των δυο φιλτρων
ma_length = 5
fir_length = 51
fir_fcut = 80.0

# για τη σαρωση του μηκους
sweep = np.arange(3, 82, 2)
trials = 30

os.makedirs("figures", exist_ok=True)
plt.rcParams["figure.figsize"] = (11, 6)

rng = np.random.default_rng(42)


# Προσθετουμε θορυβο για το SNR που θελουμε
def add_noise(x, snr_db):
    p_signal = np.mean(x ** 2)
    p_noise = p_signal / (10 ** (snr_db / 10))
    noise = rng.normal(0.0, np.sqrt(p_noise), size=x.shape)
    return x + noise, noise


def moving_average(m):
    return np.ones(m) / m


# Το windowed sinc με παραθυρο Hamming
def fir_lowpass(m, fcut=fir_fcut):
    if m % 2 == 0:
        m = m + 1
    n = np.arange(m) - (m - 1) / 2
    fc = fcut / fs
    h = 2 * fc * np.sinc(2 * fc * n)
    h = h * np.hamming(m)
    return h / np.sum(h)


# Συνελιξη και διορθωση της καθυστερησης
def apply_filter(x, h):
    y = np.convolve(x, h, mode="full")
    delay = (len(h) - 1) // 2
    return y[delay:delay + len(x)]


def snr(reference, estimate):
    err = estimate - reference
    return 10 * np.log10(np.sum(reference ** 2) / np.sum(err ** 2))


def mse(reference, estimate):
    return np.mean((estimate - reference) ** 2)


def spectrum(x):
    f = np.fft.rfftfreq(len(x), d=1 / fs)
    mag = np.abs(np.fft.rfft(x)) * 2 / len(x)
    return f, mag


def freq_response(h):
    w = np.linspace(0, np.pi, 2048)
    H = np.array([np.sum(h * np.exp(-1j * wi * np.arange(len(h)))) for wi in w])
    return w * fs / (2 * np.pi), np.abs(H)


# Το καθαρο συνημιτονο και το ιδιο με θορυβο
t = np.arange(int(fs * duration)) / fs
clean = amplitude * np.cos(2 * np.pi * f0 * t)
noisy, noise = add_noise(clean, snr_in)

h_ma = moving_average(ma_length)
h_fir = fir_lowpass(fir_length)
y_ma = apply_filter(noisy, h_ma)
y_fir = apply_filter(noisy, h_fir)

print(f"fs = {fs} Hz, f0 = {f0} Hz, {len(clean)} δειγματα")
print(f"ισχυς σηματος = {np.mean(clean ** 2):.4f} W, ισχυς θορυβου = {np.mean(noise ** 2):.4f} W")
print()
print(f"θορυβωδες σημα: SNR {snr(clean, noisy):.2f} dB, MSE {mse(clean, noisy):.4f}")
print(f"moving average M={ma_length}: SNR {snr(clean, y_ma):.2f} dB, MSE {mse(clean, y_ma):.4f}")
print(f"FIR low pass M={fir_length}: SNR {snr(clean, y_fir):.2f} dB, MSE {mse(clean, y_fir):.4f}")

# Καθυστερηση και κερδος καθε φιλτρου
for name, h in [("moving average", h_ma), ("FIR low pass", h_fir)]:
    f, mag = freq_response(h)
    gain = np.interp(f0, f, mag)
    f3db = f[np.argmax(mag < 1 / np.sqrt(2))]
    print()
    print(name)
    print(f"  καθυστερηση ομαδας {(len(h) - 1) / 2} δειγματα")
    print(f"  κερδος στα {f0} Hz = {gain:.4f} ({20 * np.log10(gain):.2f} dB)")
    print(f"  συχνοτητα -3 dB = {f3db:.1f} Hz")

# Πεδιο του χρονου
plt.figure(figsize=(11, 9))
plt.subplot(4, 1, 1)
plt.plot(t, clean)
plt.title("Clean signal")
plt.subplot(4, 1, 2)
plt.plot(t, noisy)
plt.title(f"With noise, SNR {snr_in} dB")
plt.subplot(4, 1, 3)
plt.plot(t, clean, alpha=0.5)
plt.plot(t, y_ma)
plt.title(f"Moving average M={ma_length}")
plt.subplot(4, 1, 4)
plt.plot(t, clean, alpha=0.5)
plt.plot(t, y_fir)
plt.title(f"FIR low pass M={fir_length}")
plt.xlabel("Time (s)")
plt.tight_layout()
plt.savefig("figures/fig_time_domain.png")
plt.close()

# Φασματα πριν και μετα το φιλτραρισμα
plt.figure()
for sig, label in [(noisy, "noisy"), (y_ma, "moving average"), (y_fir, "FIR"), (clean, "clean")]:
    f, mag = spectrum(sig)
    plt.plot(f, mag, label=label)
plt.xlim(0, fs / 2)
plt.xlabel("Frequency (Hz)")
plt.ylabel("Amplitude")
plt.title("Spectra")
plt.legend()
plt.savefig("figures/fig_spectra.png")
plt.close()

# Κρουστικη και συχνοτικη αποκριση των δυο φιλτρων
plt.figure(figsize=(11, 7))
plt.subplot(2, 2, 1)
plt.stem(np.arange(len(h_ma)), h_ma)
plt.title("h[n] moving average")
plt.subplot(2, 2, 3)
plt.stem(np.arange(len(h_fir)), h_fir)
plt.title("h[n] FIR")
plt.subplot(2, 2, 2)
f, mag = freq_response(h_ma)
plt.plot(f, 20 * np.log10(np.maximum(mag, 1e-6)))
plt.ylim(-60, 5)
plt.title("|H(f)| moving average")
plt.subplot(2, 2, 4)
f, mag = freq_response(h_fir)
plt.plot(f, 20 * np.log10(np.maximum(mag, 1e-6)))
plt.ylim(-60, 5)
plt.xlabel("Frequency (Hz)")
plt.title("|H(f)| FIR")
plt.tight_layout()
plt.savefig("figures/fig_filters.png")
plt.close()

# Σαρωση του μηκους, καθε σημειο ειναι μεσος ορος απο 30 δοκιμες
ma_curve = []
fir_curve = []
for m in sweep:
    s_ma = []
    s_fir = []
    for k in range(trials):
        noisy_k, _ = add_noise(clean, snr_in)
        s_ma.append(snr(clean, apply_filter(noisy_k, moving_average(m))))
        s_fir.append(snr(clean, apply_filter(noisy_k, fir_lowpass(m))))
    ma_curve.append(np.mean(s_ma))
    fir_curve.append(np.mean(s_fir))

best_ma = sweep[int(np.argmax(ma_curve))]
best_fir = sweep[int(np.argmax(fir_curve))]
print()
print(f"σαρωση μηκους, {trials} δοκιμες ανα μηκος")
print(f"  moving average: καλυτερο M = {best_ma}, SNR {max(ma_curve):.2f} dB")
print(f"  FIR low pass: καλυτερο M = {best_fir}, SNR {max(fir_curve):.2f} dB")

plt.figure()
plt.plot(sweep, ma_curve, "o-", label="moving average")
plt.plot(sweep, fir_curve, "s-", label="FIR low pass")
plt.axhline(snr_in, linestyle="--", label="input SNR")
plt.xlabel("Filter length M")
plt.ylabel("Output SNR (dB)")
plt.title("SNR vs filter length")
plt.legend()
plt.savefig("figures/fig_snr_vs_length.png")
plt.close()

print()
print("τα διαγραμματα μπηκαν στο figures/")

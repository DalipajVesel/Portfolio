import keras
from keras.models import Sequential
from keras.layers import Conv2D, MaxPooling2D, Dropout, Flatten, Dense
import os
import numpy as np
import matplotlib.pyplot as plt

# φορτώνουμε το dataset CIFAR-10
(x_train, y_train), (x_test, y_test) = keras.datasets.cifar10.load_data()

# κανονικοποίηση των pixel
x_train = x_train / 255
x_test = x_test / 255

num_classes = 10
# μετατροπή των ετικετών σε one-hot encoding
y_train = keras.utils.to_categorical(y_train, num_classes)
y_test = keras.utils.to_categorical(y_test, num_classes)

# δημιουργία του μοντέλου CNN
model = Sequential()
# είσοδος με σχήμα 32x32x3
model.add(keras.Input(shape=(32, 32, 3)))

# μπλοκ συνελικτικών στρωμάτων
model.add(Conv2D(64, (3, 3), activation='relu'))
# downsampling με max pooling
model.add(MaxPooling2D(pool_size=(2, 2)))
# αποφυγή overfitting με dropout
model.add(Dropout(0.25))

model.add(Conv2D(128, (3, 3), activation='relu'))
model.add(MaxPooling2D(pool_size=(2, 2)))
model.add(Dropout(0.3))

model.add(Conv2D(256, (3, 3), activation='relu'))
model.add(MaxPooling2D(pool_size=(2, 2)))
model.add(Dropout(0.35))

# μετατροπή σε μονοδιάστατο
model.add(Flatten())
model.add(Dense(128, activation='relu'))
model.add(Dropout(0.5))
# εξοδος πιθανότητας τον 10 κλασεων
model.add(Dense(num_classes, activation='softmax'))

# ορισμός παραμέτρων εκπαίδευσης
model.compile(loss=keras.losses.categorical_crossentropy,
              optimizer='adam',
              metrics=['accuracy'])

# δηλωλώνουμε τον αριθμό των εποχών
N = 50

# φάκελος για τα μοντέλα που αποθηκεύουμε
os.makedirs("saved_models", exist_ok=True)

# εκπαιδεύουμε μία εποχή τη φορά, για να μπορούμε να αποθηκεύουμε το μοντέλο ενδιάμεσα
for epoch in range(1, N + 1):
    # 128 εικόνες σε κάθε βήμα και μετά από κάθε εποχή μετράμε την ακρίβεια στο test set
    model.fit(
        x_train, y_train,
        batch_size=128,
        epochs=1,
        verbose=1,
        validation_data=(x_test, y_test)
    )

    # κάθε 5 εποχές αποθηκεύουμε το μοντέλο
    if epoch % 5 == 0:
        filename = f"saved_models/epoch_{epoch}.keras"
        model.save(filename)

# τελική ακρίβεια στο test set, το score[1] είναι το accuracy
score = model.evaluate(x_test, y_test, verbose=0)
print("Final model test accuracy:", score[1])

# τα ονόματα των 10 κλάσεων με τη σειρά των ετικετών του CIFAR-10
class_names = ['airplane', 'automobile', 'bird', 'cat', 'deer', 'dog', 'frog', 'horse', 'ship', 'truck']

# από one-hot πίσω στον αριθμό της κλάσης
y_test_labels = np.argmax(y_test, axis=1)

# 10 τυχαίες εικόνες του test set, με το seed 42 είναι κάθε φορά οι ίδιες
np.random.seed(42)
num_samples = 10
idx = np.random.choice(len(x_test), num_samples, replace=False)

x_vis = x_test[idx]
y_true = y_test_labels[idx]

# για κάθε αποθηκευμένο μοντέλο (εποχή 5, 10, ..., 50) κάνουμε πρόβλεψη στις ίδιες 10 εικόνες
step = 5
for epoch in range(step, N + 1, step):
    model_path = os.path.join("saved_models", f"epoch_{epoch}.keras")
    # compile=False γιατί το μοντέλο χρειάζεται μόνο για πρόβλεψη
    loaded_model = keras.models.load_model(model_path, compile=False)

    # πιθανότητα για κάθε κλάση, κρατάμε τη μεγαλύτερη
    probs = loaded_model.predict(x_vis, verbose=0)
    y_pred = np.argmax(probs, axis=1)

    # 2 γραμμές με 5 εικόνες, πάνω από κάθε εικόνα η σωστή κλάση και η πρόβλεψη
    plt.figure()
    for i in range(num_samples):
        plt.subplot(2, 5, i + 1)
        plt.imshow(x_vis[i])
        plt.title(
            "True: %s\nPredict: %s" % (class_names[y_true[i]], class_names[y_pred[i]]),
            fontsize=9
        )

    # ένα παράθυρο για κάθε μοντέλο, όταν το κλείσεις ανοίγει το επόμενο
    plt.show()

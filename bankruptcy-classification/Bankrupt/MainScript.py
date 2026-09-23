import pandas as pd
from sklearn.preprocessing import StandardScaler
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.linear_model import LogisticRegression
from sklearn.neighbors import KNeighborsClassifier
from sklearn.naive_bayes import GaussianNB
from sklearn.svm import SVC
from sklearn.neural_network import MLPClassifier
from sklearn.metrics import multilabel_confusion_matrix, precision_score, recall_score, f1_score, accuracy_score
import os

data = pd.read_excel("InputData.xlsx")
# Αφαιρούμε την στήλη με το έτος
data = data.drop(columns=["ΕΤΟΣ"])

# Εμφανίζουμε τις 5 πρώτες γραμμές
print(data.head())

scaler = StandardScaler()
# Επιλογή χαρακτηριστικών στήλες A-K δηλαδη οι 11 πρώτες
characteristics = data.iloc[:, 0:11].values

# Επιλογή της στήλης Status
status = data.iloc[:, 11].values
# Μετατροπή της στήλης Status σε δυαδική μορφή
target = np.where(status == 1, 0, 1)

# Διαχωρισμός σε train test split με stratify
X_train, X_test, Y_train, Y_test = train_test_split(
    characteristics, target, test_size=0.25, stratify=target, random_state=42
)

# Κανονικοποίηση, ο scaler μαθαίνει μόνο από το train
X_train = scaler.fit_transform(X_train)
X_test = scaler.transform(X_test)

healthy_count = np.sum(Y_train == 0)
bankrupt_count = np.sum(Y_train == 1)

ratio = healthy_count / bankrupt_count

if ratio > 3.0:
    # Random generator
    rng = np.random.default_rng()

    # Βρίσκουμε τις θέσεις των healthy και bankrupt
    healthy = np.where(Y_train == 0)[0]
    bankrupt = np.where(Y_train == 1)[0]

    healthy_ratio = int(3.0 * bankrupt_count)

    # Κανουμε downsampleing των healthy
    downsampled_healthy = rng.choice(healthy, healthy_ratio, replace=False)

    # Ενώνουμε τα downsampled healthy με τα bankrupt
    balanced = np.concatenate([downsampled_healthy, bankrupt])
    # Ανακατεύουμε το balanced dataset
    rng.shuffle(balanced)

    # Ενημερώνουμε τα X_train και Y_train
    X_train = X_train[balanced]
    Y_train = Y_train[balanced]

# Ορισμός των ταξινομητών
classifiers = [
    ("LDA", LinearDiscriminantAnalysis()),
    ("Logistic Regression", LogisticRegression(max_iter=1000)),
    ("KNN (k=5)", KNeighborsClassifier(n_neighbors=5)),
    ("KNN (k=7)", KNeighborsClassifier(n_neighbors=7)),
    ("KNN (k=9)", KNeighborsClassifier(n_neighbors=9)),
    ("GaussianNB", GaussianNB()),
    ("SVM (RBF)", SVC(kernel="rbf")),
    ("MLP (64,32)", MLPClassifier(hidden_layer_sizes=(64, 32), max_iter=400))
]

results = []

for name, clf in classifiers:
    # Εκπαίδευση του ταξινομητή
    clf.fit(X_train, Y_train)

    for set_name, x_set, y_true in [
        ("Train", X_train, Y_train),
        ("Test", X_test, Y_test)
    ]:
        # Κάνουμε προβλέψεις στο σύνολο δεδομένων
        pred = clf.predict(x_set)

        # Παιρνουμε το confusion matrix για τους υγιείς (0) και τους πτωχευμένους (1)
        ml_cm = multilabel_confusion_matrix(y_true, pred, labels=[0, 1])

        # Παιρνουμε το confusion matrix για τους πτωχευμένους
        m = ml_cm[1]

        # Αποσυσκευάζουμε τα στοιχεία του confusion matrix
        tn, fp = m[0]
        fn, tp = m[1]

        # Υπολογισμός των αξιολογίσεων
        precision = precision_score(y_true, pred, average="macro")
        recall = recall_score(y_true, pred, average="macro")
        f1 = f1_score(y_true, pred, average="macro")
        accuracy = accuracy_score(y_true, pred)

        sensitivity = tp / (tp + fn)
        specificity = tn / (tn + fp)

        # Αποθήκευση των αποτελεσμάτων
        results.append({
            "Classifier Name": name,
            "Train/Test Set": set_name,
            "Samples Count": len(y_true),
            "TP": tp,
            "TN": tn,
            "FP": fp,
            "FN": fn,
            "Precision": precision,
            "Recall": recall,
            "F1 Score": f1,
            "Accuracy": accuracy,
            "Sensitivity (Bankrupt)": sensitivity,
            "Specificity (Healthy)": specificity,

        })

if not os.path.exists("OutputData"):
    os.makedirs("OutputData")

results_df = pd.DataFrame(results)
results_df.to_excel("OutputData/classification_results.xlsx", index=False)

# Ονοματεπώνυμο: Βεσέλ Νταλίπαϊ
# ΑΜ: 161173

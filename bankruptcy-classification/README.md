# Bankruptcy Classification

Predicting if a Greek company goes bankrupt from 11 financial indicators,
with eight classifiers on the same train/test split: LDA, logistic
regression, KNN with k = 5, 7 and 9, naive Bayes, SVM and an MLP. The
healthy class is downsampled to 3:1 in the training set only, so the test
set keeps the real distribution.

The assignment asked for at least 62% sensitivity on the bankrupt companies
and 70% specificity on the healthy ones. Specificity is always above 70%,
sensitivity is under 62% in almost every run. Only 2.3% of the companies in
the data are bankrupt.

Pattern Recognition and Machine Learning course, assignment 1.

Run it from the Bankrupt folder:

    pip install pandas scikit-learn numpy openpyxl
    python MainScript.py

The scores go to OutputData/classification_results.xlsx.

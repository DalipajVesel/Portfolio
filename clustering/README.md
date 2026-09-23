# Clustering

Five clustering algorithms on three datasets (Iris, Wine, Digits): K-Means,
hierarchical with ward and complete linkage, DBSCAN, Mean Shift and spectral
clustering. The data is standardized and projected to two dimensions with
PCA, and every result is drawn as a scatter plot, with a dendrogram for the
hierarchical part.

## Results

Mean Shift with the automatic bandwidth finds 3 clusters in Iris and in Wine,
the same as the real classes, and only 1 in Digits. In two dimensions Digits
is one big cloud, because two PCA components keep very little of its 64
features, so the other methods only cut it into pieces. DBSCAN depends a lot
on eps: with 0.1 almost every point is noise and with 1.5 everything ends up
in one or two clusters.

## Build and run

    pip install scikit-learn matplotlib scipy numpy
    python clustering.py

The 63 plots are written in plots/.

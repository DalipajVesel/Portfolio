from sklearn.datasets import load_iris, load_wine, load_digits
import matplotlib.pyplot as plt
import numpy as np
from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from sklearn.cluster import KMeans, AgglomerativeClustering, DBSCAN, MeanShift, SpectralClustering, estimate_bandwidth
from sklearn.mixture import GaussianMixture
import scipy.cluster.hierarchy as sch
import os

# load datasets
datasets = [
    ("Iris", load_iris(), 3),
    ("Wine", load_wine(), 3),
    ("Digits", load_digits(), 10)
]

# set figure size for better visibility
plt.rcParams["figure.figsize"] = (10, 6)

os.makedirs("plots", exist_ok=True)

# loop through datasets
for name, data, n_classes in datasets:
    # standardize the data
    X = data.data
    X_ss = StandardScaler().fit_transform(X)
    X_pca = PCA(n_components=2).fit_transform(X_ss)

    K = [2, 3, 5]

    # K-Means Clustering
    for k in K:
        kmeans = KMeans(n_clusters=k, init='k-means++', n_init=10, random_state=0)
        y_kmeans = kmeans.fit_predict(X_pca)

        plt.scatter(X_pca[:, 0], X_pca[:, 1], c=y_kmeans, cmap='viridis')
        plt.scatter(kmeans.cluster_centers_[:, 0], kmeans.cluster_centers_[:, 1], s=300, c='red', marker='X')
        plt.title(f'K-Means for {name} with {k} clusters')
        plt.savefig(f'plots/{name}_kmeans_{k}.png')
        plt.close()

    plt.figure(figsize=(10, 5))
    dendrogram = sch.dendrogram(sch.linkage(X_pca, method='ward'))
    plt.title('Dendrogram for ' + name)
    plt.savefig(f'plots/{name}_dendrogram.png')
    plt.close()

    Linkage = ['ward', 'complete']

    for l in Linkage:
        for k in K:
            hc = AgglomerativeClustering(n_clusters=k, linkage=l)
            y_hc = hc.fit_predict(X_pca)
            plt.scatter(X_pca[:, 0], X_pca[:, 1], c=y_hc, cmap='coolwarm')
            plt.title(f'Hierarchical Clustering for {name} with {k} clusters')
            plt.savefig(f'plots/{name}_hierarchical_{l}_{k}.png')
            plt.close()

    EPS = [0.1, 0.5, 0.75, 1.5]

    for e in EPS:
        dbscan = DBSCAN(eps=e, min_samples=5)
        y_db = dbscan.fit_predict(X_pca)

        plt.scatter(X_pca[:, 0], X_pca[:, 1], c=y_db, cmap='plasma')
        plt.title(f'DBSCAN for {name} with eps={e}')
        plt.savefig(f'plots/{name}_dbscan_{e}.png')
        plt.close()

    bandwidth_auto = estimate_bandwidth(X_pca, quantile=0.2, n_samples=300)

    ms = MeanShift(bandwidth=bandwidth_auto, bin_seeding=True)
    y_ms = ms.fit_predict(X_pca)

    plt.scatter(X_pca[:, 0], X_pca[:, 1], c=y_ms, cmap='viridis')
    plt.title(f'Mean Shift for {name} with auto bandwidth={bandwidth_auto} (Found {len(np.unique(y_ms))} clusters)')
    plt.savefig(f'plots/{name}_meanshift_auto.png')
    plt.close()

    bandwidth_manual = [0.5, 1.0, 1.5]

    for b in bandwidth_manual:
        ms = MeanShift(bandwidth=b, bin_seeding=True)
        y_ms = ms.fit_predict(X_pca)

        plt.scatter(X_pca[:, 0], X_pca[:, 1], c=y_ms, cmap='viridis')
        plt.title(f'Mean Shift for {name} with bandwidth={b} (Found {len(np.unique(y_ms))} clusters)')
        plt.savefig(f'plots/{name}_meanshift_{b}.png')
        plt.close()

    for k in K:
        spectral = SpectralClustering(n_clusters=k, affinity='nearest_neighbors', assign_labels='kmeans')
        y_spectral = spectral.fit_predict(X_pca)

        plt.scatter(X_pca[:, 0], X_pca[:, 1], c=y_spectral, cmap='winter')
        plt.title(f'Spectral Clustering for {name} with {k} clusters')
        plt.savefig(f'plots/{name}_spectral_{k}.png')
        plt.close()

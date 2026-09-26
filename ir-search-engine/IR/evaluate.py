import json

from sklearn.metrics import precision_score, recall_score, f1_score

from backend_search_engine import preprocess_query, boolean_search, tfidf_ranking, bm25_ranking


def load_data():
    try:
        with open("inverted_index_data.json", "r") as inverted_index_file:
            inverted_index = json.load(inverted_index_file)

        with open("processed_data.json", "r") as processed_data_file:
            processed_data = json.load(processed_data_file)

        return processed_data, inverted_index

    except FileNotFoundError:
        print("Error loading files")

        return None, None


def calculate_evaluations(retrieved_docs, relevant_docs, total_docs):
    # τα έγγραφα αριθμούνται από το 1 έως total_docs
    y_true = [1 if i in relevant_docs else 0 for i in range(1, total_docs + 1)]
    y_pred = [1 if i in retrieved_docs else 0 for i in range(1, total_docs + 1)]

    precision = precision_score(y_true, y_pred, zero_division=0)

    recall = recall_score(y_true, y_pred, zero_division=0)

    f1 = f1_score(y_true, y_pred, zero_division=0)

    return precision, recall, f1


# πόσα από τα πρώτα k αποτελέσματα είναι σχετικά
def calculate_precision_at_k(ranked_docs, relevant_docs, k):
    hits = [doc for doc in ranked_docs[:k] if doc in relevant_docs]

    return len(hits) / k


# κάθε φορά που βρίσκουμε σχετικό κρατάμε το precision μέχρι εκείνη τη θέση,
# όσα σχετικά δεν βρέθηκαν μετράνε 0
def calculate_average_precision(ranked_docs, relevant_docs):
    found = 0
    total = 0

    for i in range(len(ranked_docs)):
        if ranked_docs[i] in relevant_docs:
            found += 1
            total += found / (i + 1)

    return total / len(relevant_docs)


def main():
    processed_data, inverted_index = load_data()

    if not processed_data or not inverted_index:
        print("Error loading data. Exiting.")
        return

    queries = [
        "information retrieval",
        "machine learning",
        "women's rights",
        "military coup",
        "television broadcasting",
        "environmental sustainability",
    ]

    # σχετικά είναι τα άρθρα που μιλάνε για το θέμα, όχι όσα έχουν απλώς τις λέξεις
    relevant_docs = {
        "information retrieval": [1, 3],
        "machine learning": [2],
        "women's rights": [9, 15, 17, 18, 28, 32],
        "military coup": [36, 38, 49],
        "television broadcasting": [10, 19, 24, 31],
        "environmental sustainability": [29, 30, 46],
    }

    total_docs = len(processed_data)

    tfidf_average_precisions = []
    bm25_average_precisions = []

    for query in queries:
        query_terms = preprocess_query(query)

        # το NOT δεν το αξιολογούμε, δίνει τα άρθρα που δεν έχουν τους όρους
        and_result = boolean_search(query_terms, inverted_index, "AND")
        or_result = boolean_search(query_terms, inverted_index, "OR")
        # το tf-idf και το bm25 δίνουν θέσεις από το 0, τις κάνουμε ids από το 1
        tfidf_result = [doc_id + 1 for doc_id, _ in tfidf_ranking(query_terms, processed_data)]
        bm25_result = [doc_id + 1 for doc_id, _ in bm25_ranking(query_terms, processed_data)]

        print(f"\nEvaluation for: {query}")

        # το boolean δίνει λίστα χωρίς σειρά
        for method, result in [("BOOLEAN AND", and_result), ("BOOLEAN OR", or_result)]:
            precision, recall, f1 = calculate_evaluations(result, relevant_docs[query], total_docs)

            print(f"\n{method}:")
            print(f"Precision: {precision:.4f}")
            print(f"Recall: {recall:.4f}")
            print(f"F1-Score: {f1:.4f}")

        # στο tf-idf και στο bm25 μετράει και η σειρά
        for method, result in [("TF-IDF", tfidf_result), ("BM25", bm25_result)]:
            precision_at_5 = calculate_precision_at_k(result, relevant_docs[query], 5)
            average_precision = calculate_average_precision(result, relevant_docs[query])

            if method == "TF-IDF":
                tfidf_average_precisions.append(average_precision)
            else:
                bm25_average_precisions.append(average_precision)

            print(f"\n{method}:")
            print(f"Precision at 5: {precision_at_5:.4f}")
            print(f"Average Precision: {average_precision:.4f}")

    # το mean average precision είναι ο μέσος όρος των average precision όλων των queries
    tfidf_mean_average_precision = sum(tfidf_average_precisions) / len(tfidf_average_precisions)
    bm25_mean_average_precision = sum(bm25_average_precisions) / len(bm25_average_precisions)

    print("\nMean Average Precision:")
    print(f"TF-IDF: {tfidf_mean_average_precision:.4f}")
    print(f"BM25: {bm25_mean_average_precision:.4f}")


if __name__ == "__main__":
    main()

from backend_search_engine import preprocess_query, boolean_search, tfidf_ranking, bm25_ranking
from sklearn.metrics import precision_score, recall_score, f1_score, average_precision_score
import json


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

    avg_precision = average_precision_score(y_true, y_pred)

    return precision, recall, f1, avg_precision


def main():
    processed_data, inverted_index = load_data()

    if not processed_data or not inverted_index:
        print("Error loading data. Exiting.")
        return

    queries = [
        "information",
        "information retrieval",
        "machine learning"]

    relevant_docs = {
        "information": [1, 3, 4, 6, 8, 10, 13, 15, 19, 21, 25, 29, 35, 36, 37, 44, 48, 49],
        "information retrieval": [1, 3, 4, 6, 7, 8, 10, 13, 15, 19, 21, 25, 29, 35, 36, 37, 44, 48, 49],
        "machine learning": [1, 2, 6, 7, 8, 13, 25, 33, 35, 36, 38, 44, 46, 48, 50],
    }

    total_docs = len(processed_data)

    for query in queries:

        query_terms = preprocess_query(query)

        and_result = boolean_search(query_terms, inverted_index, "AND")
        or_result = boolean_search(query_terms, inverted_index, "OR")
        not_result = boolean_search(query_terms, inverted_index, "NOT")
        # το tf-idf και το bm25 δίνουν θέσεις από το 0, τις κάνουμε ids από το 1
        tfidf_result = [doc_id + 1 for doc_id, _ in tfidf_ranking(query_terms, processed_data)]
        bm25_result = [doc_id + 1 for doc_id, _ in bm25_ranking(query_terms, processed_data)]

        print(f"\nEvaluation for: {query}")

        for method, result in [("BOOLEAN AND", and_result), ("BOOLEAN OR", or_result), ("BOOLEAN NOT", not_result),
                               ("TF-IDF", tfidf_result), ("BM25", bm25_result)]:
            precision, recall, f1, avg_precision = calculate_evaluations(result, relevant_docs[query], total_docs)

            print(f"\n{method}:")
            print(f"  Precision: {precision:.4f}")
            print(f"  Recall: {recall:.4f}")
            print(f"  F1-Score: {f1:.4f}")
            print(f"  MAP: {avg_precision:.4f}")


if __name__ == "__main__":
    main()

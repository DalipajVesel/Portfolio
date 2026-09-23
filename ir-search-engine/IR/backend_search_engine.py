import json
import numpy as np
from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords
from nltk.stem import WordNetLemmatizer
from sklearn.feature_extraction.text import TfidfVectorizer
from rank_bm25 import BM25Okapi


def load_data():
    try:
        with open("data.json", "r") as data_file:
            data = json.load(data_file)

        with open("inverted_index_data.json", "r") as inverted_index_file:
            inverted_index = json.load(inverted_index_file)

        with open("processed_data.json", "r") as processed_data_file:
            processed_data = json.load(processed_data_file)

        return data, processed_data, inverted_index

    except FileNotFoundError:
        print("Error loading files")

        return None, None, None


def preprocess_query(query):
    lemmatizer = WordNetLemmatizer()
    stop_words = set(stopwords.words("english"))
    tokens = word_tokenize(query.lower())
    tokens = [word for word in tokens if word.isalpha()]
    tokens = [word for word in tokens if word not in stop_words]
    tokens = [lemmatizer.lemmatize(word) for word in tokens]

    return tokens


def boolean_search(query, inverted_index, operator):
    all_ids = sorted({int(i) for index in inverted_index.values() for i in index.keys()})

    result_lists = [{int(doc_id) for doc_id in inverted_index.get(term, {}).keys()} for term in query]

    if operator == "AND":
        return list(set.intersection(*result_lists)) if result_lists else []

    elif operator == "OR":
        return sorted(set.union(*result_lists)) if result_lists else []

    elif operator == "NOT":
        result_union = set.union(*result_lists) if result_lists else set()
        return [i for i in all_ids if i not in result_union]


def tfidf_ranking(query, processed_data):
    vectorizer = TfidfVectorizer()
    tfidf_matrix = vectorizer.fit_transform(processed_data)
    query_vector = vectorizer.transform([" ".join(query)])

    scores = np.dot(tfidf_matrix, query_vector.T).toarray().flatten()
    rank = np.argsort(scores)
    rank = np.flip(rank)

    result = []
    for i in rank:
        if scores[i] > 0:
            result.append((i, scores[i]))

    return result


def bm25_ranking(query, processed_data):
    tokenized_data = [data.split() for data in processed_data]

    bm25 = BM25Okapi(tokenized_data)
    scores = bm25.get_scores(query)

    rank = np.argsort(scores)
    rank = np.flip(rank)

    result = []
    for i in rank:
        if scores[i] > 0:
            result.append((i, scores[i]))

    return result

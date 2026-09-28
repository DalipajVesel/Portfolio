# IR Search Engine

A small search engine over Wikipedia articles: a crawler, text
preprocessing, an inverted index, three retrieval models (Boolean, TF-IDF
with cosine similarity, BM25) and an evaluation with precision, recall, F1
score and mean average precision. interface_search_engine.ipynb is a search
box made with ipywidgets. search_engine.ipynb runs all the steps in
order, from the crawler to the evaluation.

Information Retrieval course.

## Results

evaluate.py has six queries. I marked an article as relevant when it is
about the topic of the query, not when it only has the words in it. Boolean
AND and OR are scored with precision, recall and F1 score, and TF-IDF and
BM25 with precision at 5 and average precision, because there the order
counts too.

BM25 gets a mean average precision of 0.93 and TF-IDF 0.83. Most of the
difference comes from machine learning, where TF-IDF puts Education first
because it has learning 61 times. Paragon Cable is a cable company but never
says television or broadcasting, so none of the models finds it.

## Build and run

    pip install requests beautifulsoup4 nltk scikit-learn rank_bm25 numpy ipywidgets
    python -m nltk.downloader punkt_tab stopwords wordnet
    python evaluate.py

The JSON files here are the 50-article corpus the relevance judgments in
evaluate.py were written for. wiki_scraper.py crawls 50 new articles into
data.json, and text_processing.py and inverted_index.py build the other two
files from it. The crawler takes the next link from a set, so the articles
end up on mixed topics, and 6 and 48 are the same article, Association for
Computing Machinery, the second time through the SIGSIM redirect.

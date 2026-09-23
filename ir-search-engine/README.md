# IR Search Engine

A small search engine over Wikipedia articles: a crawler, text
preprocessing, an inverted index, three retrieval models (Boolean, TF-IDF
with cosine similarity, BM25) and an evaluation with precision, recall, F1
and MAP. interface_search_engine.ipynb is a search box made with
ipywidgets.

Information Retrieval course.

## Build and run

    pip install requests beautifulsoup4 nltk scikit-learn rank_bm25 numpy ipywidgets
    python -m nltk.downloader punkt_tab stopwords wordnet
    python evaluate.py

The JSON files here are the 50-article corpus the relevance judgments in
evaluate.py were written for. wiki_scraper.py crawls 50 new articles into
data.json, and text_processing.py and inverted_index.py build the other two
files from it.

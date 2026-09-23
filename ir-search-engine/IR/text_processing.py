import json
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
from nltk.stem import WordNetLemmatizer

try:
    with open("data.json", "r") as file:
        data = json.load(file)
except FileNotFoundError:
    print("File not found")
    exit(0)
    
lemmatizer = WordNetLemmatizer()
stop_words = set(stopwords.words("english"))

processed_data = []

for article in data:
    content = article.get("content", "").strip()
    if not content:
        continue
    
    tokenized_words = word_tokenize(content)
    tokenized_words = [word.lower() for word in tokenized_words]
    tokenized_words = [word for word in tokenized_words if word.isalpha()]
    tokenized_words = [word for word in tokenized_words if word not in stop_words]
    tokenized_words = [lemmatizer.lemmatize(word) for word in tokenized_words]
    
    processed_data.append(" ".join(tokenized_words))

with open("processed_data.json", "w") as file:
    json.dump(processed_data, file, indent=4)

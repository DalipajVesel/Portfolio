import json

try:
    with open("processed_data.json", "r") as file:
        data = json.load(file)
except FileNotFoundError:
    print("File not found")
    exit(0)

inverted_index = {}

for doc_id, article in enumerate(data, start=1):

    words = article.split()
    for word in words:
        if word not in inverted_index:
            inverted_index[word] = {}
        if doc_id not in inverted_index[word]:
            inverted_index[word][doc_id] = 0
        inverted_index[word][doc_id] += 1

with open("inverted_index_data.json", "w") as file:
    json.dump(inverted_index, file, indent=4)

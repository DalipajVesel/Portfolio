import requests
from bs4 import BeautifulSoup
import json

url = "https://en.wikipedia.org/wiki/Information_retrieval"

max_articles = 50

articles = []
visited_url = set()
url_scrape = {url}

while url_scrape and len(articles) < max_articles:
    current_url = url_scrape.pop()
    
    if current_url in visited_url:
        continue

    try:
        response = requests.get(current_url, headers={"User-Agent": "IR-LabProject/1.0 (student project)"})
        response.raise_for_status()
        
        soup = BeautifulSoup(response.text, 'html.parser')

        article_title = soup.find('h1', {'id': 'firstHeading'}).text.strip()
        paragraphs = soup.find_all('p')
        content = " ".join([para.text for para in paragraphs if para.text])
        
        articles.append({
            "title": article_title,
            "url": current_url,
            "content": content})

        visited_url.add(current_url)

        for link in soup.find_all('a', href=True):
            links = link['href'].replace("https://en.wikipedia.org", "")
            if links.startswith('/wiki/') and not links.startswith('/wiki/Special:') and ':' not in links:
                full_url = f"https://en.wikipedia.org{links}"
                if full_url not in visited_url:
                    url_scrape.add(full_url)
    
    except requests.RequestException:
        print("Request failed")

with open("data.json", "w") as file:
    json.dump(articles, file, indent=4)

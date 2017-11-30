# OS-Web-Crawler

A simple web crawler

```
WebCrawler crawler = new WebCrawler("https://www.github.com");

Search imageSearch = new TextSearch("github") {
    @Override
    public void onFoundString(String matchedString, int characterIndex) {
        System.out.println("Found '" + matchedString + "' at " + characterIndex);
    }
};

crawler.crawl(1, imageSearch);
```

XMLTagSearch:

```
Search linkSearch = new XMLTagSearch("a") {
    @Override
    public void onFoundTag(HashMap<String, String> attributes, String tagContents) {
        String href = attributes.get("href");
        if (href != null) {
           System.out.println("Found link to: " + href);
        }
    }
};
```

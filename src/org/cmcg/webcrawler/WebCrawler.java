package org.cmcg.webcrawler;

import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.cmcg.logger.Log;
import org.cmcg.webcrawler.search.Search;
import org.cmcg.webcrawler.search.XMLTagSearch;

public class WebCrawler {

    private String homePage;
    private String rootUrl;

    public WebCrawler(String rootUrl) {
        this.rootUrl = rootUrl;
        this.homePage = rootUrl.replaceAll("www.", "").replaceAll("http://", "").replaceAll("https://", "");
    }

    /**
     * 
     * @param maxPages
     * @param searches
     */
    public void crawl(int maxPages, Search... searches) {

        ArrayList<URL> visitedPages = new ArrayList<URL>();
        ArrayList<URL> invalidPages = new ArrayList<URL>();
        ArrayList<String> pagesToVisit = new ArrayList<String>();

        Search linkSearch = new XMLTagSearch("a") {
            @Override
            public void onFoundTag(HashMap<String, String> attributes, String tagContents) {

                String href = attributes.get("href");
                if (href == null) {
                    return;
                }

                if (href.startsWith("\"") || href.startsWith("'")) {// Opening quote
                    href = href.substring(1);
                }
                if (href.endsWith("\"") || href.endsWith("'")) {// Closing quote
                    href = href.substring(0, href.length() - 1);
                }

                if (!href.contains(homePage) && !href.startsWith("/")) {// Check if link if external
                    Log.debug("Skipping external page: " + href);
                    return;
                }
                pagesToVisit.add(href);
            }

        };

        Search[] temp = new Search[searches.length + 1];
        temp[0] = linkSearch;
        for (int i = 1; i < temp.length; i++) {
            temp[i] = searches[i - 1];
        }
        searches = temp;

        PageReader currentPage = null;

        try {
            currentPage = new PageReader(rootUrl);
        } catch (Exception e) {
            Log.error("Invalid homepage" + Arrays.toString(e.getStackTrace()));
            return;
        }

        while (currentPage != null && (visitedPages.size() < maxPages || maxPages == 0)) {
            try {
                if (!visitedPages.contains(currentPage.getUrl()) && !invalidPages.contains(currentPage.getUrl())) {
                    Log.log("Searching page: " + currentPage.toString());
                    currentPage.searchPage(searches);
                    visitedPages.add(currentPage.getUrl());
                } else {
                    Log.debug("Skipping already visited page: " + currentPage.toString());
                }

                if (pagesToVisit.isEmpty()) {
                    // Out of pages
                    break;
                }

                String nextUrl = pagesToVisit.remove(0);
                if (nextUrl.startsWith("/")) {
                    nextUrl = rootUrl + nextUrl;
                }

                if (nextUrl.startsWith("\"") || nextUrl.startsWith("'")) {// Opening quote
                    nextUrl = nextUrl.substring(1);
                }

                try {
                    currentPage = new PageReader(nextUrl);
                } catch (Exception e) {
                    Log.error("Error creating URL: " + nextUrl + Arrays.toString(e.getStackTrace()));
                }
            } catch (SocketException e) {
                if (currentPage != null) {
                    pagesToVisit.add(currentPage.toString());
                }
            } catch (IOException e) {
                if (currentPage != null) {
                    Log.error("Error on URL: " + currentPage.toString());
                    invalidPages.add(currentPage.getUrl());
                }
                e.printStackTrace();
            } catch (OutOfMemoryError e) {
                Log.error("Ran out of memory" + Arrays.toString(e.getStackTrace()));
                visitedPages = null;
                break;
            }
        }

        Log.log("Searched through " + visitedPages.size() + " pages");

    }

}

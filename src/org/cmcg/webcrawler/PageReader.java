/*
* Nik M
* https://github.com/c-mcg/Web-Crawler
*/
package org.cmcg.webcrawler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.cmcg.logger.Log;
import org.cmcg.webcrawler.search.Search;

public class PageReader {
    
    private URL pageUrl;
    private String url;

    public PageReader(String pageUrl) throws MalformedURLException {
        this.pageUrl = new URL(pageUrl);
        this.url = pageUrl;
    }
    
    public void searchPage(Search... searches) throws IOException {
        InputStream is;
        try {
            is = pageUrl.openStream();
        } catch (FileNotFoundException e) {
            Log.log("Skipping non-existent page: " + url);
            return;
        }
        
        for (Search search : searches) {
            search.initialize(this.url);
        }
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        char c;
        int i;
        while ((i = br.read()) != -1) {
            c = (char) i;
                for (Search search : searches) {
                    search.processChar(c);
                }
        }
        
        for (Search search : searches) {
            search.reset();
            }
        
        is.close();
        br.close();
    }
    
    public String downloadPage() throws IOException {
        InputStream is = pageUrl.openStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        
        StringBuilder page = new StringBuilder();
        
        char c;
        while ((c = (char) br.read()) != -1) {
            page.append(c);
        }
        
        is.close();
        br.close();
        return page.toString();
    }
    
    public URL getUrl() {
        return pageUrl;
    }
    
    public String toString() {
        return url;
    }
    
    public boolean Equals(Object o) {
        if (o instanceof PageReader) {
            return ((PageReader) o).pageUrl.equals(pageUrl);
        }
        return o == this;
    }
    
}

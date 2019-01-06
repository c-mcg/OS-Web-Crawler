/*
* Nik M
* https://github.com/nik-m2/Web-Crawler
*/
package org.nikm.webcrawler.search;

/**
 * Search Utilities
 */
public class SearchUtil {
    
    /**
     * Executes the specified searches on the specified string.
     * Equivalent to <code>executeSearch(toSearch, "", searches)</code>
     * @param toSearch The string to perform the searches on
     * @param searches The searches to perform
     */
    public static void executeSearch(String toSearch, Search... searches) {
        executeSearch(toSearch, "", searches);
    }

    /**
     * Executes the specified searches on the specified string.
     * @param toSearch The string to perform the searches on
     * @param pageUrl The page url to give to the searches
     * @param searches The searches to perform
     */
    public static void executeSearch(String toSearch, String pageUrl, Search... searches) {
        for (Search s : searches) {
            s.initialize(pageUrl);
        }
        for (int i = 0; i < toSearch.length(); i++) {
            char c = toSearch.charAt(i);
            for (Search s : searches) {
                s.processChar(c);
            }
        }
        for (Search s : searches) {
            s.reset();
        }
    }
    
}

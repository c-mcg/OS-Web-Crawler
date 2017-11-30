package org.cmcg.webcrawler.search;

/**
 * A generic search object that is fed a stream of characters
 */
public abstract class Search {
    
    protected String pageUrl;
    
    /**
     * Called before processing begins
     * @param pageUrl The url of the page
     */
    public void initialize(String pageUrl) {
        this.pageUrl = pageUrl;
        reset();
    }

    /**
     * Processes a character
     * @param c The character to process
     * @return true if the char contributes to a possible match false if not
     */
    public abstract boolean processChar(char c);
    
    /**
     * Called after a page is processed
     */
    public abstract void reset();
    
}

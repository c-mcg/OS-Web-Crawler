/*
* Connor McG
* https://github.com/c-mcg/Web-Crawler
*/
package org.cmcg.webcrawler.search;

/**
 * A Search object that searches for text
 */
public abstract class TextSearch extends Search {

    protected String stringToFind;
    protected boolean caseSensitive;
    
    protected int numCharsProcessed;
    protected int currentIndex;
    protected String matchedString;
    
    /**
     * Creates a case insensitive text search
     * @param stringtoFind The text to search for
     */
    public TextSearch(String stringToFind) {
        this(stringToFind, false);
    }
    
    /**
     * 
     * @param stringtoFind The text to search for
     * @param caseSensitive if true matches must have the same character case as the search 
     */
    public TextSearch(String stringToFind, boolean caseSensitive) {
        this.stringToFind = stringToFind;
        this.caseSensitive = caseSensitive;
    }
    
    /**
     * Called when the string is found
     * @param matchedString The string that was matched
     * @param characterIndex The character index for the start of the match
     */
    public abstract void onFoundString(String matchedString, int characterIndex);
    
    @Override
    public boolean processChar(char c) {
        char toMatch = stringToFind.charAt(currentIndex);
        if (c == toMatch  || (!caseSensitive && 
                Character.toLowerCase(c) == Character.toLowerCase(toMatch))) {
            matchedString += c;
            currentIndex++;
            if (currentIndex == stringToFind.length()) {
                onFoundString(matchedString, numCharsProcessed - currentIndex);
                matchedString = "";
                currentIndex = 0;
            }
            return true;
        } else if (currentIndex != 0) {
            matchedString = "";
            currentIndex = 0;
        }
        numCharsProcessed++;
        return false;
    }
    
    @Override
    public void reset() {
        this.numCharsProcessed = 0;
        this.currentIndex = 0;
        this.matchedString = "";
    }
    
}

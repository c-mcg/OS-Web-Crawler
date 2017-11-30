/*
* Connor McG
* https://github.com/c-mcg/Web-Crawler
*/
package org.cmcg.webcrawler.search;

import java.util.HashMap;

/**
 * A search object that searches for xml tags and returns it's attributes
 */
public abstract class XMLTagSearch extends TextSearch {
    
    private enum State {
        SEARCHING_FOR_TAG,
        CHECKING_TAG,
        CONFIRMING_TAG,
        NEED_SPACE_TO_CONFIRM,
        LOOKING_FOR_ATTRIBUTE,
        BUILDING_ATTRIBUTE,
        LOOKING_FOR_EQUAL_SIGN,
        LOOKING_FOR_ATTRIBUTE_VALUE,
        BUILDING_ATTRIBUTE_VALUE,
    }
    
    private State state;
    private boolean tagConfirmed;
    private boolean inQuotes;
    private boolean inDoubleQuotes;
    private HashMap<String, String> tagAttributes;
    private StringBuilder attributeName;
    private StringBuilder attributeValue;

    /**
     * @param tagToFind The xml tag to find
     */
    public XMLTagSearch(String tagToFind) {
        super(tagToFind);
        resetSearch();
    }
    
    /**
     * Called when the specified tag is found
     * @param attributes The tag's attributes
     * @param tagContents The tag's contents (TODO always empty at the moment)
     */
    public abstract void onFoundTag(HashMap<String, String> attributes, String tagContents);
    
    private void resetSearch() {
        state = State.SEARCHING_FOR_TAG;
        tagConfirmed = false;
        inQuotes = false;
        inDoubleQuotes = false;
        tagAttributes = new HashMap<String, String>();
        attributeName = new StringBuilder();
        attributeValue = new StringBuilder();
    }
    
    private void submitSearch() {
        onFoundTag(tagAttributes, "");
        resetSearch();
    }
    
    @Override
    public boolean processChar(char c) {
        switch(state) {
            case SEARCHING_FOR_TAG:
                if (c == '<') {//Found a tag to check
                    state = State.CHECKING_TAG;
                }
                break;
                
            case CHECKING_TAG:
                if (c == '>') {//Didn't find the tag
                    resetSearch();
                    break;
                }
                if (super.processChar(c)) {//Search for the start of a match
                    state = State.CONFIRMING_TAG;
                }
                super.numCharsProcessed--;//To counteract super.processChar
                break;
                
            case CONFIRMING_TAG://Confirm the rest of the match
                if (super.processChar(c) ||
                    (stringToFind.length() == 1 && Character.isWhitespace(c))) {
                    if (tagConfirmed) {
                        state = State.LOOKING_FOR_ATTRIBUTE;
                    }
                } else if (c == '>') {
                    resetSearch();
                } else {
                    state = State.CHECKING_TAG;
                }
                super.numCharsProcessed--;//To counteract super.processChar
                break;
                
            case NEED_SPACE_TO_CONFIRM:
                if (Character.isWhitespace(c)) {
                    state = State.LOOKING_FOR_ATTRIBUTE;
                    break;
                }
                state = State.CHECKING_TAG;
                break;
            
            case LOOKING_FOR_ATTRIBUTE:
                if (c == '>') {
                    submitSearch();
                }
                if (Character.isWhitespace(c)) {
                    break;
                }
                attributeName.append(c);
                state = State.BUILDING_ATTRIBUTE;
                break;
                
            case BUILDING_ATTRIBUTE:
                if (c == '>') {
                    submitSearch();
                }
                if (Character.isWhitespace(c)) {
                    state = State.LOOKING_FOR_EQUAL_SIGN;
                    break;
                }
                if (c == '=') {
                    state = State.BUILDING_ATTRIBUTE_VALUE;
                    break;
                }
                attributeName.append(c);
                break;
                
            case LOOKING_FOR_EQUAL_SIGN:
                if (c == '>') {
                    submitSearch();
                }
                if (Character.isWhitespace(c)) {
                    break;
                }
                if (c == '=') {
                    state = State.LOOKING_FOR_ATTRIBUTE_VALUE;
                    break;
                }
                attributeName = new StringBuilder();
                attributeName.append(c);
                state = State.BUILDING_ATTRIBUTE;
                break;
                
            case LOOKING_FOR_ATTRIBUTE_VALUE:
                if (c == '>') {
                    submitSearch();
                }
                if (Character.isWhitespace(c)) {
                    break;
                }
                if (c == '\'') {
                    inQuotes = true;
                }
                if (c == '"') {
                    inDoubleQuotes = true;
                }
                attributeValue.append(c);
                state = State.BUILDING_ATTRIBUTE_VALUE;
                break;
                
            case BUILDING_ATTRIBUTE_VALUE:
                if (c == '\'' && !inDoubleQuotes) {
                    inQuotes = !inQuotes;
                    if (!inQuotes) {
                        inDoubleQuotes = false;
                        attributeValue.append(c);
                        c = ' ';
                    }
                }
                if (c == '"' && !inQuotes) {
                    inDoubleQuotes = !inDoubleQuotes;
                    if (!inDoubleQuotes) {
                        inQuotes = false;
                        attributeValue.append(c);
                        c = ' ';
                    }
                }
                if ((Character.isWhitespace(c) || c == '>') && !inQuotes && !inDoubleQuotes) {
                    tagAttributes.put(attributeName.toString(), attributeValue.toString());
                    attributeName = new StringBuilder();
                    attributeValue = new StringBuilder();
                    state = State.LOOKING_FOR_ATTRIBUTE;
                }
                if (c == '>') {
                    submitSearch();
                    break;
                }
                attributeValue.append(c);
                break;
                
            default:
                throw new RuntimeException("State does not have handler: " + state);
            
        }
        super.numCharsProcessed++;
        return false;
    }
    
    @Override
    public void reset() {
        super.reset();
        this.resetSearch();
    }
    
    @Override
    public void onFoundString(String matchedString, int characterIndex) {
        tagConfirmed = true;
    }

}

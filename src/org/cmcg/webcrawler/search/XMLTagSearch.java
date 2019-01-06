/*
* Nik M
* https://github.com/c-mcg/Web-Crawler
*/
package org.cmcg.webcrawler.search;

import java.util.HashMap;

/**
 * A search object that searches for xml tags and returns it's attributes
 */
public abstract class XMLTagSearch extends TextSearch {
    
    private enum State {
        SEARCHING_FOR_TAG_OPENING,
        SEARCHING_FOR_TAG,
        BUILDING_TAG,
        NEED_SPACE_TO_CONFIRM,
        SEARCHING_FOR_ATTRIBUTE_NAME,
        BUILDING_ATTRIBUTE_NAME,
        SEARCHING_FOR_EQUAL_SIGN,
        SEARCHING_FOR_ATTRIBUTE_VALUE,
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
        state = State.SEARCHING_FOR_TAG_OPENING;
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
            case SEARCHING_FOR_TAG_OPENING:
            	
            	//Look for the opening of a tag
                if (c == '<') {
                    state = State.SEARCHING_FOR_TAG;
                }
                
                //Continue otherwise
                
                break;
                
            case SEARCHING_FOR_TAG:
            	
            	//Reset if we found the closing bracket but no tag
                if (c == '>') {
                    resetSearch();
                    break;
                }
                
                //Preemptively set this because processChar may change the state
                //This switch will happen if we found the first letter, and the tag length is greater than 1
                state = State.BUILDING_TAG;
                
                //Cancel the preemptive state switch if we didn't find anything
                //This will switch the state to SEARCHING_FOR_ATTRIBUTE_NAME if the tag is 1 letter and if found
                if (!super.processChar(c)) {
                    state = State.SEARCHING_FOR_TAG;
                }
                
                //TODO this is to counteract TextSearch.processChar, but really we shouldn't be adding here if we don't need
                super.numCharsProcessed--;//To counteract super.processChar
                
                break;
            
            //We have found the opening bracket, and the first letter of the tag
            //We know the length of the tag is more than 1
            case BUILDING_TAG:
            	
            	//Continue looking for the tag
                if (super.processChar(c)) {
                	
                } else if (c == '>') {
                    resetSearch();
                } else {
                    state = State.SEARCHING_FOR_TAG;
                }
                
                //To counteract super.processChar
                super.numCharsProcessed--;//To counteract super.processChar
                
                break;
            
            case SEARCHING_FOR_ATTRIBUTE_NAME:
                if (c == '>') {
                    submitSearch();
                }
                if (Character.isWhitespace(c)) {
                    break;
                }
                attributeName.append(c);
                state = State.BUILDING_ATTRIBUTE_NAME;
                break;
                
            case BUILDING_ATTRIBUTE_NAME:
                if (c == '>') {
                    submitSearch();
                }
                if (Character.isWhitespace(c)) {
                    state = State.SEARCHING_FOR_EQUAL_SIGN;
                    break;
                }
                if (c == '=') {
                    state = State.BUILDING_ATTRIBUTE_VALUE;
                    break;
                }
                attributeName.append(c);
                break;
                
            case SEARCHING_FOR_EQUAL_SIGN:
                if (c == '>') {
                    submitSearch();
                }
                if (Character.isWhitespace(c)) {
                    break;
                }
                if (c == '=') {
                    state = State.SEARCHING_FOR_ATTRIBUTE_VALUE;
                    break;
                }
                attributeName = new StringBuilder();
                attributeName.append(c);
                state = State.BUILDING_ATTRIBUTE_NAME;
                break;
                
            case SEARCHING_FOR_ATTRIBUTE_VALUE:
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
                    state = State.SEARCHING_FOR_ATTRIBUTE_NAME;
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
        switch(state) {
        
	        case SEARCHING_FOR_TAG:
	        case BUILDING_TAG:
	        	state = State.SEARCHING_FOR_ATTRIBUTE_NAME;
	        	break;
        
        }
    }

}

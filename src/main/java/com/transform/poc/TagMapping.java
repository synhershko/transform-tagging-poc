package com.transform.poc;

import java.util.Collection;
import java.util.List;

/**
 * Created by synhershko on 9/18/14.
 */
public class TagMapping {
    private String tagName;
    private Collection<String> keywords;

    public TagMapping(String name, Collection<String> keywords) {
        tagName = name;
        this.keywords = keywords;
    }

    public Collection<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Collection<String> keywords) {
        this.keywords = keywords;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}

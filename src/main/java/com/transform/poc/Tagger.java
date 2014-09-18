package com.transform.poc;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by synhershko on 9/18/14.
 */
public interface Tagger extends Closeable {
    public Collection<TagMatch> getTags(Document doc) throws IOException;
    public void registerTag(String tagName, Iterable<String> keywords) throws IOException;
}

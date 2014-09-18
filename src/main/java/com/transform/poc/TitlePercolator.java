package com.transform.poc;

import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by synhershko on 9/18/14.
 */
public class TitlePercolator extends Percolator {
    public Collection<TagMatch> getTags(Document doc) throws IOException {
        //Build a document to check against the percolator
        XContentBuilder docBuilder = XContentFactory.jsonBuilder().startObject();
        docBuilder.field("doc").startObject(); //This is needed to designate the document
        docBuilder.field("title", doc.getTitle());
        docBuilder.endObject(); //End of the doc field
        docBuilder.endObject(); //End of the JSON root object

        // Percolate
        PercolateResponse response = client.preparePercolate()
                .setIndices("tagging-index")
                .setDocumentType("nutch-document")
                .setSource(docBuilder).execute().actionGet();

        //Iterate over the results
        final List<TagMatch> ret = new ArrayList<TagMatch>();
        for(PercolateResponse.Match match : response) {
            // match.getScore() now contains the score, e.g. tag confidence
            // Obviously, this "confidence" is tightly coupled with the actual
            // query and index mapping used
            ret.add(new TagMatch(match.getId().string(), match.getScore()));
        }
        return ret;
    }
}

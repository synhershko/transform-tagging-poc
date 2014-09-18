package com.transform.poc;

import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

/**
 * Created by synhershko on 9/18/14.
 */
public class TitlePercolator extends Percolator {

    public void registerTag(String tagName, Iterable<String> keywords) throws IOException {
        //This is the query we're registering in the percolator
        final BoolQueryBuilder qb = boolQuery();
        for (String keyword : keywords) {
            qb.should(matchQuery("title", keyword));
        }
    }

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
            ret.add(new TagMatch(match.getId().string(), getTaggerScore()));
        }
        return ret;
    }
}

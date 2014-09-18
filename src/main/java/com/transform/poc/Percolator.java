package com.transform.poc;

import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.node.Node;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.Closeable;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Created by synhershko on 9/17/14.
 */
public class Percolator implements Closeable {
    private Node node;
    private Client client;

    public Percolator(){
        String hostname = "percolator";
        try {
            hostname = java.net.InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
        }

        final long start = System.currentTimeMillis();
        final String percolatorId = hostname + "_" + new DateTime(start).toString(DateTimeFormat.forPattern("yyyyMMddHHmm"));
        node = nodeBuilder().clusterName("tagging")
                .settings(ImmutableSettings.settingsBuilder()
                        .put("node.name", percolatorId)
                        .put("node.tag", "percolator")
                        .put("http.enabled", false)
                        .put("transport.tcp.port", "9350-9400")
                        .build())
                .local(true)
                .node();
        client = node.client();

        client.admin().indices().preparePutTemplate("tagging-template")
                .setSource("{\n" +
                        "    \"template\" : \"tagging-*\",\n" +
                        "    \"order\" : 0,\n" +
                        "    \"settings\" : {\n" +
                        "        \"number_of_shards\" : 1,\n" +
                        "        \"number_of_replicas\" : 0\n" +
                        "    },\n" +
                        "    \"mappings\" : {\n" +
                        "        \"nutch-document\" : {\n" +
                        "            \"_all\" : { \"enabled\" : false },\n" +
                        "            \"properties\": {\n" +
                        "                \"title\": {\n" +
                        "                  \"type\": \"string\"\n" +
                        "                },\n" +
                        "                \"content\": {\n" +
                        "                  \"type\": \"string\"\n" +
                        "                }\n" +
                        "             }\n" +
                        "         }\n" +
                        "    }\n" +
                        "}")
                .execute().actionGet();

        // register index mapping to support helpful full-text searches
        if (!client.admin().indices().prepareExists("tagging-index").execute().actionGet().isExists()) {
            client.admin().indices().prepareCreate("tagging-index").execute().actionGet();
        }
    }

    public void clear(){
        client.admin().indices().prepareDelete("tagging-index").execute().actionGet();
        client.admin().indices().prepareCreate("tagging-index").execute().actionGet();
    }

    // Percolation is explained here http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-percolate.html
    public void registerQuery(String tagName, Iterable<String> keywords) throws IOException {
        //This is the query we're registering in the percolator
        final BoolQueryBuilder qb = boolQuery();
        for (String keyword : keywords) {
            qb.should(matchQuery("content", keyword));
        }

        //Index the query = register it in the percolator
        client.prepareIndex("tagging-index", ".percolator", tagName)
                .setSource(jsonBuilder()
                        .startObject()
                        .field("query", qb) // Register the query
                        .endObject())
                .execute().actionGet();
    }

    public void flush(){
        client.admin().indices().prepareFlush("tagging-index").execute().actionGet();
        client.admin().indices().prepareRefresh("tagging-index").execute().actionGet();
    }

    public Collection<TagMatch> getTags(Document doc) throws IOException {
        //Build a document to check against the percolator
        XContentBuilder docBuilder = XContentFactory.jsonBuilder().startObject();
        docBuilder.field("doc").startObject(); //This is needed to designate the document
        docBuilder.field("title", doc.getTitle());
        docBuilder.field("content", doc.getContent());
        docBuilder.endObject(); //End of the doc field
        docBuilder.endObject(); //End of the JSON root object

        // Percolate
        PercolateResponse response = client.preparePercolate()
                .setIndices("tagging-index")
                .setDocumentType("nutch-document")
//                .setScore(true).setSortByScore(true)
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


    @Override
    public void close() throws IOException {
        client.close();
        node.close();
    }
}
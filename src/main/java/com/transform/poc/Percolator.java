package com.transform.poc;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.node.Node;

import java.io.IOException;
import java.net.UnknownHostException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Created by synhershko on 9/17/14.
 */
public abstract class Percolator implements Tagger {
    private Node node;
    protected Client client;
    private float taggerScore = 1.0f;

    protected Percolator(){
        String hostname = "percolator";
        try {
            hostname = java.net.InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
        }

        final long start = System.currentTimeMillis();
        final String percolatorId = hostname; //+ "_" + new DateTime(start).toString(DateTimeFormat.forPattern("yyyyMMddHHmm"));
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
                        "        \"number_of_replicas\" : 0,\n" +
                        "        \"analysis\": {\n" +
                        "           \"filter\":{\n"+
                        "               \"english_stemmer\": {\n" +
                        "               \"type\":       \"stemmer\",\n" +
                        "               \"language\":   \"english\"\n" +
                        "        },\n" +
                        "        \"english_possessive_stemmer\": {\n" +
                        "          \"type\":       \"stemmer\",\n" +
                        "          \"language\":   \"possessive_english\"\n" +
                        "        }},\n" +
                                "            \"analyzer\": {\n" +
                                "                \"english-html\": {\n" +
                        "                            \"char_filter\":[\"html_strip\"],\n" +
                                "                    \"tokenizer\":      \"standard\",\n" +
                                "                    \"filter\": [\"english_possessive_stemmer\",\"lowercase\",\"english_stemmer\"]\n" +
                                "                }\n" +
                                "            }\n" +
                                "        }\n" +
                        "    },\n" +
                        "    \"mappings\" : {\n" +
                        "        \"nutch-document\" : {\n" +
                        "            \"_all\" : { \"enabled\" : false },\n" +
                        "            \"properties\": {\n" +
                        "                \"title\": {\n" +
                        "                  \"type\": \"string\", \"analyzer\":\"english-html\"\n" +
                        "                },\n" +
                        "                \"content\": {\n" +
                        "                  \"type\": \"string\", \"position_offset_gap\": 10000, \"analyzer\":\"english-html\"\n" +
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
    protected void addPercolationQuery(String name, QueryBuilder query) throws IOException {
        //Index the query = register it in the percolator
        client.prepareIndex("tagging-index", ".percolator", name)
                .setSource(jsonBuilder()
                        .startObject()
                        .field("query", query) // Register the query
                        .endObject())
                .execute().actionGet();
    }

    public void flush(){
        client.admin().indices().prepareFlush("tagging-index").execute().actionGet();
        client.admin().indices().prepareRefresh("tagging-index").execute().actionGet();
    }

    @Override
    public void close() throws IOException {
        client.close();
        node.close();
    }

    public float getTaggerScore() {
        return taggerScore;
    }

    public void setTaggerScore(float taggerScore) {
        this.taggerScore = taggerScore;
    }
}
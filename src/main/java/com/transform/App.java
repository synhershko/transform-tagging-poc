package com.transform;

import com.transform.poc.*;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.*;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException {
        List<TagMapping> _tags = new ArrayList<TagMapping>();
        _tags.add(new TagMapping("export", Arrays.asList("elling overseas", "foreign trade")));
        _tags.add(new TagMapping("export-market-intelligence", Arrays.asList("competition", "competitors", "research")));
        _tags.add(new TagMapping("export-develop", Arrays.asList("overseas", "UKTI", "partner", "consultant")));
        _tags.add(new TagMapping("export-distribute", Arrays.asList("customs", "supplier", "logistics", "transport")));
        _tags.add(new TagMapping("export-show", Arrays.asList("exhibition", "advertising", "promotion")));
        _tags.add(new TagMapping("export-culture", Arrays.asList("language", "custom", "polite", "manners")));
        _tags.add(new TagMapping("export-grow", Arrays.asList("grow", "overseas", "customers", "expansion", "supply")));

        Percolator percolator = new ContentPercolator();
        for (TagMapping tag : _tags) {
            percolator.registerTag(tag.getTagName(), tag.getKeywords());
        }
        percolator.flush();

        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("Enter a URL or q to quit: ");

            if (!in.hasNext()) break;

            String s = in.next();
            if (s == null || "q".equals(s)) {
                break;
            }

            if ("clean".equals(s)) {
                percolator.clear();
                System.out.println("Percolator re-initialized");
            } else {
                try {
                    Document doc = downloadDocument(s);
                    Collection<TagMatch> tags = percolator.getTags(doc);
                    if (tags.size() == 0) {
                        System.out.println("\t No tags found!");
                    } else {
                        for (final TagMatch tag : tags) {
                            System.out.println("\t" + tag);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            percolator.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Document downloadDocument(String url) throws IOException {
        System.out.println("Retrieving " + url);
        org.jsoup.nodes.Document document = Jsoup.connect(url).get();
        System.out.println("\t got webpage " + document.title());
        return new Document(document.title(), document.body().text());
    }
}

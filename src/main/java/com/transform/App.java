package com.transform;

import com.transform.poc.ContentPercolator;
import com.transform.poc.Document;
import com.transform.poc.Percolator;
import com.transform.poc.TagMatch;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException {
        Percolator percolator = new ContentPercolator();

        percolator.registerQuery("export", Arrays.asList("elling overseas", "foreign trade"));
        percolator.registerQuery("export-market-intelligence", Arrays.asList("competition", "competitors", "research"));
        percolator.registerQuery("export-develop", Arrays.asList("overseas", "UKTI", "partner", "consultant"));
        percolator.registerQuery("export-distribute", Arrays.asList("customs", "supplier", "logistics", "transport"));
        percolator.registerQuery("export-show", Arrays.asList("exhibition", "advertising", "promotion"));
        percolator.registerQuery("export-culture", Arrays.asList("language", "custom", "polite", "manners"));
        percolator.registerQuery("export-grow", Arrays.asList("grow", "overseas", "customers", "expansion", "supply"));

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

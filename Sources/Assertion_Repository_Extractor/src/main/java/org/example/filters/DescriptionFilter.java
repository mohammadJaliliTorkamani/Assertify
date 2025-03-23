package org.example.filters;

import org.example.Filter;
import org.example.RepositoriesInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Set;
import java.util.stream.Collectors;

import static org.example.Utils.hasAnyKeywords;

public class DescriptionFilter implements Filter {
    private final String[] prohibitedKeywords;

    public DescriptionFilter(String[] prohibitedKeywords) {
        this.prohibitedKeywords = prohibitedKeywords;
    }

    @Override
    public Set<String> apply(Set<String> urls) {
        return urls.stream().filter(item -> {
            try {
                Document document = Jsoup.connect(item).get();
                String description = document.body().getElementsByClass("f4 my-3").get(0).text();
                RepositoriesInfo.getInstance().putDescriptionsKeywords(item, !hasAnyKeywords(description, prohibitedKeywords));
                return !hasAnyKeywords(description, prohibitedKeywords);
            } catch (Exception e) {
                return false;
            }
        }).collect(Collectors.toSet());
    }
}

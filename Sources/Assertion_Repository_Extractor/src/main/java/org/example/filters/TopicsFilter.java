package org.example.filters;

import org.example.Filter;
import org.example.RepositoriesInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.Utils.hasAnyKeywords;

public class TopicsFilter implements Filter {
    private final String[] prohibitedKeywords;

    public TopicsFilter(String[] prohibitedKeywords) {
        this.prohibitedKeywords = prohibitedKeywords;
    }

    @Override
    public Set<String> apply(Set<String> urls) {
        return urls.stream().filter(item -> {
            try {
                Document document = Jsoup.connect(item).get();
                List<Element> elements = document.body().getElementsByClass("topic-tag");
                if (elements.size() > 0) {
                    Set<String> topics = elements.stream().map(Element::text).collect(Collectors.toSet());
                    RepositoriesInfo.getInstance().putTopicsKeywords(item, topics.stream().noneMatch(topic -> hasAnyKeywords(topic, prohibitedKeywords)));
                    return topics.stream().noneMatch(topic -> hasAnyKeywords(topic, prohibitedKeywords));
                }
                RepositoriesInfo.getInstance().putTopicsKeywords(item, false);
                return false;
            } catch (Exception e) {
                return false;
//                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }
}

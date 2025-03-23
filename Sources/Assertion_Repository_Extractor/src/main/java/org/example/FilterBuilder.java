package org.example;

import org.example.filters.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilterBuilder {
    private final Set<String> urls;
    private final List<Filter> filters = new ArrayList<>();

    public FilterBuilder(Set<String> urls) {
        this.urls = urls;
    }

    /**
     * applies the filters sequentially and returns the candidate repositories
     *
     * @return
     */
    public Set<String> getURLs() {
        Set<String> filtered = new HashSet<>(urls);
        for (Filter filter : filters) {
            System.out.printf("Applying %s (size: %d) ...%n", filter.getClass().getSimpleName(), filtered.size());
            filtered = filter.apply(filtered);
        }
        return filtered;
    }

    public FilterBuilder filterLanguages(String[] languages) {
        filters.add(new LanguageFilter(languages));
        return this;
    }

    public FilterBuilder filterExistence() {
        filters.add(new ExistenceFilter());
        return this;
    }

    public FilterBuilder filterDeprecated() {
        filters.add(new NotDeprecatedFilter());
        return this;
    }

    public FilterBuilder filterAge(int minimumAgeInYear) {
        filters.add(new AgeFilter(minimumAgeInYear));
        return this;
    }

    public FilterBuilder filterCommits(int minimumCommitsAgeInDays) {
        filters.add(new CommitsFilter(minimumCommitsAgeInDays));
        return this;
    }

    public FilterBuilder filterIssues(int minimumIssuesAgeInDays) {
        filters.add(new IssuesFilter(minimumIssuesAgeInDays));
        return this;
    }

    public FilterBuilder filterPullRequests(int minimumPR_AgeInDays) {
        filters.add(new PullRequestFilter(minimumPR_AgeInDays));
        return this;
    }

    public FilterBuilder filterContributors(int minimumContributors) {
        filters.add(new ContributorsFilter(minimumContributors));
        return this;
    }

    public FilterBuilder filterForks(int minimumForks) {
        filters.add(new ForksFilter(minimumForks));
        return this;
    }

    public FilterBuilder filterReadMeContents(String[] prohibitedKeywords) {
        filters.add(new ReadMeContentFilter(prohibitedKeywords));
        return this;
    }

    public FilterBuilder filterDescription(String[] prohibitedKeywords) {
        filters.add(new DescriptionFilter(prohibitedKeywords));
        return this;
    }

    public FilterBuilder filterTopics(String[] prohibitedKeywords) {
        filters.add(new TopicsFilter(prohibitedKeywords));
        return this;
    }

    public FilterBuilder filterStars(int minimumStars) {
        filters.add(new NumberOfStarsFilter(minimumStars));
        return this;
    }
}

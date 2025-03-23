package org.example;

import java.util.HashMap;
import java.util.Map;

public class RepositoriesInfo {
    private static RepositoriesInfo instance;
    private int numberOfStars;
    private int numberOfContributors;
    private boolean notDeprecated;
    private int age; //in years;
    private int numberOfCommits; //in last 2 years;
    private int numberOfIssues; // in last 2 years;
    private int numberOfPR; // in last 2 years;
    private boolean readmeKeywords;
    private boolean topicsKeywords;
    private boolean descriptionsKeywords;
    private final Map<String, RepositoriesInfo> map = new HashMap<>();

    private RepositoriesInfo() {
    }

    public static RepositoriesInfo getInstance() {
        if (instance == null)
            instance = new RepositoriesInfo();
        return instance;
    }

    public void setNumberOfStars(int numberOfStars) {
        this.numberOfStars = numberOfStars;
    }

    public void setNumberOfContributors(int numberOfContributors) {
        this.numberOfContributors = numberOfContributors;
    }

    public void setNotDeprecated(boolean notDeprecated) {
        this.notDeprecated = notDeprecated;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setNumberOfCommits(int numberOfCommits) {
        this.numberOfCommits = numberOfCommits;
    }

    public void setNumberOfIssues(int numberOfIssues) {
        this.numberOfIssues = numberOfIssues;
    }

    public void setNumberOfPR(int numberOfPR) {
        this.numberOfPR = numberOfPR;
    }

    public void setReadmeKeywords(boolean readmeKeywords) {
        this.readmeKeywords = readmeKeywords;
    }

    public void setTopicsKeywords(boolean topicsKeywords) {
        this.topicsKeywords = topicsKeywords;
    }

    public void setDescriptionsKeywords(boolean descriptionsKeywords) {
        this.descriptionsKeywords = descriptionsKeywords;
    }

    //

    public void putNumberOfStars(String url, int numberOfStars) {
        if (map.containsKey(url))
            map.get(url).setNumberOfStars(numberOfStars);
        else {
            RepositoriesInfo repositoriesInfo = new RepositoriesInfo();
            repositoriesInfo.setNumberOfStars(numberOfStars);
            map.put(url, repositoriesInfo);
        }
    }

    public void putNumberOfContributors(String url, int numberOfContributors) {
        if (map.containsKey(url))
            map.get(url).setNumberOfContributors(numberOfContributors);
        else {
            RepositoriesInfo repositoriesInfo = new RepositoriesInfo();
            repositoriesInfo.setNumberOfContributors(numberOfContributors);
            map.put(url, repositoriesInfo);
        }
    }

    public void putNotDeprecated(String url, boolean notDeprecated) {
        if (map.containsKey(url))
            map.get(url).setNotDeprecated(notDeprecated);
        else {
            RepositoriesInfo repositoriesInfo = new RepositoriesInfo();
            repositoriesInfo.setNotDeprecated(notDeprecated);
            map.put(url, repositoriesInfo);
        }
    }

    public void putAge(String url, int age) {
        if (map.containsKey(url))
            map.get(url).setAge(age);
        else {
            RepositoriesInfo repositoriesInfo = new RepositoriesInfo();
            repositoriesInfo.setAge(age);
            map.put(url, repositoriesInfo);
        }
    }

    public void putNumberOfCommits(String url, int numberOfCommits) {
        if (map.containsKey(url))
            map.get(url).setNumberOfCommits(numberOfCommits);
        else {
            RepositoriesInfo repositoriesInfo = new RepositoriesInfo();
            repositoriesInfo.setNumberOfCommits(numberOfCommits);
            map.put(url, repositoriesInfo);
        }
    }

    public void putNumberOfIssues(String url, int numberOfIssues) {
        if (map.containsKey(url))
            map.get(url).setNumberOfIssues(numberOfIssues);
        else {
            RepositoriesInfo repositoriesInfo = new RepositoriesInfo();
            repositoriesInfo.setNumberOfIssues(numberOfIssues);
            map.put(url, repositoriesInfo);
        }
    }

    public void putNumberOfPR(String url, int numberOfPR) {
        if (map.containsKey(url))
            map.get(url).setNumberOfPR(numberOfPR);
        else {
            RepositoriesInfo repositoriesInfo = new RepositoriesInfo();
            repositoriesInfo.setNumberOfPR(numberOfPR);
            map.put(url, repositoriesInfo);
        }
    }

    public void putReadmeKeywords(String url, boolean readmeKeywords) {
        if (map.containsKey(url))
            map.get(url).setReadmeKeywords(readmeKeywords);
        else {
            RepositoriesInfo repositoriesInfo = new RepositoriesInfo();
            repositoriesInfo.setReadmeKeywords(readmeKeywords);
            map.put(url, repositoriesInfo);
        }
    }

    public void putTopicsKeywords(String url, boolean topicsKeywords) {
        if (map.containsKey(url))
            map.get(url).setTopicsKeywords(topicsKeywords);
        else {
            RepositoriesInfo repositoriesInfo = new RepositoriesInfo();
            repositoriesInfo.setTopicsKeywords(topicsKeywords);
            map.put(url, repositoriesInfo);
        }
    }

    public void putDescriptionsKeywords(String url, boolean descriptionsKeywords) {
        if (map.containsKey(url))
            map.get(url).setDescriptionsKeywords(descriptionsKeywords);
        else {
            RepositoriesInfo repositoriesInfo = new RepositoriesInfo();
            repositoriesInfo.setDescriptionsKeywords(descriptionsKeywords);
            map.put(url, repositoriesInfo);
        }
    }

    public Map<String, RepositoriesInfo> getMap() {
        return map;
    }
}

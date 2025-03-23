package org.example;

import org.eclipse.jgit.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * a Java program to scrape expected repositories from GitHub
 */
public class Main {

    /**
     * whether scrape repos from grep.app website
     */
    private static final boolean USE_GREP_APP = false;

    /**
     * whether to use sourcegraph.com exported csv file.
     */
    private static final boolean USE_SOURCE_GRAPH_CSV = true;
    /**
     * number of repositories to be cloned from the GitHub
     */
    private static final int NUMBER_OF_REPOSITORIES = 4000;
    /**
     * the path of the assertions to be explored from the repositories
     */
    private static final String GREP_APP_REPO_SOURCE_PATH = "src/main/java/";
    /**
     * the keyword to be searched from REPO_SOURCE_PATH repositories. (could be a regex)
     */
    private static final String[] KEYWORDS = new String[]{"^[^(\\/\\/)\\*]\\s*assert\\s+\\w*"};
//            , "^[^(\\/\\/)\\*]\\s*Assert\\..+\\(.+", "^[^(\\/\\/)\\*]\\s*assert\\w+\\(.*"};//these two regex were commented as we only need java standard assertions yet
    /**
     * whether to search the exact keyword or not.
     */
    private static final boolean GREP_APP_CASE_SENSITIVE_SEARCH = true;
    /**
     * whether to search the whole keyword as word or not. (if regex is disabled).
     */
    private static final boolean GREP_APP_WHOLE_WORDS_SEARCH = false;
    /**
     * whether to consider the keyword as a regex or not.
     */
    private static final boolean GREP_APP_REGULAR_EXPRESSION = true;
    /**
     * minimum number of years from the repository's creation time until now
     */
    private static final int MINIMUM_AGE_IN_YEAR = 3;
    /**
     * Language(s) of the candidate repositories
     */
    private static final String[] LANGUAGES = new String[]{"Java"};
    /**
     * the time interval (in days) to check if any commits has been pushed into the repository
     */
    private static final int MINIMUM_COMMITS_AGE_IN_DAYS = 24 * 30;
    /**
     * the time interval (in days) to check if any issues has been created for the repository
     */
    private static final int MINIMUM_ISSUES_AGE_IN_DAYS = 24 * 30;
    /**
     * the time interval (in days) to check if any Pull Requests has been created for the repository
     */
    private static final int MINIMUM_PR_AGE_IN_DAYS = 24 * 30;
    /**
     * minimum number of contributors
     */
    private static final int MINIMUM_CONTRIBUTORS = 3;
    /**
     * minimum number of forks
     */
    private static final int MINIMUM_FORKS = 50;
    /**
     * keywords which should not exist in a repository
     */
    private static final String[] PROHIBITED_KEYWORDS = new String[]{"hello-world", "solution"};
    /**
     * name of the file to be saved after fetching the urls. this file can also be used for fetching offline
     * NOTE: better to be added in .gitignore
     */
    private static final String SERIALIZED_TOTAL_URLS_FILE_NAME = "total_urls.ser";
    /**
     * name of the file to be saved after filtering the urls. this file can also be used for fetching offline
     * NOTE: better to be added in .gitignore
     */
    private static final String SERIALIZED_FILTERED_URLS_FILE_NAME = "filtered_urls.ser";
    private static final int MINIMUM_NUMBER_OF_STARS = 1000;


    public static List<String> listEmptyFolders(String directoryPath) {
        List<String> emptyFolders = new ArrayList<>();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && isEmptyDirectory(file)) {
                        emptyFolders.add(file.getAbsolutePath());
                    }
                }
            }
        } else {
            System.out.println("The provided path is not a valid directory.");
        }

        return emptyFolders;
    }

    private static boolean isEmptyDirectory(File directory) {
        File[] files = directory.listFiles();
        return files != null && files.length == 0;
    }

    public static void main(String[] args) {
        assert !(GREP_APP_WHOLE_WORDS_SEARCH && GREP_APP_REGULAR_EXPRESSION) : "Regex and WholeWord options can not be both true";
        assert USE_SOURCE_GRAPH_CSV || USE_GREP_APP : "You have to enable at least one data source";

        final Map<String, Set<String>> unfilteredRepositoriesMap = new HashMap<>();
        if (USE_GREP_APP) {
            try (org.example.SeleniumConfig config = SeleniumConfig.getInstance()) {
                SearchURL searchURL = new SearchURL(GREP_APP_CASE_SENSITIVE_SEARCH, GREP_APP_REGULAR_EXPRESSION, GREP_APP_WHOLE_WORDS_SEARCH,
                        LANGUAGES, GREP_APP_REPO_SOURCE_PATH, null);

                if (!Utils.existsURLs(SERIALIZED_TOTAL_URLS_FILE_NAME)) {
                    System.out.printf("%d keywords detected...%n", KEYWORDS.length);
                    Arrays.stream(KEYWORDS).forEach(keyword -> {
                        System.out.println("Trying keyword: " + keyword);
                        searchURL.setKeywordQuery(keyword.replace("+", "%2B"));
                        Scrapper.getInstance().scrapeURLs(config.getDriver(), searchURL, NUMBER_OF_REPOSITORIES).forEach((url, items) -> {
                            if (unfilteredRepositoriesMap.containsKey(url))
                                unfilteredRepositoriesMap.get(url).addAll(items);
                            else
                                unfilteredRepositoriesMap.put(url, new HashSet<>(items));
                        });
                    });
                } else {
                    System.out.println("Local records exists for fetched URLs...");
                    unfilteredRepositoriesMap.putAll(Utils.readURLs(SERIALIZED_TOTAL_URLS_FILE_NAME));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.printf("%d Grep.App  URLS found%n", unfilteredRepositoriesMap.size());
        }

        if (USE_SOURCE_GRAPH_CSV) {
            Utils.readURLsFromSourceGraphCSV(unfilteredRepositoriesMap, NUMBER_OF_REPOSITORIES);
        }
        Utils.writeURLs(unfilteredRepositoriesMap, SERIALIZED_TOTAL_URLS_FILE_NAME);
        System.out.printf("%d Total unfiltered URLSs found from %s%n", unfilteredRepositoriesMap.size(), (USE_GREP_APP ? "GREP.APP" : (USE_SOURCE_GRAPH_CSV ? "SourceGraph" : "Empty")));

        Set<String> filteredRepositoriesURLs;
        if (Utils.existsRepoURLs(SERIALIZED_FILTERED_URLS_FILE_NAME)) {
            System.out.printf("%nLocal records exists for filtered URLs...%n");
            filteredRepositoriesURLs = Utils.readRepoURLs(SERIALIZED_FILTERED_URLS_FILE_NAME);
            System.out.printf("%n%d filtered URLs found%n", filteredRepositoriesURLs.size());
        } else {
            filteredRepositoriesURLs = new FilterBuilder(unfilteredRepositoriesMap.keySet())
//                        .filterLanguages(LANGUAGES)
//                        .filterExistence()
                    .filterStars(MINIMUM_NUMBER_OF_STARS)
//                        .filterDeprecated()
//                    .filterAge(MINIMUM_AGE_IN_YEAR)
//                        .filterCommits(MINIMUM_COMMITS_AGE_IN_DAYS)
//                        .filterIssues(MINIMUM_ISSUES_AGE_IN_DAYS)
//                        .filterPullRequests(MINIMUM_PR_AGE_IN_DAYS)
//                    .filterContributors(MINIMUM_CONTRIBUTORS)
                    //.filterForks(MINIMUM_FORKS)
//                        .filterReadMeContents(PROHIBITED_KEYWORDS)
//                        .filterDescription(PROHIBITED_KEYWORDS)
//                        .filterTopics(PROHIBITED_KEYWORDS)
                    .getURLs();

//            Utils.writeStatistics(RepositoriesInfo.getInstance().getMap());

            System.out.println("After filtering, #URLS: "+filteredRepositoriesURLs.size());
            Utils.writeRepoURLs(filteredRepositoriesURLs, SERIALIZED_FILTERED_URLS_FILE_NAME);
        }

        Map<String, Set<String>> filteredRepositoriesMap = new HashMap<>();
        unfilteredRepositoriesMap.forEach((url, urls) -> {
            if (filteredRepositoriesURLs.contains(url))
                filteredRepositoriesMap.put(url, urls);
        });
        Scrapper.getInstance().cloneRepositories(filteredRepositoriesURLs, value -> System.out.printf("%nCloning %s", value));
        Scrapper.getInstance().writeAssertionPathInFile(filteredRepositoriesMap);
        System.out.printf("%n%n%d of %d repos survived for clone from GitHub%n", filteredRepositoriesMap.size(), unfilteredRepositoriesMap.size());
    }
}
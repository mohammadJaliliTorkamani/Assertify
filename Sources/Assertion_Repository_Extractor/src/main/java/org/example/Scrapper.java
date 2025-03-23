package org.example;

import org.eclipse.jgit.api.Git;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/***
 * Scrapper singleton class used for scrapping the GitHub repositories by the given SearchURL
 */
public class Scrapper {

    /**
     * the split character which is used to separate the creator and repo name of the directories inside repositories
     */
    private static final char SPLIT_CHAR = '@';
    /**
     * the name of the cloned-repositories container
     */
    //EXISTS IN '.gitignore' FILE AS WELL
    private static final String REPOSITORIES_DIR_PATH = "./repositories";
    /**
     * this is the output name of the file which contains the URLs of the files containing the keyword
     */
    //THE FILE CONTAINS FOUND THE FILES HAVING PASSED KEYWORD | MOST OF THE TIME MORE RECORDS THAN THE PASSED
    // REPOSITORY SIZE (BECAUSE OF THE REDUNDANT REPOS)
    private static final String OUTPUT_FILE_NAME = "output.txt";
    private static Scrapper instance;

    private Scrapper() {
    }

    /**
     * getInstance method of the singleton pattern
     *
     * @return the singleton object
     */
    public static Scrapper getInstance() {
        if (instance == null)
            instance = new Scrapper();
        return instance;
    }

    /**
     * scrapes the repositories in the given search URL having a particular keyword, language and path
     *
     * @param driver    selenium driver to be used as the scrapper
     * @param searchURL the configuration of the repositories to be search from
     * @param size      number of repositories to be cloned, having the searchURL configuration
     * @return the URLs of the repositories and the files having the keyword
     */
    public Map<String, Set<String>> scrapeURLs(WebDriver driver, org.example.SearchURL searchURL, int size) {
        ///
        int counter = 0;
        System.out.println(searchURL.toString());
        driver.get(searchURL.toString());
        WebDriverWait pathWait = new WebDriverWait(driver, Duration.ofSeconds(600));
        WebElement pathElement = pathWait.until(
                ExpectedConditions.presenceOfElementLocated(By.className("sui-layout-sidebar"))
        );

        List<WebElement> pathsElements = pathElement.findElement(By.tagName("div")).findElements(By.xpath("./*")).get(1)
                .findElement(By.className("sui-multi-checkbox-facet")).findElements(By.xpath("./*"));
        Map<String, Set<String>> map = new HashMap<>();

        List<String> paths = pathsElements.stream().map(path -> path.findElement(By.tagName("div"))
                .findElement(By.tagName("span")).getAttribute("innerHTML")).collect(Collectors.toList());
        for (String path : paths) {
            System.out.println("Trying sub-path: " + path);
            int _length;
            searchURL.setSubDirToCodePath(path);
            searchURL.resetPageNumber();
            driver.get(searchURL.toString());
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(600));
            int maxPage = getMaxPage(wait);

            do {
                System.out.printf("%nFetching (%d of %d | total received: %d | page %d of %d) from %s",
                        Math.min((size - counter),
                                SearchURL.PAGE_LIMIT),
                        SearchURL.PAGE_LIMIT,
                        counter,
                        searchURL.getPageNumber(),
                        maxPage,
                        searchURL);
                driver.get(searchURL.toString());
                WebElement element = wait.until(
                        ExpectedConditions.presenceOfElementLocated(By.className("sui-results-container"))
                );
                List<WebElement> children = element.findElements(By.xpath("./*"));
                _length = children.size();
                for (WebElement webElement : children) {
                    if (counter < size) {
                        WebElement _element = webElement
                                .findElement(By.className("sui-result__header"))
                                .findElement(By.className("sui-result__title"));
                        String fileURL = _element
                                .findElement(By.className("result-file-info"))
                                .findElement(By.className("result-file"))
                                .findElement(By.tagName("a"))
                                .getAttribute("href");

                        String repoURL = _element
                                .findElement(By.className("result-repo"))
                                .findElement(By.tagName("a"))
                                .getAttribute("href");

                        if (!map.containsKey(repoURL)) {
                            Set<String> set = new HashSet<>();
                            set.add(fileURL);
                            map.put(repoURL, set);
                            counter++;
                        } else
                            map.get(repoURL).add(fileURL);
                    }
                }

                if (counter < size && _length == SearchURL.PAGE_LIMIT)
                    searchURL.increasePageNumber();
            } while (counter != size && _length > 0 && searchURL.getPageNumber() + 1 < maxPage);
        }
        //

        System.out.printf("%n%d URLs fetched successfully from GitHub%n", counter);
        return map;
    }

    /**
     * returns the last page of the document
     *
     * @param wait webdriver wait to scrape with.
     * @return the last page of the document
     */
    private int getMaxPage(WebDriverWait wait) {
        List<WebElement> pages = wait.until(
                        ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ant-pagination.mini")))
                .findElements(By.tagName("li"));
        List<WebElement> elements = pages.stream().filter(webElement -> webElement.getAttribute("class")
                .contains("ant-pagination-item ant-pagination-item-")).collect(Collectors.toList());
        return Integer.parseInt(elements.get(elements.size() - 1).getAttribute("title"));
    }

    /**
     * clones and stores repositories of URLs in the repositories directory
     *
     * @param repositories       repositories to be cloned
     * @param onDownloadCallback the callback to be invoked after each clone
     */
    public void cloneRepositories(Set<String> repositories, CallBack<String> onDownloadCallback) {
        System.out.println("Cloning...");
        try {
            createRepositoryDirectoryIfNotExists();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Started...");
        repositories.forEach(key -> {
            String[] splitKey = key.split("/");
            String creator = splitKey[splitKey.length - 2];
            String repoName = splitKey[splitKey.length - 1];
            File repositoryDir = new File(
                    REPOSITORIES_DIR_PATH + File.separator + creator + SPLIT_CHAR + repoName
            );

            if (!repositoryDir.exists()) {
                try {
                    Files.createDirectory(
                            Paths.get(REPOSITORIES_DIR_PATH + File.separator + creator + SPLIT_CHAR + repoName)
                    );
                    onDownloadCallback.onSuccess(key);
                    Git.cloneRepository().setURI("https://" + key + ".git").setDirectory(repositoryDir).call().close();
                } catch (Exception e) {
                    System.out.println("Corrputed URL:  " + "https://" + key + ".git     |     "+repositoryDir);
                    e.printStackTrace();
                }
            } else
                System.out.printf("%nRepository already exists (%s)", key);
        });
    }

    /**
     * creates an empty directory in REPOSITORIES_DIR_PATH, if it does not exist
     *
     * @throws IOException if there is any problem while creating directory
     */
    private void createRepositoryDirectoryIfNotExists() throws IOException {
        File destDir = new File(REPOSITORIES_DIR_PATH);
        if (!destDir.exists())
            Files.createDirectory(Paths.get(REPOSITORIES_DIR_PATH));
    }

    /**
     * populates output.txt file with the URLs of the files containing the keyword
     *
     * @param URLs URLs of the repositories
     */
    public void writeAssertionPathInFile(Map<String, Set<String>> URLs) {
        try {
            createRepositoryDirectoryIfNotExists();
            FileWriter writer = new FileWriter(REPOSITORIES_DIR_PATH + File.separator + OUTPUT_FILE_NAME);
            URLs.forEach((key, value) -> {
                value.forEach(item -> {

                    try {
                        writer.write(item + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

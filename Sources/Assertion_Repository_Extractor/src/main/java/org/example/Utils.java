package org.example;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    /***
     * retrieves the repository owner from the repo url
     * @param repoURL the url of repository
     * @return the owner name
     */
    public static String getRepoOwner(String repoURL) {
        String[] arr = repoURL.trim().split("/");
        return arr[arr.length - 2];
    }

    /***
     * retrieves the repository name from the repo url
     * @param repoURL the url of repository
     * @return the repository name
     */
    public static String getRepoName(String repoURL) {
        String[] arr = repoURL.trim().split("/");
        return arr[arr.length - 1];
    }

    /**
     * checks if there is any keyword in a given text
     *
     * @param text     content to be analyzed
     * @param keywords keywords to be checked with
     * @return boolean of existence
     */
    public static boolean hasAnyKeywords(String text, String[] keywords) {
        return Arrays.stream(keywords).anyMatch(keyword -> text.toLowerCase().contains(keyword.toLowerCase()));
    }

    /**
     * returns a dateTime in "yyyy-MM-dd'T'HH:mm:ss'Z'" for the last @daysBefore days
     *
     * @param daysBefore the offset from today
     * @return formatted datetime string
     */
    public static String getLastN_DaysDateTime(int daysBefore) {
        LocalDateTime dateTime = LocalDateTime.now().minusDays(daysBefore);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        return dateTime.format(formatter);
    }

    /**
     * decodes an encoded base64 string
     *
     * @param encodedStr encoded text
     * @return decoded text
     */
    public static String decodeBase64(String encodedStr) {
        byte[] asBytes = Base64.getDecoder().decode(encodedStr);
        return new String(asBytes, StandardCharsets.UTF_8);
    }

    /**
     * reads the content of output.txt file as a set of lines
     *
     * @param repositoriesDirPath local base url of the output.txt file
     * @return set of lines
     * @throws IOException if file couldn't be opened
     */
    public static Set<String> readJavaFile(String repositoriesDirPath) throws IOException {
        String filePath = repositoriesDirPath + "output.txt";
        Set<String> set = new HashSet();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            while (line != null) {
                set.add(line);
                line = reader.readLine();
            }
        }

        return set;
    }

    /**
     * reads repo urls from a file
     *
     * @return the read repo urls
     */
    public static Set<String> readRepoURLs(String SERIALIZED_FILTERED_URLS_FILE_NAME) {
        Set<String> repoURLs = null;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SERIALIZED_FILTERED_URLS_FILE_NAME))) {
            repoURLs = (Set<String>) ois.readObject(); // Read the map from the file
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return repoURLs;
    }

    /**
     * reads urls from a file
     *
     * @return the read urls
     */
    public static Map<String, Set<String>> readURLs(String SERIALIZED_TOTAL_URLS_FILE_NAME) {
        Map<String, Set<String>> urls = null;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SERIALIZED_TOTAL_URLS_FILE_NAME))) {
            urls = (Map<String, Set<String>>) ois.readObject(); // Read the map from the file
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return urls;
    }

    /**
     * generates the absolute java pat from the java file web url
     *
     * @param url                  web url to process
     * @param repositoriesRootPath basePath of the repositories
     * @return absolute local java path of the web url
     */
    public static String extractJavaAbsolutePath(String url, String repositoriesRootPath) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        url = url.trim();
        Pattern pattern = Pattern.compile("github\\.com\\/([^\\/]+)\\/([^\\/]+)");
        String ownerName = null;
        String repoName = null;
        System.out.println("URL : "+url);
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            ownerName = matcher.group(1);
            repoName = matcher.group(2);
        }

        String[] parts = url.split("/");
        if (parts.length < 4) {
            return null;
        }

        return  repositoriesRootPath + ownerName + "@" + repoName + "/" +
                String.join("/", Arrays.copyOfRange(parts, Arrays.asList(parts).indexOf("master") + 1, parts.length));
    }

    /**
     * checks the existence of URLs file
     *
     * @return existence of urls (boolean)
     */
    public static boolean existsURLs(String SERIALIZED_TOTAL_URLS_FILE_NAME) {
        File file = new File(SERIALIZED_TOTAL_URLS_FILE_NAME);
        return file.exists();
    }

    /**
     * checks the existence of repo URLs file
     *
     * @return existence of repo urls (boolean)
     */
    public static boolean existsRepoURLs(String SERIALIZED_FILTERED_URLS_FILE_NAME) {
        File file = new File(SERIALIZED_FILTERED_URLS_FILE_NAME);
        return file.exists();
    }

    /**
     * writes urls into a file
     *
     * @param urls to be written
     */
    public static void writeURLs(Map<String, Set<String>> urls, String SERIALIZED_TOTAL_URLS_FILE_NAME) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SERIALIZED_TOTAL_URLS_FILE_NAME))) {
            oos.writeObject(urls);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * writes repo urls into a file
     *
     * @param repoURLs to be written
     */
    public static void writeRepoURLs(Set<String> repoURLs, String SERIALIZED_FILTERED_URLS_FILE_NAME) {
        System.out.println(repoURLs.size()+" IS");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SERIALIZED_FILTERED_URLS_FILE_NAME))) {
            oos.writeObject(repoURLs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * counts the occurrence of regex inside the output.txt file
     *
     * @param regex regex to match
     * @return number of occurrence
     * @throws IOException if thrown in reading output.txt file
     */
    public static int countRegexOccurrence(String regex) throws IOException {
        String editedRegex = regex.replace("^[^(\\/\\/)\\*]\\s*", ""); //it is not supported in java but is supported in grep.app
        String repositoriesDirPath = "C:\\Users\\Administrator\\Desktop\\Assertions_generation\\Sources\\Assertion_Repository_Extractor\\repositories\\";
        Set<String> javaFilePaths = Utils.readJavaFile(repositoriesDirPath).stream().map(s -> {
//            System.out.println("QQQ: " +s);
            return Utils.extractJavaAbsolutePath(s, repositoriesDirPath);
        }).collect(Collectors.toSet());

        int numOccurrences = 0;

        Pattern pattern = Pattern.compile(editedRegex, Pattern.CASE_INSENSITIVE);
        for (String filePath : javaFilePaths) {
            try {
                String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));
                Matcher matcher = pattern.matcher(fileContent);
                while (matcher.find())
                    numOccurrences++;
            } catch (IOException e) {
                System.out.printf("File Path : "+filePath);
                e.printStackTrace();
            }
        }
        return numOccurrences;
    }

    public static void writeStatistics(Map<String, RepositoriesInfo> map) {
        System.out.println("Saving... ");
        try (FileWriter writer = new FileWriter("statistics.json")) {
            Gson gson = new Gson();
            Type listType = new TypeToken<Map<String, RepositoriesInfo>>() {
            }.getType();
            gson.toJson(map, listType, writer);
            System.out.println("List of objects has been saved to statistics.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readURLsFromSourceGraphCSV(Map<String, Set<String>> URLs, int numberOfRepositories) {
        String path = "external_source_graph.csv";
        try {
            CSVReader csvReader = new CSVReaderBuilder(new FileReader(path)).withSkipLines(1).build();
            String[] line;

            while ((line = csvReader.readNext()) != null && URLs.size() < numberOfRepositories) {
                String repository = line[0]; // Assuming repository link is in the first column
                String file = line[2]; // Assuming file relative path is in the second column
                if (URLs.containsKey(repository))
                    URLs.get(repository).add(repository + "/blob/master/" + file);
                else {
                    Set<String> set = new HashSet<>();
                    set.add(repository + "/blob/master/" + file);
                    URLs.put(repository, set);
                }
            }

            csvReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

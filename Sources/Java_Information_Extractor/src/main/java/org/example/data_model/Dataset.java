package org.example.data_model;

import com.github.javaparser.ast.body.MethodDeclaration;
import org.example.Extractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Dataset {

    private final String repoDirPath;
    private final String dataFilePath;
    private List<Repository> repositories;

    public Dataset(String repoDirPath, String dataFilePath) {
        this.repoDirPath = repoDirPath;
        this.dataFilePath = dataFilePath;
        this.repositories = new LinkedList<>();
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<Repository> repositories) {
        this.repositories = repositories;
    }

    public void populate() {
        String[] filesPaths = getFilesLocalPath();
        System.out.println(filesPaths.length + " files detected! Parsing... .");
        Arrays.stream(filesPaths).forEach(path -> {
            System.out.println("Parsing item: " + path);
            Extractor extractor = new Extractor(path);
            try {
                for (MethodDeclaration methodDeclaration : extractor.extractMethods()) {
                    try {
                        if (methodDeclaration.getBody().get().toString().contains("assert ")) {
                            if (repositories.stream().noneMatch(repository -> repository.getName().equals(getRepoNameFromPath(path)))) {
                                Repository repository = new Repository();
                                repository.setPath(getRepoLocalPathFromPath(path));
                                repository.setName(getRepoNameFromPath(path));
                                repository.setUrl(getRepoURLFromPath(path));
                                repository.addFileFromPath(path);
                                repositories.add(repository);
                            } else
                                getRepoWithName(getRepoNameFromPath(path)).addFileFromPath(path);
                            break;
                        }
                    }catch (Exception e){
                    }
                }
            } catch (IOException e) {

            }
        });
    }

    private Repository getRepoWithName(String repoName) {
        for (Repository repository : repositories)
            if (repository.getName().equals(repoName))
                return repository;
        return null;
    }

    private String getRepoURLFromPath(String path) {
        return "https://github.com/" + getOwnerNameFromPath(path) + "/" + getRepoNameFromPath(path);
    }

    private String getRepoNameFromPath(String path) {
        int srcIndex = Arrays.asList(path.split("/")).indexOf("repositories");
        return path.split("/")[srcIndex + 1].split("@")[1];
    }

    private String getOwnerNameFromPath(String path) {
        for (String str : path.split("/"))
            if (str.contains("@"))
                return str.split("@")[0];
        return null;
    }


    private String getRepoLocalPathFromPath(String path) {
        Pattern pattern = Pattern.compile("([^\\/]+@[^\\/]+)");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find())
            return String.join("/", Arrays.copyOfRange(path.split("/"), 0,
                    Arrays.asList(path.split("/")).indexOf(matcher.group(1)) + 1));
        return null;
//        path.substring(0, path.indexOf("/src"));
    }

    private String[] getFilesLocalPath() {
        List<String> list = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String ownerName = getOwnerNameFromURL(line);
                String repoName = getRepoNameFromURL(line);
                list.add(repoDirPath
                        + "/"
                        + (ownerName + "@" + repoName)
                        + "/"
                        + getLocalSubURLFrom(line));
            }

            String[] array = list.toArray(new String[0]);
            Arrays.sort(array);
            return array;
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{};
        }
    }

    private String getLocalSubURLFrom(String url) {
        String[] array = url.split("/");
        return String.join("/", Arrays.copyOfRange(array, Arrays.asList(array).indexOf("blob") + 2, array.length));
    }

    private String getOwnerNameFromURL(String url) {
        Pattern pattern = Pattern.compile("github\\.com\\/([^\\/]+)\\/([^\\/]+)");
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String getRepoNameFromURL(String url) {
        Pattern pattern = Pattern.compile("github\\.com\\/([^\\/]+)\\/([^\\/]+)");
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }
}

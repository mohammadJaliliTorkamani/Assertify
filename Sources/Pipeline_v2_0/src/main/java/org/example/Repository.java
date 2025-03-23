package org.example;

import java.util.LinkedList;
import java.util.List;

public class Repository {
    private final List<RepoFile> files;
    private String name;
    private String url;
    private String path;

    public Repository() {
        this.files = new LinkedList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<RepoFile> getFiles() {
        return files;
    }

    public void addFile(RepoFile file) {
        this.files.add(file);
    }
}

package org.example;

import java.util.List;

public class RepoFile {
    private String name;
    private String path;
    private String url;
    private List<Method> methods;

    public RepoFile(String name, String path, String url, List<Method> methods) {
        this.name = name;
        this.path = path;
        this.url = url;
        this.methods = methods;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public void setMethods(List<Method> methods) {
        this.methods = methods;
    }

}

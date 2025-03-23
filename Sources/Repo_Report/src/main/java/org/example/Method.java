package org.example;

import java.util.List;

public class Method {
    private String signature;
    private String javadoc;
    private String originalContent;
    private String contentWithoutAssertion;
    private String contentWithoutComment;
    private String contentWithoutJavaDoc;
    private String className;
    private List<Comment> comments;
    private List<Assertion> assertions;

    public Method(String className, String signature, String javadoc, String originalContent,
                  String contentWithoutAssertion, String contentWithoutComment, String contentWithoutJavaDoc,
                  List<Assertion> assertions, List<Comment> comments) {
        this.className = className;
        this.signature = signature;
        this.javadoc = javadoc;
        this.originalContent = originalContent;
        this.contentWithoutAssertion = contentWithoutAssertion;
        this.contentWithoutComment = contentWithoutComment;
        this.contentWithoutJavaDoc = contentWithoutJavaDoc;
        this.comments = comments;
        this.assertions = assertions;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getJavadoc() {
        return javadoc;
    }

    public void setJavadoc(String javadoc) {
        this.javadoc = javadoc;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getContentWithoutAssertion() {
        return contentWithoutAssertion;
    }

    public void setContentWithoutAssertion(String contentWithoutAssertion) {
        this.contentWithoutAssertion = contentWithoutAssertion;
    }

    public String getContentWithoutComment() {
        return contentWithoutComment;
    }

    public void setContentWithoutComment(String contentWithoutComment) {
        this.contentWithoutComment = contentWithoutComment;
    }

    public String getContentWithoutJavaDoc() {
        return contentWithoutJavaDoc;
    }

    public void setContentWithoutJavaDoc(String contentWithoutJavaDoc) {
        this.contentWithoutJavaDoc = contentWithoutJavaDoc;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Assertion> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<Assertion> assertions) {
        this.assertions = assertions;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}

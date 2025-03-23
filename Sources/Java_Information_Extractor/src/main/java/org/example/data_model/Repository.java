package org.example.data_model;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.javadoc.Javadoc;
import org.example.Extractor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Repository {
    private String name;
    private String url;
    private String path;
    private final List<RepoFile> files;

    public Repository() {
        this.files = new LinkedList<>();
    }

    public static String generateSignature(MethodDeclaration methodDeclaration) {
        StringBuilder signatureBuilder = new StringBuilder();

        // Append modifiers and return type
        for (Modifier modifier : methodDeclaration.getModifiers()) {
            signatureBuilder.append(modifier.getKeyword().toString().toLowerCase());
            signatureBuilder.append(" ");
        }
        signatureBuilder.append(methodDeclaration.getType());
        signatureBuilder.append(" ");

        // Append method name
        signatureBuilder.append(methodDeclaration.getName());

        // Append parameter types and names
        signatureBuilder.append("(");
        for (int i = 0; i < methodDeclaration.getParameters().size(); i++) {
            if (i > 0) {
                signatureBuilder.append(", ");
            }
            signatureBuilder.append(methodDeclaration.getParameter(i).getType());
            signatureBuilder.append(" ");
            signatureBuilder.append(getParameterName(methodDeclaration.getParameter(i)));
        }
        signatureBuilder.append(")");

        return signatureBuilder.toString();
    }

    private static String getParameterName(Parameter parameter) {
        if (parameter.isVarArgs()) {
            return parameter.getNameAsString() + "...";
        }
        return parameter.getNameAsString();
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

    public void addFileFromPath(String path) {
        Extractor extractor = new Extractor(path);
        List<Method> methods = new LinkedList<>();
        try {
            List<MethodDeclaration> methodDeclarations = extractor.extractMethods();
            if (!methodDeclarations.isEmpty()) {
                for (MethodDeclaration methodDeclaration : methodDeclarations) {
                    if(methodDeclaration.toString().trim().contains("assert ")) {
                        List<Assertion> assertions;
                        if (methodDeclaration.getBody().toString().trim().contains("assert "))
                            assertions = methodDeclaration.findAll(AssertStmt.class).stream().map(assertStmt -> new Assertion(assertStmt.toString())).collect(Collectors.toList());
                        else
                            assertions = new LinkedList<>();
                        List<Comment> comments = extractor
                                .extractComments(methodDeclaration.getNameAsString())
                                .stream()
                                .map(com.github.javaparser.ast.comments.Comment::getContent)
                                .map(Comment::new)
                                .collect(Collectors.toList());
                        String signature = generateSignature(methodDeclaration);
                        Javadoc _javadoc = extractor.extractJavadoc(methodDeclaration.getNameAsString());
                        String javadoc = null;
                        if (_javadoc != null)
                            javadoc = _javadoc.toComment().toString();
                        String originalMethod = extractor.printMethod(methodDeclaration, false, false, false);
                        String methodWithoutComments = extractor.printMethod(methodDeclaration, true, false, false);
                        String methodWithoutAssertions = extractor.printMethod(methodDeclaration, false, true, false);
                        String methodWithoutJavadoc = extractor.printMethod(methodDeclaration, false, false, true);

                        Method method = new Method(methodDeclaration.findAncestor(ClassOrInterfaceDeclaration.class).get().getNameAsString(), signature, javadoc, originalMethod, methodWithoutAssertions,
                                methodWithoutComments, methodWithoutJavadoc, assertions, comments);

                        methods.add(method);
                    }
                }

                String className = null;

//                Node parentNode = methodDeclarations.get(0);
//                while (parentNode.getParentNode().isPresent()) {
//                    parentNode = parentNode.getParentNode().get();
//                    if (parentNode instanceof ClassOrInterfaceDeclaration) {
//                        ClassOrInterfaceDeclaration containingClass = (ClassOrInterfaceDeclaration) parentNode;
//                        className = containingClass.getName().toString();
//                        break;
//                    }
//                }
                String[] splitPath = path.split("/");
                String name = splitPath[splitPath.length - 1];

                String url = getFileURLFromPath(path);
                RepoFile repoFile = new RepoFile(name, path, url, methods);
                files.add(repoFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFileURLFromPath(String path) {
        Pattern pattern = Pattern.compile("repositories/([^\\/]+@[^\\/]+)/");

        Matcher matcher = pattern.matcher(path);
        String repoFolderName = null;
        while (matcher.find())
            repoFolderName = matcher.group(1);

        if (repoFolderName != null) {
            String p = Paths.get(
                            Path.of(String.join("/", Arrays.copyOfRange(path.split("/"), 0, Arrays.asList(path.split("/")).indexOf(repoFolderName) + 1))).toString()
                    )
                    .relativize(Path.of(path))
                    .toString().replace("\\", "/");
            return "https://github.com/" + repoFolderName.split("@")[0] + "/" + repoFolderName.split("@")[1]
                    + "/blob/master/" + p;
        }
        return null;
    }
}

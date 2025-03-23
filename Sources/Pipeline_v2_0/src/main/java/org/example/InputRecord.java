package org.example;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class InputRecord {
    private final String className;
    private String name;
    private String path;
    private String alternativePath;
    private String signature;

    public InputRecord(String className, String signature, String name, String path) {
        this.className = className;
        this.signature = signature;
        this.name = name;
        this.path = path;
    }

    public InputRecord(String className, TestDeclaration testDeclaration) {
        this(
                className,
                testDeclaration.getMethodDeclaration().getDeclarationAsString(),
                testDeclaration.getMethodDeclaration().getNameAsString(),
                testDeclaration.getPath()
        );
    }

    public String getClassName() {
        return className;
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

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAlternativePath() {
        return alternativePath;
    }

    public void setAlternativePath(String alternativePath) {
        this.alternativePath = alternativePath;
    }

    @Override
    public String toString() {
        return String.format("Path: %s , Alternative path: %s , Method Name: %s , signature: %s , className : %s", path, alternativePath, name, signature, className);
    }

    public String getRepoPath(boolean backupVersion) {
//        File file = new File(backupVersion ? alternativePath : path);
//        String parentPath = file.getParent();
//
//        while (parentPath != null) {
//            if (parentPath.endsWith("src")) {
//                return file.getParentFile().getParent();
//            }
//            file = new File(parentPath);
//            parentPath = file.getParent();
//        }
//        return null; // Repository address not found

        return Utils.getFirstAddressUpwardHaving(backupVersion ? alternativePath : path, "@", true).toString();
//        File file = new File(backupVersion ? alternativePath : path);
//        do{
//            if(file.getName().contains("@"))
//                return file.getPath();
//            file = file.getParentFile();
//        }while(true);

    }


    public boolean exists(boolean backupVersion) throws Exception {
        boolean fileExists = new File(backupVersion ? this.getAlternativePath() : this.getPath()).exists();
        if (fileExists)
            try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(backupVersion ? alternativePath : path)) {
                MethodVisitor methodVisitor = new MethodVisitor(getClassName(), name, signature);
                List<MethodDeclaration> methodDeclarationList = new LinkedList<>();
                methodVisitor.visit(cuw.getCompilationUnit(), methodDeclarationList);
                return !methodDeclarationList.isEmpty();
            } catch (Exception e) {
                e.printStackTrace();
            }

        return false;
    }

    public String getRepoName() {
        Path filePath = Path.of(path);
        Path repositoryPath = filePath.getParent();
        while (repositoryPath != null) {
            String directoryName = repositoryPath.getFileName().toString();
            if (directoryName.contains("@")) {
                return directoryName;
            }
            repositoryPath = repositoryPath.getParent();
        }
        return "";
    }

    public String getRecordDirName() {
        return getRepoName() + "_" + getName();
    }

    public String getRelativeFilePath() {
//        Path filePath = Path.of(path);
//        Path repositoryPath = filePath.getParent();
//        while (repositoryPath != null) {
//            String directoryName = repositoryPath.getFileName().toString();
//            if (directoryName.contains("@")) {
//                return repositoryPath.relativize(filePath).toString();
//            }
//            repositoryPath = repositoryPath.getParent();
//        }
//        return filePath.toString(); // Repository not found, return the entire file path

        return Utils.getFirstAddressUpwardHaving(path, "@", true).relativize(Path.of(path)).toString();
//        Path filePath = Path.of(path);
//
//        Path repositoryPath = filePath;
//        do{
//            if (repositoryPath.getParent().getFileName().toString().contains("@"))
//                return repositoryPath.getParent().relativize(filePath).toString();
//            repositoryPath = repositoryPath.getParent();
//        }while(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InputRecord other = (InputRecord) o;

        return Objects.equals(signature, other.signature) &&
                Objects.equals(name, other.name) &&
                Objects.equals(path, other.path) &&
                Objects.equals(className, other.className) &&
                Objects.equals(alternativePath, other.alternativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, signature, name, path, alternativePath);
    }
}

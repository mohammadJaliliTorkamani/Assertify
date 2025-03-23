package org.example;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class DatasetStatistics {
    private int totalNumberOfBoundedRepositories; //having a bound(threshold) because of java heap size error
    private int totalNumberOfAssertions;
    private int totalNumberOfLinesHavingAssertions;
    private int totalNumberOfFiles;
    private int totalNumberOfFilesHavingAssertions;
    private int totalNumberOfMethods;
    private int totalNumberOfMethodsHavingAssertions;
    private int totalNumberOfClasses;
    private int totalNumberOfClassesHavingAssertions;
    private int totalNumberOfClassesAssertionsWorkWith;
    private int totalNumberOfMethodsAssertionsWorkWith;
    private int totalNumberOfVariablesAssertionsWorkWith;
    private int totalNumberOfVariables;
    private String methodsCorpusFilePath;

    /**
     * percent
     */
    private double distributionOfAssertionLocationsPerFile;
    /**
     * percent
     */
    private double distributionOfAssertionLocationsPerClass;
    /**
     * percent
     */
    private double distributionOfAssertionLocationsPerMethod;
    /**
     * percent
     */
    private double distributionOfAssertionTargetsPerVariable;
    /**
     * percent
     */
    private double distributionOfAssertionTargetsPerClass;
    /**
     * percent
     */
    private double distributionOfAssertionTargetsPerMethod;

    public DatasetStatistics(String methodsCorpusFilePath) {
        this.methodsCorpusFilePath = methodsCorpusFilePath;
    }

    private static List<File> findNonTestJavaFiles(String projectPath) {
        List<File> javaFiles = new ArrayList<>();
        File projectFolder = new File(projectPath);

        if (!projectFolder.isDirectory()) {
            return javaFiles;
        }

        File[] subDirs = projectFolder.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                if (subDir.getName().toLowerCase().contains("test")) {
                    continue;
                }
                findJavaFiles(subDir, javaFiles);
            }
        }

        return javaFiles;
    }

    private static void findJavaFiles(File directory, List<File> javaFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!file.getName().toLowerCase().contains("test"))
                        findJavaFiles(file, javaFiles);
                } else if (file.getName().endsWith(".java") && !file.getName().endsWith("Test.java") &&
                        file.getPath().contains("src" + File.separator + "main" + File.separator + "java")) {
                    javaFiles.add(file);
                }
            }
        }
    }

    //It is convenient to increase the java heap size by adding the option like -Xss40g
    public boolean extract() throws Exception {
        Gson gson = new Gson();
        System.out.println(methodsCorpusFilePath);
        FileReader _fileReader = new FileReader(methodsCorpusFilePath);
        BufferedReader reader = new BufferedReader(_fileReader);
        Repository[] repositories = gson.fromJson(reader, Repository[].class);

        List<File> nonTestFiles = new ArrayList<>();
        for (int i = 0; i < repositories.length; i++) {
            nonTestFiles.clear();
            nonTestFiles.addAll(findNonTestJavaFiles(repositories[i].getPath()));
            if (nonTestFiles.size() <= Constants.DATASET_STATISTICS_MAX_NON_TEST_JAVA_FILES) {
                totalNumberOfBoundedRepositories++;
                totalNumberOfFiles += nonTestFiles.size();
                for (File file : nonTestFiles) {
                    try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(file.getPath())) {
                        StatisticalAssertionsCounterVisitor assertionVisitor = new StatisticalAssertionsCounterVisitor();
                        cuw.getCompilationUnit().accept(assertionVisitor, null);
                        totalNumberOfAssertions += assertionVisitor.getAssertionCount();
                        totalNumberOfLinesHavingAssertions += assertionVisitor.getAssertionLineCount();
                        totalNumberOfFilesHavingAssertions += assertionVisitor.getOneIfFileHasAnyAssertion();
                        totalNumberOfMethods += assertionVisitor.getMethodsCount();
                        totalNumberOfMethodsHavingAssertions += assertionVisitor.getAssertionMethodCount();
                        totalNumberOfClasses += assertionVisitor.getClassesCount();
                        totalNumberOfClassesHavingAssertions += assertionVisitor.getAssertionClassCount();
                        totalNumberOfClassesAssertionsWorkWith += assertionVisitor.getWorkingAssertionClassCount();
                        totalNumberOfMethodsAssertionsWorkWith += assertionVisitor.getWorkingAssertionMethodsCount();
                        totalNumberOfVariables += assertionVisitor.getVariablesCount();
                        totalNumberOfVariablesAssertionsWorkWith += assertionVisitor.getWorkingAssertionVariablesCount();
                    } catch (Exception e) {//any parsing errors
                    }
                }
            }

            System.out.printf("    %d of %d repositories processed (%%%.2f) (%d repositories processed) | %s%n", i + 1,
                    repositories.length, 100.0 * i / repositories.length, totalNumberOfBoundedRepositories, this);

        }

        return computeOverallStatistics();
    }

    private boolean computeOverallStatistics() {
        if (totalNumberOfFiles == 0 || totalNumberOfClasses == 0 || totalNumberOfMethods == 0 || totalNumberOfVariables == 0)
            return false;
        distributionOfAssertionLocationsPerFile = 100.0 * totalNumberOfFilesHavingAssertions / totalNumberOfFiles;
        distributionOfAssertionLocationsPerClass = 100.0 * totalNumberOfClassesHavingAssertions / totalNumberOfClasses;
        distributionOfAssertionLocationsPerMethod = 100.0 * totalNumberOfMethodsHavingAssertions / totalNumberOfMethods;
        distributionOfAssertionTargetsPerClass = 100.0 * totalNumberOfClassesAssertionsWorkWith / totalNumberOfClasses;
        distributionOfAssertionTargetsPerMethod = 100.0 * totalNumberOfMethodsAssertionsWorkWith / totalNumberOfMethods;
        distributionOfAssertionTargetsPerVariable = 100.0 * totalNumberOfVariablesAssertionsWorkWith / totalNumberOfVariables;
        return true;
    }

    public int getTotalNumberOfBoundedRepositories() {
        return totalNumberOfBoundedRepositories;
    }

    public void setTotalNumberOfBoundedRepositories(int totalNumberOfBoundedRepositories) {
        this.totalNumberOfBoundedRepositories = totalNumberOfBoundedRepositories;
    }

    public int getTotalNumberOfAssertions() {
        return totalNumberOfAssertions;
    }

    public void setTotalNumberOfAssertions(int totalNumberOfAssertions) {
        this.totalNumberOfAssertions = totalNumberOfAssertions;
    }

    public int getTotalNumberOfLinesHavingAssertions() {
        return totalNumberOfLinesHavingAssertions;
    }

    public void setTotalNumberOfLinesHavingAssertions(int totalNumberOfLinesHavingAssertions) {
        this.totalNumberOfLinesHavingAssertions = totalNumberOfLinesHavingAssertions;
    }

    public int getTotalNumberOfFiles() {
        return totalNumberOfFiles;
    }

    public void setTotalNumberOfFiles(int totalNumberOfFiles) {
        this.totalNumberOfFiles = totalNumberOfFiles;
    }

    public int getTotalNumberOfFilesHavingAssertions() {
        return totalNumberOfFilesHavingAssertions;
    }

    public void setTotalNumberOfFilesHavingAssertions(int totalNumberOfFilesHavingAssertions) {
        this.totalNumberOfFilesHavingAssertions = totalNumberOfFilesHavingAssertions;
    }

    public int getTotalNumberOfMethods() {
        return totalNumberOfMethods;
    }

    public void setTotalNumberOfMethods(int totalNumberOfMethods) {
        this.totalNumberOfMethods = totalNumberOfMethods;
    }

    public int getTotalNumberOfMethodsHavingAssertions() {
        return totalNumberOfMethodsHavingAssertions;
    }

    public void setTotalNumberOfMethodsHavingAssertions(int totalNumberOfMethodsHavingAssertions) {
        this.totalNumberOfMethodsHavingAssertions = totalNumberOfMethodsHavingAssertions;
    }

    public int getTotalNumberOfClasses() {
        return totalNumberOfClasses;
    }

    public void setTotalNumberOfClasses(int totalNumberOfClasses) {
        this.totalNumberOfClasses = totalNumberOfClasses;
    }

    public int getTotalNumberOfClassesHavingAssertions() {
        return totalNumberOfClassesHavingAssertions;
    }

    public void setTotalNumberOfClassesHavingAssertions(int totalNumberOfClassesHavingAssertions) {
        this.totalNumberOfClassesHavingAssertions = totalNumberOfClassesHavingAssertions;
    }

    public String getMethodsCorpusFilePath() {
        return methodsCorpusFilePath;
    }

    public void setMethodsCorpusFilePath(String methodsCorpusFilePath) {
        this.methodsCorpusFilePath = methodsCorpusFilePath;
    }

    public int getTotalNumberOfClassesAssertionsWorkWith() {
        return totalNumberOfClassesAssertionsWorkWith;
    }

    public void setTotalNumberOfClassesAssertionsWorkWith(int totalNumberOfClassesAssertionsWorkWith) {
        this.totalNumberOfClassesAssertionsWorkWith = totalNumberOfClassesAssertionsWorkWith;
    }

    public int getTotalNumberOfMethodsAssertionsWorkWith() {
        return totalNumberOfMethodsAssertionsWorkWith;
    }

    public void setTotalNumberOfMethodsAssertionsWorkWith(int totalNumberOfMethodsAssertionsWorkWith) {
        this.totalNumberOfMethodsAssertionsWorkWith = totalNumberOfMethodsAssertionsWorkWith;
    }

    public int getTotalNumberOfVariablesAssertionsWorkWith() {
        return totalNumberOfVariablesAssertionsWorkWith;
    }

    public void setTotalNumberOfVariablesAssertionsWorkWith(int totalNumberOfVariablesAssertionsWorkWith) {
        this.totalNumberOfVariablesAssertionsWorkWith = totalNumberOfVariablesAssertionsWorkWith;
    }

    public int getTotalNumberOfVariables() {
        return totalNumberOfVariables;
    }

    public void setTotalNumberOfVariables(int totalNumberOfVariables) {
        this.totalNumberOfVariables = totalNumberOfVariables;
    }

    public double getDistributionOfAssertionLocationsPerFile() {
        return distributionOfAssertionLocationsPerFile;
    }

    public void setDistributionOfAssertionLocationsPerFile(double distributionOfAssertionLocationsPerFile) {
        this.distributionOfAssertionLocationsPerFile = distributionOfAssertionLocationsPerFile;
    }

    public double getDistributionOfAssertionLocationsPerClass() {
        return distributionOfAssertionLocationsPerClass;
    }

    public void setDistributionOfAssertionLocationsPerClass(double distributionOfAssertionLocationsPerClass) {
        this.distributionOfAssertionLocationsPerClass = distributionOfAssertionLocationsPerClass;
    }

    public double getDistributionOfAssertionLocationsPerMethod() {
        return distributionOfAssertionLocationsPerMethod;
    }

    public void setDistributionOfAssertionLocationsPerMethod(double distributionOfAssertionLocationsPerMethod) {
        this.distributionOfAssertionLocationsPerMethod = distributionOfAssertionLocationsPerMethod;
    }

    public double getDistributionOfAssertionTargetsPerVariable() {
        return distributionOfAssertionTargetsPerVariable;
    }

    public void setDistributionOfAssertionTargetsPerVariable(double distributionOfAssertionTargetsPerVariable) {
        this.distributionOfAssertionTargetsPerVariable = distributionOfAssertionTargetsPerVariable;
    }

    public double getDistributionOfAssertionTargetsPerClass() {
        return distributionOfAssertionTargetsPerClass;
    }

    public void setDistributionOfAssertionTargetsPerClass(double distributionOfAssertionTargetsPerClass) {
        this.distributionOfAssertionTargetsPerClass = distributionOfAssertionTargetsPerClass;
    }

    public double getDistributionOfAssertionTargetsPerMethod() {
        return distributionOfAssertionTargetsPerMethod;
    }

    public void setDistributionOfAssertionTargetsPerMethod(double distributionOfAssertionTargetsPerMethod) {
        this.distributionOfAssertionTargetsPerMethod = distributionOfAssertionTargetsPerMethod;
    }

    @Override
    public String toString() {
        return "DatasetStatistics{" +
                "totalNumberOfAssertions=" + totalNumberOfAssertions +
                ", totalNumberOfLinesHavingAssertions=" + totalNumberOfLinesHavingAssertions +
                ", totalNumberOfFiles=" + totalNumberOfFiles +
                ", totalNumberOfFilesHavingAssertions=" + totalNumberOfFilesHavingAssertions +
                ", totalNumberOfMethods=" + totalNumberOfMethods +
                ", totalNumberOfMethodsHavingAssertions=" + totalNumberOfMethodsHavingAssertions +
                ", totalNumberOfClasses=" + totalNumberOfClasses +
                ", totalNumberOfClassesHavingAssertions=" + totalNumberOfClassesHavingAssertions +
                ", totalNumberOfClassesAssertionsWorkWith=" + totalNumberOfClassesAssertionsWorkWith +
                ", totalNumberOfMethodsAssertionsWorkWith=" + totalNumberOfMethodsAssertionsWorkWith +
                ", totalNumberOfVariablesAssertionsWorkWith=" + totalNumberOfVariablesAssertionsWorkWith +
                ", totalNumberOfVariables=" + totalNumberOfVariables +
                '}';
    }
}

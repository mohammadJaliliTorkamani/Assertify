package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.gson.Gson;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class DataModulator {
    private final String evalInputFilePath;
    private final String fslDatabaseInputFilePath;
    private final double evalSetDatasetPopulationRatio;
    private final String methodsCorpusFilePath;
    private final List<FSLRecord> fslRecords;
    private final Set<InputRecord> evalRecords;
    private final int methodsCorpusMinimumAcceptableNumberOfAssertions;

    public DataModulator(String methodsCorpusFilePath, String evalInputFilePath,
                         String fslDatabaseInputFilePath, int methodsCorpusMinimumAcceptableNumberOfAssertions,
                         double evalSetDatasetPopulationRatio) {
        this.evalInputFilePath = evalInputFilePath;
        this.methodsCorpusFilePath = methodsCorpusFilePath;
        this.fslDatabaseInputFilePath = fslDatabaseInputFilePath;
        this.evalSetDatasetPopulationRatio = evalSetDatasetPopulationRatio;
        this.fslRecords = new LinkedList<>();
        this.evalRecords = new HashSet<>();
        this.methodsCorpusMinimumAcceptableNumberOfAssertions = methodsCorpusMinimumAcceptableNumberOfAssertions;
    }

//    private static boolean belongsToOneRepositoryAt(String repoName, Set<InputRecord> list) {
//        return list.stream().anyMatch(record -> record.getRepoName().contains(repoName));
//    }


    public double populate(LLM_Client client, int evalSizeThreshold, int fslSizeThreshold) {
        double cost = 0;
        if (!hasEvalInputRecords() || !hasEvalInputRecords()) {
            System.out.println("Datasets not found. Generating...");
            cost = generateFilteredDatasets(client);
        }

        System.out.println("Reading datasets from disk...");
        evalRecords.clear();
        try {
            evalRecords.addAll(Utils.readEvalInputRecords(evalInputFilePath));
            if (evalSizeThreshold != -1 && evalRecords.size() > evalSizeThreshold) {
                Iterator<InputRecord> iterator = evalRecords.iterator();
                for (int i = 0; i < evalRecords.size() - evalSizeThreshold; i++)
                    evalRecords.remove(iterator.next());
            }

            System.out.println("Read " + evalRecords.size() + " records of evaluation datasets from disk.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            fslRecords.addAll(Utils.readFSLRecords(fslDatabaseInputFilePath));
            if (fslSizeThreshold != -1 && fslRecords.size() > fslSizeThreshold) {
                Iterator<FSLRecord> iterator = fslRecords.iterator();
                for (int i = 0; i < fslRecords.size() - fslSizeThreshold; i++)
                    fslRecords.remove(iterator.next());
            }
            System.out.println("Read " + fslRecords.size() + " records of FSL datasets from disk.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cost;
    }

    public double populate(LLM_Client client) {
        return populate(client, -1, -1);
    }

    private double generateFilteredDatasets(LLM_Client client) {
        System.out.println("Filtering dataset based on minimum number of " + Constants.METHODS_CORPUS_MINIMUM_REPO_ASSERTIONS_THRESHOLD + " assertions per repo...");
        List<Repository> filteredRepositories = new ArrayList<>();
        List<Repository> compilableValidRepositories = new LinkedList<>();
        Gson gson = new Gson();
        double cost = -1;

        try {
            FileReader fileReader = new FileReader(methodsCorpusFilePath);
            BufferedReader reader = new BufferedReader(fileReader);
            Repository[] repositories = gson.fromJson(reader, Repository[].class);

            int counter = 1;
            for (Repository repository : repositories) {
                System.out.println("Compiling repository " + (counter++) + " out of " + repositories.length + " (" + repository.getName() + " | " + repository.getPath() + ")...");
//                if (isAmong(repository)) {// || (repoHasAnyValidAndSufficientAssertion(repository, methodsCorpusMinimumAcceptableNumberOfAssertions) && isRepositoryCompilable(repository))) {
                if (repoHasAnyValidAndSufficientAssertion(repository, methodsCorpusMinimumAcceptableNumberOfAssertions) && isRepositoryCompilable(repository)) {
                    System.out.println("Added");
                    compilableValidRepositories.add(repository);
                } else
                    System.out.println("Discarded");
            }

            System.out.println("Saving compilable valid repositories...");
            String json = gson.toJson(compilableValidRepositories);
            try (FileWriter writer = new FileWriter(new File(methodsCorpusFilePath).getParentFile().getAbsolutePath() + File.separator + "compilable_valid_and_filtered_methods_corpus.json")) {
                writer.write(json);
            } catch (IOException e) {
                e.printStackTrace();
            }


            int totalNumberOfAssertions = 0;
            int totalNumberOfMethodsHavingAssertions = 0;
            int totalNumberOfRepositoriesHavingAssertions = 0;
            int totalNumberOfFilesHavingAssertions = 0;

            boolean consideredRepo;
            boolean consideredFile;

            for (Repository repository : compilableValidRepositories) {
                consideredRepo = false;
                for (RepoFile repoFile : repository.getFiles()) {
                    consideredFile = false;
                    for (Method method : repoFile.getMethods()) {
                        if (hasAssertions(method) && !method.getOriginalContent().contains("instanceof") && new JavaParser()
                                .parseMethodDeclaration(method.getOriginalContent())
                                .getResult()
                                .orElse(null)!=null) {
                            totalNumberOfMethodsHavingAssertions++;
                            totalNumberOfAssertions += method.getAssertions().stream().filter(assertion -> assertion.getType().equals("java")).count();
                            if (!consideredRepo) {
                                consideredRepo = true;
                                totalNumberOfRepositoriesHavingAssertions++;
                            }

                            if (!consideredFile) {
                                consideredFile = true;
                                totalNumberOfFilesHavingAssertions++;
                            }
                        }
                    }
                }
            }

            System.out.println("---------------- Statistics ----------------");
            System.out.println("#Compilable valid repositories: " + totalNumberOfRepositoriesHavingAssertions);
            System.out.println("#File having production assertions: " + totalNumberOfFilesHavingAssertions);
            System.out.println("#Methods having production assertions: " + totalNumberOfMethodsHavingAssertions);
            System.out.println("#Production assertions: " + totalNumberOfAssertions);
            System.out.println("--------------------------------------------");

            //Collecting Methods linked to files and repos
            System.out.println("Shuffling methods...");
            List<Pair<Method, Pair<RepoFile, Repository>>> methods = new LinkedList<>();
            Set<String> signatures = new HashSet<>();
            for (Repository repository : compilableValidRepositories) {
                for (RepoFile repoFile : repository.getFiles()) {
                    for (Method method : repoFile.getMethods()) {
                        try {
                            if (hasAssertions(method) && !method.getOriginalContent().contains("instanceof") && new JavaParser()
                                    .parseMethodDeclaration(method.getOriginalContent())
                                    .getResult()
                                    .orElse(null) != null && !signatures.contains(repository.getPath()+" "+repoFile.getPath()+" "+method.getOriginalContent())){
                                methods.add(Pair.of(method, Pair.of(repoFile, repository)));
                                signatures.add(repository.getPath()+" "+repoFile.getPath()+" "+method.getOriginalContent());
                                }
                        }catch (Exception e){}
                    }
                }
            }

            //Shuffling
            System.out.println("Shuffling "+methods.size()+" methods...");
            Collections.shuffle(methods);

            //Construction of Eval and FSL methods list

            int evalSize = (int)(methods.size() * evalSetDatasetPopulationRatio);

            int fslDBSize = methods.size() - evalSize;

            List<Pair<Method, Pair<RepoFile, Repository>>> evalMethods = new LinkedList<>(methods.subList(0, evalSize));
            List<Pair<Method, Pair<RepoFile, Repository>>> FSLMethods = new LinkedList<>(methods.subList(evalSize, evalSize + fslDBSize));

            // save as dataset files
            System.out.println("Constructing evaluation set...");
            Set<InputRecord> evalRecords = new HashSet<>();
            List<String> evalUniqueRepos = new LinkedList<>();
            List<String> evalUniqueFiles = new LinkedList<>();
            List<String> evalUniqueMethods = new LinkedList<>();
            int numberOfEvalAssertions = 0;

            for (Pair<Method, Pair<RepoFile, Repository>> pair : evalMethods) {
                Method method = pair.getFirst();
                RepoFile repoFile = pair.getSecond().getFirst();
                Repository repository = pair.getSecond().getSecond();

                ///
                MethodDeclaration unresolvableMD = new JavaParser()
                        .parseMethodDeclaration(method.getOriginalContent())
                        .getResult()
                        .orElse(null);

                    if (!evalUniqueRepos.contains(repository.getPath()))
                        evalUniqueRepos.add(repository.getPath());
                    if (!evalUniqueFiles.contains(repoFile.getPath()))
                        evalUniqueFiles.add(repoFile.getPath());
                    if (!evalUniqueMethods.contains(method.getOriginalContent()+" "+repoFile.getPath()))
                        evalUniqueMethods.add(method.getOriginalContent()+" "+repoFile.getPath());
                    numberOfEvalAssertions += method.getAssertions().stream().filter(assertion -> assertion.getType().equals("java")).count();

                    InputRecord record = new InputRecord(method.getClassName(),
                            unresolvableMD.getDeclarationAsString(),
                            unresolvableMD.getNameAsString(),
                            repoFile.getPath());

                    evalRecords.add(record);
            }

            System.out.println("--------------- Evaluation Set Statistics ---------------");
            System.out.println("#Unique Repositories: " + evalUniqueRepos.size());
            System.out.println("#Unique Files: " + evalUniqueFiles.size());
            System.out.println("#Unique Methods: " + evalUniqueMethods.size());
            System.out.println("#Unique Total Assertions: " + numberOfEvalAssertions);
            System.out.println("---------------------------------------------------------");

            if (!evalRecords.isEmpty()) {
                System.out.println("Saving Eval set...");
                Utils.saveInputToFile(evalRecords, evalInputFilePath);
            }

            System.out.println("Constructing FSL dataset...");
            List<FSLRecord> fslRecords = new LinkedList<>();

            List<String> FSLUniqueRepos = new LinkedList<>();
            List<String> FSLUniqueFiles = new LinkedList<>();
            List<String> FSLUniqueMethods = new LinkedList<>();
            int numberOfFSLAssertions = 0;
            for (Pair<Method, Pair<RepoFile, Repository>> pair : FSLMethods) {
                Method method = pair.getFirst();
                RepoFile repoFile = pair.getSecond().getFirst();
                Repository repository = pair.getSecond().getSecond();
                MethodDeclaration unresolvableMD = new JavaParser()
                        .parseMethodDeclaration(method.getOriginalContent())
                        .getResult()
                        .orElse(null);

                if (unresolvableMD != null) {
                    if (!FSLUniqueRepos.contains(repository.getPath()))
                        FSLUniqueRepos.add(repository.getPath());
                    if (!FSLUniqueFiles.contains(repoFile.getPath()))
                        FSLUniqueFiles.add(repoFile.getPath());
                    if (!FSLUniqueMethods.contains(method.getOriginalContent()+" "+repoFile.getPath()))
                        FSLUniqueMethods.add(method.getOriginalContent()+" "+repoFile.getPath());
                    numberOfFSLAssertions += method.getAssertions().stream().filter(assertion -> assertion.getType().equals("java")).count();

                    InputRecord record = new InputRecord(method.getClassName(),
                            unresolvableMD.getDeclarationAsString(),
                            unresolvableMD.getNameAsString(),
                            repoFile.getPath());


                    addToFSLRecordIfNotContains(new FSLRecord(method, record), fslRecords);

                } else
                    System.out.println("Could not resolve FSL set method => " + method.getOriginalContent());
            }

            System.out.println("--------------- FSL Set Statistics ---------------");
            System.out.println("#Repositories: " + FSLUniqueRepos.size());
            System.out.println("#Files: " + FSLUniqueFiles.size());
            System.out.println("#Methods: " + FSLUniqueMethods.size());
            System.out.println("#Assertions: " + numberOfFSLAssertions);
            System.out.println("---------------------------------------------------------");
            if (!fslRecords.isEmpty()) {
                System.out.println("Saving FSL dataset...");
                Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
                System.out.println("Calculating embedding vectors...");
                cost = FSLRecord.calculateEmbeddings(fslRecords, client);
                List<FSLRecord> purifiedFSL = fslRecords.stream().filter(fslRecord -> fslRecord.getEmbeddingVector() != null && !fslRecord.getEmbeddingVector().isEmpty()).collect(Collectors.toList());
                fslRecords.clear();
                fslRecords.addAll(purifiedFSL);
                System.out.println("Saving new FSL dataset...");
                Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return cost;
    }

    private static boolean isRepositoryCompilable(Repository repository) {
        BuildToolModel buildToolModel = BuildToolModel.getProjectBuildToolModel(repository.getPath());
        if (buildToolModel != null) {
            try {
                return buildToolModel.compile(true, null).isOK();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private static boolean repoHasAnyValidAndSufficientAssertion(Repository repository, int minimumNumberOfAssertions) {
        int validAssertions = 0;
        for (RepoFile repoFile : repository.getFiles())
            for (Method method : repoFile.getMethods())
                for (Assertion assertion : method.getAssertions())
                    if (assertion.getType().equals("java")) { //always must happen because that is how methods' corpus is created
                        validAssertions++;
                    }

        return validAssertions >= minimumNumberOfAssertions;
    }

    private boolean notBelongsToEvalMethods(InputRecord record, Set<InputRecord> records) {
        return !records.contains(record);
    }

//    @Deprecated
//    public double generate(LLM_Client client, Parser parser) throws Exception {
//        double cost = 0;
//        if (hasFSLRecords()) {
//            System.out.println("Reading FSL records from file....");
//            this.fslRecords.addAll(Utils.readFSLRecords(fslDatabaseInputFilePath));
//            printLog(0);
//            if (!this.fslRecords.isEmpty() && fslRecords.get(0).getEmbeddingVector() == null) {
//                System.out.println("FSL records do not have embedding vectors. Calculating embeddings....");
//                cost = FSLRecord.calculateEmbeddings(fslRecords, client);
//                List<FSLRecord> purifiedFSL = fslRecords.stream().filter(fslRecord -> fslRecord.getEmbeddingVector() != null && !fslRecord.getEmbeddingVector().isEmpty()).collect(Collectors.toList());
//                fslRecords.clear();
//                fslRecords.addAll(purifiedFSL);
//                System.out.println("Saving updated FSL records file...");
//                Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//            }
//        }
//        if (hasEvalInputRecords()) {
//            System.out.println("Reading Evaluation records from file....");
//            this.evalRecords.addAll(Utils.readEvalInputRecords(evalInputFilePath));
//            printLog(0);
//        }
//        if (hasTestInputMethodsCorpusRecords()) {
//            System.out.println("Reading Test records from file....");
//            this.testRecords.addAll(Utils.readTestInputRecords(testInputFilePath));
//            this.testRecordsMethods.addAll(
//                    testRecords.stream().map(
//                                    testInputRecord -> new MethodInfo(testInputRecord.getMethodSignature(),
//                                            testInputRecord.getMethodClassName(),
//                                            testInputRecord.getMethodName(),
//                                            testInputRecord.getMethodPath()))
//                            .collect(Collectors.toList())
//            );
//            printLog(0);
//        }
//
//        FileReader fileReader = new FileReader(methodsCorpusFilePath);
//        BufferedReader reader = new BufferedReader(fileReader);
//        if (isEvalNeeded()) {
//            System.out.println("Generating Eval inputs...");
//            generateEvalInputs(reader);
//        }
//
//        if (isTestNeeded() || (fslRecords.size() < fslDBSize && fslDBSize != Double.POSITIVE_INFINITY)) {
//            FileReader _fileReader = new FileReader(methodsCorpusFilePath);
//            BufferedReader _reader = new BufferedReader(_fileReader);
//            System.out.println("Generating FSL & Test inputs...");
//            generateFSLAndTestInputs(client, _reader);
//        }
//
//        if (Constants.GENERATE_TOGA_DATASET) {
//            System.out.println("Generating ToGA records for Eval inputs...");
//            if (!Utils.generateToGAInput(parser, getEvalRecords()))
//                System.err.println("ToGA dataset generation failed...");
//        }
//
//        return cost;
//    }
//
//    private void generateFSLAndTestInputs(LLM_Client client, BufferedReader reader) throws Exception {
//        Gson gson = new Gson();
//        int counter = 0;
//        Set<InputRecord> set = new HashSet<>();
//        InputRecord record;
//
//        Repository[] repositories = gson.fromJson(reader, Repository[].class);
//        for (Repository repository : repositories) {
//            if (belongsToOneRepositoryAt(repository.getName(), evalRecords)) {
//                for (RepoFile repoFile : repository.getFiles()) {
//                    for (Method method : repoFile.getMethods()) {
//                        MethodDeclaration unresolvableMD = new JavaParser()
//                                .parseMethodDeclaration(method.getOriginalContent())
//                                .getResult()
//                                .orElse(null);
//                        if (unresolvableMD != null) {
//
//                            record = new InputRecord(method.getClassName(),
//                                    unresolvableMD.getDeclarationAsString(),
//                                    unresolvableMD.getNameAsString(),
//                                    repoFile.getPath());
//
//                            if (hasAssertions(method)) {
//                                if (isTestNeeded()) {
//                                    set.clear();
//                                    if (notBelongsToEvalMethods(record, evalRecords)) {
//                                        testAnalyzer
//                                                .inspect(record, false, unresolvableMD)
//                                                .forEach(testDeclaration -> set.add(new InputRecord(null, testDeclaration)));
//                                        if (!set.isEmpty()) {
//                                            for (InputRecord inputRecord : set)
////                                            InputRecord inputRecord = set.iterator().next();
//                                                addToTestRecord(new TestInputRecord(
//                                                        method.getClassName(),
//                                                        repository.getPath(),
//                                                        inputRecord.getName(),
//                                                        inputRecord.getSignature(),
//                                                        unresolvableMD.getNameAsString(),
//                                                        unresolvableMD.getDeclarationAsString(),
//                                                        inputRecord.getPath(),
//                                                        repoFile.getPath()
//                                                ), testRecords);
//                                        } else {
//                                            if (isFSLNeeded())
//                                                addToFSLRecordIfNotContains(new FSLRecord(method, record), fslRecords);
//                                        }
//                                    }
//                                } else {
//                                    if (isFSLNeeded() && notBelongsToEvalMethods(record, evalRecords))
//                                        addToFSLRecordIfNotContains(new FSLRecord(method, record), fslRecords);
//                                }
//                            } else {
//                                if (isTestNeeded()) {
//                                    set.clear();
//                                    if (notBelongsToEvalMethods(record, evalRecords)) {
//                                        testAnalyzer
//                                                .inspect(record, false, unresolvableMD)
//                                                .forEach(testDeclaration -> set.add(new InputRecord(null, testDeclaration)));
//                                        if (!set.isEmpty()) {
//                                            for (InputRecord inputRecord : set)
////                                            InputRecord inputRecord = set.iterator().next();
//                                                addToTestRecord(new TestInputRecord(
//                                                        method.getClassName(),
//                                                        repository.getPath(),
//                                                        inputRecord.getName(),
//                                                        inputRecord.getSignature(),
//                                                        unresolvableMD.getNameAsString(),
//                                                        unresolvableMD.getDeclarationAsString(),
//                                                        inputRecord.getPath(),
//                                                        repoFile.getPath()
//                                                ), testRecords);
//                                        }
//                                    }
//                                }
//                            }
//                        }
//
//                        printLog(++counter);
//                        if (noFSLOrTestRequired()) {
//                            Utils.saveInputToFile(testRecords, testInputFilePath);
//                            Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//                            System.out.println("Calculating embeddings....");
//                            FSLRecord.calculateEmbeddings(fslRecords, client);
//                            List<FSLRecord> purifiedFSL = fslRecords.stream().filter(fslRecord -> fslRecord.getEmbeddingVector() != null && !fslRecord.getEmbeddingVector().isEmpty()).collect(Collectors.toList());
//                            fslRecords.clear();
//                            fslRecords.addAll(purifiedFSL);
//                            Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//                            return;
//                        }
//                    }
//                }
//            } else {
//                for (RepoFile repoFile : repository.getFiles()) {
//                    for (Method method : repoFile.getMethods()) {
//
//                        MethodDeclaration unresolvableMD = new JavaParser()
//                                .parseMethodDeclaration(method.getOriginalContent())
//                                .getResult()
//                                .orElse(null);
//
//                        if (unresolvableMD != null) {
//                            if (hasAssertions(method)) {
//                                if (isFSLNeeded()) {
//                                    record = new InputRecord(method.getClassName(),
//                                            unresolvableMD.getDeclarationAsString(),
//                                            unresolvableMD.getNameAsString(),
//                                            repoFile.getPath());
//
//                                    addToFSLRecordIfNotContains(new FSLRecord(method, record), fslRecords);
//                                }
//                            }
//                        }
//
//                        if (noFSLOrTestRequired()) {
//                            Utils.saveInputToFile(testRecords, testInputFilePath);
//                            Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//                            System.out.println("Calculating embeddings....");
//                            FSLRecord.calculateEmbeddings(fslRecords, client);
//                            List<FSLRecord> purifiedFSL = fslRecords.stream().filter(fslRecord -> fslRecord.getEmbeddingVector() != null && !fslRecord.getEmbeddingVector().isEmpty()).collect(Collectors.toList());
//                            fslRecords.clear();
//                            fslRecords.addAll(purifiedFSL);
//                            Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//                            return;
//                        }
//                    }
//                }
//            }
//        }
//
//        if (fslDBSize != Double.POSITIVE_INFINITY)
//            throw new Exception("Could not find enough records for fsl and test. current size is: " +
//                    fslRecords.size() + ", " + testRecords.size());
//        else {
//            Utils.saveInputToFile(testRecords, testInputFilePath);
//            Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//            System.out.println("Calculating embeddings....");
//            FSLRecord.calculateEmbeddings(fslRecords, client);
//            List<FSLRecord> purifiedFSL = fslRecords.stream().filter(fslRecord -> fslRecord.getEmbeddingVector() != null && !fslRecord.getEmbeddingVector().isEmpty()).collect(Collectors.toList());
//            fslRecords.clear();
//            fslRecords.addAll(purifiedFSL);
//            Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//        }
//    }
//
//    private boolean noFSLOrTestRequired() {
//        return (fslDBSize != Double.POSITIVE_INFINITY && fslRecords.size() == fslDBSize) && !isEvalNeeded() && !isTestNeeded();
//    }


    private void addToFSLRecordIfNotContains(FSLRecord record, List<FSLRecord> fslRecords) {
        //given that sometimes there could be identical methods in different files having assertions, we just store a new method if its method with and without assertions were uniq. not path or name
        if (fslRecords.stream().noneMatch(fslRecord ->
                fslRecord.getMethodWithAssertions().equals(record.getMethodWithAssertions()) &&
                        fslRecord.getMethodWithoutAssertions().equals(record.getMethodWithoutAssertions()) &&
                fslRecord.getPath().equals(record.getPath())))
            fslRecords.add(record);
        else
            System.out.println("Redundant FSL record not added");
    }

//    private void addToTestRecord(TestInputRecord record, Set<TestInputRecord> testRecords) {
//        testRecords.add(record);
//        testRecordsMethods.add(
//                new MethodInfo(
//                        record.getMethodSignature(),
//                        record.getMethodClassName(),
//                        record.getMethodName(),
//                        record.getMethodPath()
//                )
//        );
//    }

//    private void generateEvalInputs(BufferedReader reader) throws Exception {
//        Gson gson = new Gson();
//        int counter = 0;
//        Set<InputRecord> set = new HashSet<>();
//        InputRecord record;
//
//        Repository[] repositories = gson.fromJson(reader, Repository[].class);
//        for (Repository repository : repositories) {
//            for (RepoFile repoFile : repository.getFiles()) {
//                for (Method method : repoFile.getMethods()) {
//                    if (hasAssertions(method)) {
//                        MethodDeclaration unresolvableMD = new JavaParser()
//                                .parseMethodDeclaration(method.getOriginalContent())
//                                .getResult()
//                                .orElse(null);
//
//                        if (unresolvableMD != null) {
//                            record = new InputRecord(method.getClassName(),
//                                    unresolvableMD.getDeclarationAsString(),
//                                    unresolvableMD.getNameAsString(),
//                                    repoFile.getPath());
//
//                            set.clear();
//                            testAnalyzer
//                                    .inspect(record, false, unresolvableMD)
//                                    .forEach(testDeclaration -> set.add(new InputRecord(null, testDeclaration)));
//
//                            if (!set.isEmpty() && isEvalNeeded()) {
//                                evalRecords.add(record);
//                            }
//
//
//                            printLog(++counter);
//                            if (!isEvalNeeded()) {
//                                Utils.saveInputToFile(evalRecords, evalInputFilePath);
//                                return;
//                            }
//                        } else
//                            System.out.printf("Error occurred while parsing original method: %n%s%n", method.getOriginalContent());
//                    }
//                }
//            }
//        }
//
//        throw new Exception("Could not find enough records for eval. current size is: " +
//                evalRecords.size() + " / " + evalSize);
//    }

//    private boolean isFSLNeeded() {
//        return fslDBSize == Double.POSITIVE_INFINITY || fslRecords.size() < fslDBSize;
//    }
//
//    private boolean isEvalNeeded() {
//        return evalRecords.size() < evalSize;
//    }
//
//    private boolean isTestNeeded() {
//        return testRecordsMethods.size() < testSize;
//    }

    private static boolean hasAssertions(Method method) {
        return method.getAssertions().stream().anyMatch(assertion -> assertion.getType().equals("java"));
    }

    public Set<InputRecord> getEvalRecords() {
        return evalRecords;
    }

    public List<FSLRecord> getFslRecords() {
        return fslRecords;
    }

    private boolean hasFSLRecords() {
        File FSLFile = new File(fslDatabaseInputFilePath);
        return FSLFile.exists() && FSLFile.isFile();
    }

    private boolean hasEvalInputRecords() {
        File evalFile = new File(evalInputFilePath);
        return evalFile.exists() && evalFile.isFile();
    }

    public DatasetStatistics computeDBStatistics() throws Exception{
        DatasetStatistics datasetStatistics = new DatasetStatistics(methodsCorpusFilePath);
        if (datasetStatistics.extract())
            return datasetStatistics;
        return null;
    }

//    private void printLog(int methodNumber) {
//        System.out.printf("FSL: %d / %s , EVAL: %d / %d , TEST_METHOD: %d / %d , TESTS: %d | Method Number: %d%n",
//                fslRecords.size(), (fslDBSize == Double.POSITIVE_INFINITY ? "As Much" : (int) fslDBSize),
//                evalRecords.size(), evalSize,
//                testRecordsMethods.size(), testSize,
//                testRecords.size(),
//                methodNumber);
//    }

//    public String getAlternativeRepoPathOf(TestInputRecord tRecord) {
//        for (InputRecord inputRecord : evalRecords)
//            if (inputRecord.getName().equals(tRecord.getMethodName()) &&
//                    inputRecord.getRepoName().equals(tRecord.getRepoName()) &&
//                    inputRecord.getClassName().equals(tRecord.getMethodClassName()) &&
//                    inputRecord.getRepoPath(false).equals(tRecord.getRepoPath()))
//                return inputRecord.getAlternativePath();
//        return null;
//    }

//    public DatasetStatistics computeDBStatistics() throws Exception {
//        DatasetStatistics datasetStatistics = new DatasetStatistics(methodsCorpusFilePath);
//        if (datasetStatistics.extract())
//            return datasetStatistics;
//        return null;
//    }

//    private boolean fileHasAssertions(RepoFile file) {
//        for (Method method : file.getMethods()) {
//            if (hasAssertions(method)) {
//                return true;
//            }
//        }
//        return false;
//    }

//    private int computeTotalNumberOfAssertions(Repository repository) {
//        int totalNumberOfAssertions = 0;
//        for (RepoFile repoFile : repository.getFiles()) {
//            for (Method method : repoFile.getMethods()) {
//                if (hasAssertions(method))
//                    totalNumberOfAssertions += method.getAssertions().size();
//            }
//        }
//        return totalNumberOfAssertions;
//    }

//    public double new_generate(LLM_Client client, Parser parser) throws Exception {
//        double cost = 0;
//        if (hasFSLRecords()) {
//            System.out.println("Reading FSL records from file....");
//            this.fslRecords.addAll(Utils.readFSLRecords(fslDatabaseInputFilePath));
//            printLog(0);
//            if (!this.fslRecords.isEmpty() && fslRecords.get(0).getEmbeddingVector() == null) {
//                System.out.println("FSL records do not have embedding vectors. Calculating embeddings....");
//                cost = FSLRecord.calculateEmbeddings(fslRecords, client);
//                List<FSLRecord> purifiedFSL = fslRecords.stream().filter(fslRecord -> fslRecord.getEmbeddingVector() != null && !fslRecord.getEmbeddingVector().isEmpty()).collect(Collectors.toList());
//                fslRecords.clear();
//                fslRecords.addAll(purifiedFSL);
//                System.out.println("Saving updated FSL records file...");
//                Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//            }
//        }
//        if (hasEvalInputRecords()) {
//            System.out.println("Reading Evaluation records from file....");
//            this.evalRecords.addAll(Utils.readEvalInputRecords(evalInputFilePath));
//            printLog(0);
//        }
//
//        if (isEvalNeeded() || isFSLNeeded()) {
//            FileReader fileReader = new FileReader(methodsCorpusFilePath);
//            BufferedReader reader = new BufferedReader(fileReader);
//
//            ////////////////////
//            Gson gson = new Gson();
//            int counter = 0;
//            InputRecord record;
//
//            Repository[] repositories = gson.fromJson(reader, Repository[].class);
//            for (Repository repository : repositories) {
//                for (RepoFile repoFile : repository.getFiles()) {
//                    for (Method method : repoFile.getMethods()) {
//                        if (hasAssertions(method)) {
//                            MethodDeclaration unresolvableMD = new JavaParser()
//                                    .parseMethodDeclaration(method.getOriginalContent())
//                                    .getResult()
//                                    .orElse(null);
//
//                            if (unresolvableMD != null) {
//                                record = new InputRecord(method.getClassName(),
//                                        unresolvableMD.getDeclarationAsString(),
//                                        unresolvableMD.getNameAsString(),
//                                        repoFile.getPath());
//
//                                if (isEvalNeeded()) {
//                                    evalRecords.add(record);
//                                    if (!isEvalNeeded())
//                                        Utils.saveInputToFile(evalRecords, evalInputFilePath);
//                                } else if (fslDBSize == Double.POSITIVE_INFINITY || fslRecords.size() < fslDBSize) {
//                                    fslRecords.add(new FSLRecord(method, record));
//                                }
//                                printLog(++counter);
//                                if (fslRecords.size() == fslDBSize) {
//                                    Utils.saveInputToFile(testRecords, testInputFilePath);
//                                    Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//                                    System.out.println("Calculating embeddings....");
//                                    cost = FSLRecord.calculateEmbeddings(fslRecords, client);
//                                    List<FSLRecord> purifiedFSL = fslRecords.stream().filter(fslRecord -> fslRecord.getEmbeddingVector() != null && !fslRecord.getEmbeddingVector().isEmpty()).collect(Collectors.toList());
//                                    fslRecords.clear();
//                                    fslRecords.addAll(purifiedFSL);
//                                    Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//                                    if (Constants.GENERATE_TOGA_DATASET) {
//                                        System.out.println("Generating ToGA records for Eval inputs...");
//                                        if (!Utils.generateToGAInput(parser, getEvalRecords()))
//                                            System.err.println("ToGA dataset generation failed...");
//                                    }
//                                    return cost;
//                                }
//                            } else
//                                System.out.printf("Error occurred while parsing original method: %n%s%n", method.getOriginalContent());
//                        } else
//                            System.out.println("Take this: " + repository.getUrl() + " and " + method.getOriginalContent());
//                    }
//                }
//            }
//            if (fslDBSize == Double.POSITIVE_INFINITY || fslRecords.size() == fslDBSize) {
//                Utils.saveInputToFile(testRecords, testInputFilePath);
//                Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//                System.out.println("Calculating embeddings....");
//                cost = FSLRecord.calculateEmbeddings(fslRecords, client);
//                List<FSLRecord> purifiedFSL = fslRecords.stream().filter(fslRecord -> fslRecord.getEmbeddingVector() != null && !fslRecord.getEmbeddingVector().isEmpty()).collect(Collectors.toList());
//                fslRecords.clear();
//                fslRecords.addAll(purifiedFSL);
//                Utils.saveFSLRecordToFile(fslRecords, fslDatabaseInputFilePath);
//                if (Constants.GENERATE_TOGA_DATASET) {
//                    System.out.println("Generating ToGA records for Eval inputs...");
//                    if (!Utils.generateToGAInput(parser, getEvalRecords()))
//                        System.err.println("ToGA dataset generation failed...");
//                }
//            }
//
//        }
//        ////////////////////
//
//        return cost;
//    }

//    private static void randomlyChooseMethodsAndConstructNewMethodsCorpus(int targetMethodsSize, String methodsCorpusFileName) {
//        Gson gson = new Gson();
//        List<Repository> _repositories = new LinkedList<>();
//
//        try {
//            FileReader fileReader = new FileReader(methodsCorpusFileName);
//            BufferedReader reader = new BufferedReader(fileReader);
//
//            Repository[] repositories = gson.fromJson(reader, Repository[].class);
//            List<Pair<Method, Pair<RepoFile, Repository>>> allMethods = new LinkedList<>();
//            for (Repository repository : repositories)
//                for (RepoFile repoFile : repository.getFiles())
//                    for (Method method : repoFile.getMethods())
//                        if (hasAssertions(method))
//                            allMethods.add(Pair.of(method, Pair.of(repoFile, repository)));
//
//            if (targetMethodsSize < allMethods.size()) {
//                Collections.shuffle(allMethods);
//                List<Pair<Method, Pair<RepoFile, Repository>>> randomlyChosenMethods = allMethods.subList(0, targetMethodsSize);
//
//
//                for (Pair<Method, Pair<RepoFile, Repository>> pair : randomlyChosenMethods) {
//                    Method method = pair.getFirst();
//                    RepoFile repoFile = pair.getSecond().getFirst();
//                    Repository repo = pair.getSecond().getSecond();
//
//                    if (repoExistsInRepositories(repo, _repositories)) {
//                        Repository existingRepo = extractRepoOf(repo, _repositories);
//
//                        if (repoFileExistsInRepoFilesOf(repoFile, existingRepo)) {
//                            RepoFile existingRepoFile = extractRepoFileOf(repoFile, existingRepo);
//                            existingRepoFile.getMethods().add(method);
//                        } else {
//                            List<Method> methodsListToAdd = new LinkedList<>();
//                            methodsListToAdd.add(method);
//
//                            RepoFile repoFileToAdd = new RepoFile(repoFile.getName(), repoFile.getPath(),
//                                    repoFile.getUrl(), methodsListToAdd);
//
//                            existingRepo.addFile(repoFileToAdd);
//
//                        }
//                    } else {
//                        Repository toAddRepo = new Repository();
//                        toAddRepo.setName(repo.getName());
//                        toAddRepo.setPath(repo.getPath());
//                        toAddRepo.setUrl(repo.getUrl());
//
//                        List<Method> methodsListToAdd = new LinkedList<>();
//                        methodsListToAdd.add(method);
//
//                        RepoFile repoFileToAdd = new RepoFile(repoFile.getName(), repoFile.getPath(),
//                                repoFile.getUrl(), methodsListToAdd);
//
//                        toAddRepo.addFile(repoFileToAdd);
//                        _repositories.add(toAddRepo);
//                    }
//                }
//
//                String json = gson.toJson(_repositories);
//
//
//                try (FileWriter writer = new FileWriter(methodsCorpusFileName)) {
//                    writer.write(json);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    private static RepoFile extractRepoFileOf(RepoFile repoFile, Repository r) {
//        for (RepoFile existingRepoFile : r.getFiles())
//            if (existingRepoFile.getUrl().equals(repoFile.getUrl()) &&
//                    existingRepoFile.getPath().equals(repoFile.getPath()) &&
//                    existingRepoFile.getName().equals(repoFile.getName()))
//                return existingRepoFile;
//
//        return null;
//    }
//
//    private static boolean repoFileExistsInRepoFilesOf(RepoFile repoFile, Repository existingRepo) {
//        for (RepoFile existingRepoFile : existingRepo.getFiles())
//            if (existingRepoFile.getUrl().equals(repoFile.getUrl()) &&
//                    existingRepoFile.getPath().equals(repoFile.getPath()) &&
//                    existingRepoFile.getName().equals(repoFile.getName()))
//                return true;
//        return false;
//
//    }
//
//    private static Repository extractRepoOf(Repository repo, List<Repository> repositories) {
//        for (Repository _repo : repositories)
//            if (_repo.getName().equals(repo.getName()) && _repo.getPath().equals(repo.getPath()) &&
//                    _repo.getUrl().equals(repo.getUrl()))
//                return _repo;
//        return null;
//    }
//
//    private static boolean repoExistsInRepositories(Repository repo, List<Repository> repositories) {
//        for (Repository _repo : repositories)
//            if (_repo.getName().equals(repo.getName()) && _repo.getPath().equals(repo.getPath()) &&
//                    _repo.getUrl().equals(repo.getUrl()))
//                return true;
//
//        return false;
//    }
}

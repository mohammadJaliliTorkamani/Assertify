package org.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;

import java.io.File;
import java.util.*;

public class Parser {
    private final List<Flag> flags;
    private Extractor extractor;
    private String originalMethod;
    private String prunedMethod;
    private MethodDeclaration methodDeclaration;

    public Parser(Flag... flags) {
        this.flags = Arrays.asList(flags);
    }

    public void initialize(InputRecord record, boolean backupVersion) throws Exception {
        this.extractor = new Extractor(backupVersion ? record.getAlternativePath() : record.getPath());
        this.methodDeclaration = extractMethod(record.getName(), record.getClassName(), record.getSignature());
        if (methodDeclaration != null) {
            this.originalMethod = extractor.printMethod(methodDeclaration);
            this.prunedMethod = extractor.printMethod(methodDeclaration, containsFlag(Flag.NO_METHOD_COMMENTS),
                    containsFlag(Flag.NO_METHOD_ASSERTIONS), containsFlag(Flag.NO_METHOD_JAVADOCS));
        } else {
            System.err.println("No method declaration could be extracted");
        }
    }

    public MethodDeclaration getCommentLessAndJavadocLessMethodDeclaration(MethodDeclaration methodDeclaration) throws Exception {
//        final ParserConfiguration parserConfiguration = new ParserConfiguration();
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

//        javaParser.parse(sourceFile).getResult().get().;

        return StaticJavaParser.parseMethodDeclaration(extractor.printMethod(methodDeclaration, true, false, true));
    }

    public MethodDeclaration getOriginalMethodDeclaration() {
        return methodDeclaration;
    }

    public List<String> extractAssertions(String className, String methodName, String signature) throws Exception {
        return new ArrayList<String>(this.extractor.extractAssertions(className, methodName, signature));
    }

    public String getOriginalMethod() {
        return originalMethod;
    }

    public String getPrunedMethod() {
        return prunedMethod;
    }

    public boolean containsFlag(Flag flag) {
        return flags.contains(flag);
    }

    private MethodDeclaration extractMethod(String name, String className, String signature) throws Exception {
        List<MethodDeclaration> methodDeclarations = extractor.extractMethods(className);
        return Extractor.getMethodWithName(methodDeclarations, name, signature);
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder(String.format("Original Method:%n" +
                        "%s" +
                        "%n%n" +
                        "Pruned Method:%n" +
                        "%s" +
                        "%n%n" +
                        "Flags:" +
                        "%n",
                originalMethod,
                prunedMethod));
        for (Flag flag : flags)
            output.append(flag.toString());
        return output.toString();
    }

    public List<MethodCallExpr> extractMethodCallExpr(MethodDeclaration methodDeclaration, InputRecord record) throws Exception {
        return extractor.extractMethodCallExpr(methodDeclaration, record);
    }

    public Map<MethodCallExpr, MethodDeclaration> extractInvokedSourceCodes(InputRecord record) throws Exception {
        List<MethodCallExpr> callExpressions = extractMethodCallExpr(StaticJavaParser.parseMethodDeclaration(getPrunedMethod()), record);
        Map<MethodCallExpr, MethodDeclaration> map = new HashMap<>();
        for (MethodCallExpr callExpr : callExpressions) {
            try {
                ResolvedMethodDeclaration resolvedMethodDeclaration = callExpr.resolve();
                if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) { //if defined inside the project
                    if (!((JavaParserMethodDeclaration) resolvedMethodDeclaration).getWrappedNode().getBody().isEmpty()) {
                        map.put(callExpr, ((JavaParserMethodDeclaration) resolvedMethodDeclaration).getWrappedNode());
                    } else {//in this state, the method has been defined in the interface. so we need to search all the java files to find its implementation
                        MethodDeclaration implementedMethodDeclaration = findImplementedMethodDeclaration(record, resolvedMethodDeclaration);
                        if (implementedMethodDeclaration != null) {
                            map.put(callExpr, implementedMethodDeclaration);
                        }
                    }
                } else
                    map.put(callExpr, null); // declaration source code was not found  in the project
            } catch (Exception e) {
                map.put(callExpr, null);
            }
        }
        return map;
    }

    private MethodDeclaration findImplementedMethodDeclaration(InputRecord record, ResolvedMethodDeclaration bodyLessResolvedMethodDeclaration) {
        Queue<File> queue = new ArrayDeque<>();
        File rootDirectory = new File(record.getRepoPath(true));
        if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return null;
        }

        queue.add(rootDirectory);

        while (!queue.isEmpty()) {
            File currentDirectory = queue.poll();
            File[] files = currentDirectory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".java")) {
                        try (CompilationUnitWrapper cu = new CompilationUnitWrapper(file.getPath())) {
                            for (MethodDeclaration methodDeclaration1 : cu.getCompilationUnit().findAll(MethodDeclaration.class)) {
                                if (methodDeclaration1.getBody().isPresent())
                                    if (methodDeclaration1.getNameAsString().equals(bodyLessResolvedMethodDeclaration.getName()) &&
                                            methodDeclaration1.getParameters().size() == bodyLessResolvedMethodDeclaration.getNumberOfParams()) {
                                        boolean allEqual = true;
                                        for (int i = 0; i < bodyLessResolvedMethodDeclaration.getNumberOfParams(); i++) {
                                            String[] type = bodyLessResolvedMethodDeclaration.getParam(i).getType().describe().split("\\.");
                                            if (!type[type.length - 1].equals(methodDeclaration1.getParameter(i).getType().asString())) {
                                                allEqual = false;
                                                break;
                                            }
                                        }
                                        if (allEqual) {
                                            return methodDeclaration1;
                                        }
                                    }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (file.isDirectory()) {
                        queue.add(file); // Add subdirectories to the queue
                    }
                }
            }
        }
        return null;
    }

    public enum Flag {
        NO_METHOD_ASSERTIONS, NO_METHOD_COMMENTS, NO_METHOD_JAVADOCS
    }
}

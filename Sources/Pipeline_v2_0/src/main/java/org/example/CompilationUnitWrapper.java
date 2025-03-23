package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CompilationUnitWrapper implements AutoCloseable {

    private final String javaPath;
    private FileInputStream fis;

    public CompilationUnitWrapper(String javaPath) {
        this.javaPath = javaPath;
    }

    private static String extractRepoPath(String javaFilePath) {
        //older version
//        javaFilePath = javaFilePath.replace("\\", "/");
//        Pattern pattern = Pattern.compile("src/.*/java(?=/)");
//        Matcher matcher = pattern.matcher(javaFilePath);
//        if (!matcher.find())
//            return javaFilePath;
//
//        String foundStr = matcher.group();
//        int index = javaFilePath.indexOf(foundStr);
//        return javaFilePath.substring(0, index + foundStr.length()).replace("src/test/java", "src/main/java");//replace was added by me to cover finding unit test

        //newer version
        javaFilePath = javaFilePath.replace("\\", "/");
        int repositoriesIndex = Arrays.asList(javaFilePath.split("/")).indexOf("repositories");
        int endIndex = repositoriesIndex + 5;
        return String.join("/", Arrays.copyOfRange(javaFilePath.split("/"), 0, endIndex)).replace("src/test/java", "src/main/java");
    }

    public CompilationUnit getCompilationUnit() throws Exception {
        try {
            this.fis = new FileInputStream(javaPath);
//        TypeSolver typeSolver = new ReflectionTypeSolver();
//
//        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
//        JavaParser javaParser = new JavaParser();
//        javaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
//
//
//        return javaParser
//                .parse(fis, StandardCharsets.UTF_8)
//                .getResult()
//                .orElse(null);

            String repoPath = extractRepoPath(javaPath);
            TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
            TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(repoPath));

            CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
            combinedSolver.add(reflectionTypeSolver);
            combinedSolver.add(javaParserTypeSolver);

            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);

//        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

            JavaParser javaParser = new JavaParser();
            javaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
            StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
            return javaParser
                    .parse(fis, StandardCharsets.UTF_8)
                    .getResult()
                    .orElse(null);
//        return StaticJavaParser.parse(fis, StandardCharsets.UTF_8);
        } finally {
            close();
        }
    }

    @Override
    public void close() throws Exception {
        if (this.fis != null)
            this.fis.close();
    }
}

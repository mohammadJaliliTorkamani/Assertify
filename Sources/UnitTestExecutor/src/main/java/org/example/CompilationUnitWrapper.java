package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;

public class CompilationUnitWrapper implements AutoCloseable {

    private final String javaPath;
    private FileInputStream fis;

    public CompilationUnitWrapper(String javaPath) {
        this.javaPath = javaPath;
    }

    public CompilationUnit getCompilationUnit() throws FileNotFoundException {
        this.fis = new FileInputStream(javaPath);
        TypeSolver typeSolver = new ReflectionTypeSolver();

        // Configure the JavaSymbolSolver with the TypeSolver
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        JavaParser javaParser = new JavaParser();
        javaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        return javaParser
                .parse(fis, StandardCharsets.UTF_8)
                .getResult()
                .orElse(null);
    }

    @Override
    public void close() throws Exception {
        if (this.fis != null)
            this.fis.close();
    }
}

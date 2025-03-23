package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;

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
        return new JavaParser(new ParserConfiguration())
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

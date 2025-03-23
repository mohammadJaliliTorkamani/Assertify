package org.example;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;

import java.io.IOException;
import java.util.*;

/**
 * This class is responsible for extracting java items (statement-level granularity) from a given .java file
 */
public class Extractor {
    private final String javaPath;

    public Extractor(String javaPath) {
        this.javaPath = javaPath;
    }

    public static MethodDeclaration getMethodWithName(List<MethodDeclaration> methodDeclarations, String methodName, String signature) {
        for (MethodDeclaration methodDeclaration : methodDeclarations)
            if (
                    methodDeclaration.getNameAsString().equals(methodName) &&
                            methodDeclaration.getDeclarationAsString().equals(signature)
            )
                return methodDeclaration;
        return null;
    }

    /**
     * extracts the javadoc for the given method
     *
     * @param methodName name of the method of extract javadoc from
     * @return javadoc of the method
     */
    public Javadoc extractJavadoc(String methodName) throws Exception {
        try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(javaPath)) {
            Optional<MethodDeclaration> methodOptional = cuw.getCompilationUnit().findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals(methodName));
            if (methodOptional.isPresent()) return methodOptional.get().getJavadoc().orElse(null);
        }
        return null;
    }
}

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
     * extracts methods from the given java file (constructor)
     *
     * @return list of all methods available in the file
     * @throws IOException if file cannot be opened
     */
    public List<MethodDeclaration> extractMethods(String className) throws Exception {
        List<MethodDeclaration> methods = new ArrayList<>();
        try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(javaPath)) {
            new MethodVisitor(className).visit(cuw.getCompilationUnit(), methods);
            return methods;
        }
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

    /**
     * extracts all the comments in a method
     *
     * @param methodName method name to extract comments from
     * @return list of omments inside the method
     */
    public List<Comment> extractComments(String methodName) throws Exception {
        try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(javaPath)) {
            List<Comment> comments = new LinkedList<>(cuw.getCompilationUnit().findAll(MethodDeclaration.class, m -> m.getNameAsString().equals(methodName)).get(0).getAllContainedComments());
            comments.forEach(comment -> comment.setContent(comment.getContent().trim()));
            return comments;
        }
    }

    /**
     * excludes comments and assertions from a method
     *
     * @param methodDeclaration method from which information is going to be excluded
     * @param excludeComments   whether comments are excluded
     * @param excludeAssertions whether assertions are excluded
     * @return method as string
     */
    public String printMethod(MethodDeclaration methodDeclaration, boolean excludeComments, boolean excludeAssertions, boolean excludeJavadoc) throws Exception {
        MethodDeclaration _methodDeclaration = methodDeclaration.clone();

        if (excludeAssertions) {
            _methodDeclaration.accept(new AssertionRemovalVisitor(),null);
//            try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(javaPath)) {
//                visitor.visit(cuw.getCompilationUnit(), null);
//                System.out.println("WWW:"+cuw.getCompilationUnit());
//            }
        }

        PrettyPrinterConfiguration configuration = new PrettyPrinterConfiguration();
        /*
         * seems to be corrupted in case of setPrintComments:false,setPrintJavadoc:true, so we come up
         * with manually printing/not printing javadocs
         */
        configuration.setPrintJavadoc(false);
        configuration.setPrintComments(!excludeComments);

        String javadoc = "";
        if (!excludeJavadoc) {
            Javadoc _javadoc = extractJavadoc(_methodDeclaration.getNameAsString());
            if (_javadoc != null) javadoc = _javadoc.toComment().toString();
        }

        PrettyPrinter prettyPrinter = new PrettyPrinter(configuration);
        return javadoc + prettyPrinter.print(_methodDeclaration);
    }

    public String printMethod(MethodDeclaration methodDeclaration) throws Exception {
        return printMethod(methodDeclaration, false, false, false);
    }

    /**
     * extract assertions from a method
     *
     * @param methodName method to extract assertions from
     * @return set of all assertions
     */
    public Set<String> extractAssertions(String className, String methodName, String signature) throws Exception {
        Set<String> assertions = new HashSet<>();
        try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(javaPath)) {
            new AssertionVisitor(methodName, className, signature).visit(cuw.getCompilationUnit(), assertions);
        }

        return assertions;
    }

    public List<MethodCallExpr> extractMethodCallExpr(MethodDeclaration methodDeclaration, InputRecord record) throws Exception {
        List<MethodCallExpr> methodCallExpressions = new ArrayList<>();
        try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(javaPath)) {
            new MethodCallExprVisitor(methodDeclaration.getDeclarationAsString(), record).visit(cuw.getCompilationUnit(), methodCallExpressions);
        }
        return methodCallExpressions;
    }
}

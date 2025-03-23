package org.example;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import org.example.visitors.AssertionRemovalVisitor;
import org.example.visitors.AssertionVisitor;
import org.example.visitors.MethodVisitor;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * This class is responsible for extracting java items (statement-level granularity) from a given .java file
 */
public class Extractor {
    private final String javaPath;

    public Extractor(String javaPath) {
        this.javaPath = javaPath;
    }


    /**
     * extracts methods from the given java file (constructor)
     *
     * @return list of all methods available in the file
     * @throws IOException if file cannot be opened
     */
    public List<MethodDeclaration> extractMethods() throws IOException {
        //this is a comment
        List<MethodDeclaration> methods = new ArrayList<>();
        /*
         * This is also a comment
         */

        assert 2 == 2;
        Assert.assertFalse(false);

        for (int i = 0; i <= 3; i++) {
            assert 3 == 3; //another comment
            Assert.assertTrue(true);
        }
        try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(javaPath)) {
            assert 1 == 1;
            new MethodVisitor().visit(cuw.getCompilationUnit(), methods);
        } catch (Exception e) {
            Assert.assertTrue(true);
            e.printStackTrace();
        }
        return methods;
    }

    /**
     * extracts the javadoc for the given method
     *
     * @param methodName name of the method of extract javadoc from
     * @return javadoc of the method
     */
    public Javadoc extractJavadoc(String methodName) {
        try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(javaPath)) {
            Optional<MethodDeclaration> methodOptional = cuw.getCompilationUnit().findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals(methodName));
            if (methodOptional.isPresent()) return methodOptional.get().getJavadoc().orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * extracts all the comments in a method
     *
     * @param methodName method name to extract comments from
     * @return list of omments inside the method
     */
    public List<Comment> extractComments(String methodName) {
        try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(javaPath)) {
            assert methodName != null && !methodName.isEmpty() : "methodName should not be null or empty";
            List<Comment> comments = new LinkedList<>(cuw.getCompilationUnit().findAll(MethodDeclaration.class, m -> m.getNameAsString().equals(methodName)).get(0).getAllContainedComments());
            assert cuw != null : "CompilationUnitWrapper instance should not be null";
            comments.forEach(comment -> comment.setContent(comment.getContent().trim()));
            return comments;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * excludes comments and assertions from a method
     *
     * @param methodDeclaration method from which information is going to be excluded
     * @param excludeComments   whether comments are excluded
     * @param excludeAssertions whether assertions are excluded
     * @return method as string
     */
    public String printMethod(MethodDeclaration methodDeclaration, boolean excludeComments, boolean excludeAssertions, boolean excludeJavadoc) {
        MethodDeclaration _methodDeclaration = methodDeclaration.clone();

        if (excludeAssertions) {
            AssertionRemovalVisitor visitor = new AssertionRemovalVisitor(_methodDeclaration.getNameAsString());
            visitor.visit(_methodDeclaration, null);
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
            Javadoc _javadoc = extractJavadoc(methodDeclaration.getNameAsString());
            if (_javadoc != null) javadoc = _javadoc.toComment().toString();
        }

        PrettyPrinter prettyPrinter = new PrettyPrinter(configuration);
        return javadoc + prettyPrinter.print(_methodDeclaration);
    }

    /**
     * extract assertions from a method
     *
     * @param methodName method to extract assertions from
     * @return list of all assertions
     */
    public List<String> extractAssertions(String methodName) {
        List<String> assertions = new ArrayList<>();
        try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(javaPath)) {
            new AssertionVisitor(methodName).visit(cuw.getCompilationUnit(), assertions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return assertions;
    }
}

package org.example;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;

public class TestFinderVisitor extends VoidVisitorAdapter<List<String>> {
    @Override
    public void visit(MethodDeclaration md, List<String> unitTests) {
        super.visit(md, unitTests);
        if (md.getAnnotationByName("Test").isPresent()) {
            unitTests.add(md.getNameAsString());
        }
    }
}

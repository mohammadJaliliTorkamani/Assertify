package org.example;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;

public class MethodCallExprVisitor extends VoidVisitorAdapter<List<MethodCallExpr>> {
    private final String declarationAsString;
    private final InputRecord record;

    public MethodCallExprVisitor(String declarationAsString, InputRecord record) {
        this.declarationAsString = declarationAsString;
        this.record = record;
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, List<MethodCallExpr> list) {
        MethodDeclaration enclosingMethodName = methodCallExpr.findAncestor(MethodDeclaration.class).orElse(null);
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = methodCallExpr.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);

        if (classOrInterfaceDeclaration != null &&
                !classOrInterfaceDeclaration.isInterface() &&
                classOrInterfaceDeclaration.getNameAsString().equals(record.getClassName()) &&
                enclosingMethodName != null &&
                enclosingMethodName.getDeclarationAsString().equals(declarationAsString) &&
                !methodCallExpr.findAncestor(AssertStmt.class).isPresent()) {
            list.add(methodCallExpr);
        }
        super.visit(methodCallExpr, list);
    }
}

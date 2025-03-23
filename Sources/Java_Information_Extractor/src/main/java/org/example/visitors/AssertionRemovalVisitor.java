package org.example.visitors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

/**
 * This class is responsible for removing assertions (standard java assertions or junit assertions)
 */
public class AssertionRemovalVisitor extends ModifierVisitor<Void> {
    private final String methodName;

    public AssertionRemovalVisitor(String methodName) {
        this.methodName = methodName;
    }

    /**
     * visits java assertions
     *
     * @param assertStmt never used. we are sure that it is an assertion
     * @param arg        never being used
     * @return null to discard(remove) assertion
     */
    @Override
    public Visitable visit(AssertStmt assertStmt, Void arg) {
        String enclosingMethodName = assertStmt.findAncestor(MethodDeclaration.class).get().getNameAsString();
        if (enclosingMethodName.equals(methodName))
            return null;
        return super.visit(assertStmt, arg);
    }


    /**
     * visits junit assertions
     *
     * @param methodCallExpr to check junit invocation
     * @param arg            never being used
     * @return whether saw this node
     */
    @Override
    public Visitable visit(MethodCallExpr methodCallExpr, Void arg) {
        String enclosingMethodName = methodCallExpr.findAncestor(MethodDeclaration.class).get().getNameAsString();
        String methodCallName = methodCallExpr.getName().getIdentifier();
        if (enclosingMethodName.equals(methodName) && methodCallName.startsWith("assert"))
            return null;

        return super.visit(methodCallExpr, arg);
    }
}
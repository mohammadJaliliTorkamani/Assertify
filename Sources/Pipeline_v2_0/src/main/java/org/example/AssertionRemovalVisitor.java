package org.example;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.github.javaparser.ast.stmt.Statement;

/**
 * This class is responsible for removing assertions (standard java assertions)
 */
public class AssertionRemovalVisitor extends ModifierVisitor<Void> {

    /**
     * visits java assertions
     *
     * @param node never used. we are sure that it is an assertion
     * @param arg        never being used
     * @return null to discard(remove) assertion
     */
    @Override
    public Visitable visit(AssertStmt node, Void arg) {
        if(node.toString().contains("assert ")) {
            return null;
        }
        return super.visit(node, arg);
    }
}
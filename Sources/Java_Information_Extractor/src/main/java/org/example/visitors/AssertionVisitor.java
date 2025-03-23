package org.example.visitors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;

/**
 * This class is responsible for visiting every assertion of a given method and adding it into the given list
 */
public class AssertionVisitor extends VoidVisitorAdapter<List<String>> {
    /**
     * method name to find assertions from
     */
    private final String methodName;

    public AssertionVisitor(String methodName) {
        this.methodName = methodName;
    }

    /**
     * adds standard java assertions into the list if it resides in the given function
     *
     * @param assertStmt statement to process
     * @param arg        list to add into
     */
    @Override
    public void visit(AssertStmt assertStmt, List<String> arg) {
        MethodDeclaration enclosingMethodName = assertStmt.findAncestor(MethodDeclaration.class).orElse(null);
        if (enclosingMethodName != null && enclosingMethodName.getNameAsString().equals(methodName))
            for (String strLine : assertStmt.toString().split("\n")) {
                if (strLine.startsWith("assert "))
                    arg.add(strLine);
            }

        super.visit(assertStmt, arg);
    }
}

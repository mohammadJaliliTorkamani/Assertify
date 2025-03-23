package org.example;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is responsible for visiting every assertion of a given method and adding it into the given list
 */
public class AssertionVisitor extends VoidVisitorAdapter<Set<String>> {
    /**
     * method name to find assertions from
     */
    private final String methodName;
    private final String className;
    private final String signature;

    public AssertionVisitor(String methodName, String className, String signature) {
        this.methodName = methodName;
        this.className = className;
        this.signature = signature;
    }

    /**
     * adds standard java assertions into the list if it resides in the given function
     *
     * @param assertStmt statement to process
     * @param arg        list to add into
     */
    @Override
    public void visit(AssertStmt assertStmt, Set<String> arg) {
        if(assertStmt.toString().strip().contains("assert ")) {
            MethodDeclaration enclosingMethod = findOutermostEnclosingMethodDeclaration(assertStmt, methodName, signature, className);
            ClassOrInterfaceDeclaration enclosingClass = assertStmt.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
            if (enclosingMethod != null && enclosingClass != null && !enclosingClass.isInterface()
                    && enclosingMethod.getNameAsString().equals(methodName)
                    && enclosingMethod.getDeclarationAsString().equals(signature)
                    && enclosingClass.getNameAsString().equals(className)
            ) {
                for (String strLine : assertStmt.toString().split("\n"))
                    if (strLine.strip().contains("assert "))
                        arg.add(strLine);
            }
        }
        super.visit(assertStmt, arg);
    }

    private MethodDeclaration findOutermostEnclosingMethodDeclaration(AssertStmt node, String methodName, String signature, String className) {
        List<MethodDeclaration> methods = node
                .findRootNode()
                .findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .filter(classOrInterfaceDeclaration -> {
                    return !classOrInterfaceDeclaration.isInterface();
                })
                .filter(classOrInterfaceDeclaration -> classOrInterfaceDeclaration.getNameAsString().equals(className))
                .map(classOrInterfaceDeclaration -> {
                    List<MethodDeclaration> classMethods = classOrInterfaceDeclaration.getMethods();
                    for (MethodDeclaration method : classMethods) {
                        if(method.getDeclarationAsString().equals(signature) &&
                                method.getNameAsString().equals(methodName) &&
                                method.getBody().get().toString().contains(node.toString()))
                            return method;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!methods.isEmpty())
            return methods.get(0);
        return null;
    }
}

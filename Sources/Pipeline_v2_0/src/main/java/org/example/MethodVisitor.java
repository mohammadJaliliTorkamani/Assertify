package org.example;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;

/**
 * visits every method and adds it into the list of MethodDeclarations
 */
public class MethodVisitor extends VoidVisitorAdapter<List<MethodDeclaration>> {
    private final String className;
    private String methodName = null;
    private String signature;

    public MethodVisitor(String className) {
        this.className = className;
    }

    public MethodVisitor(String className, String methodName, String signature) {
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
    }

    /**
     * adds the method into the list
     *
     * @param classOrInterfaceDeclaration declaration to be inspected
     * @param methods                     list to add into
     */
    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, List<MethodDeclaration> methods) {
        if (classOrInterfaceDeclaration.isInterface()) {
            // Skip interfaces
            return;
        }

        if (classOrInterfaceDeclaration.getNameAsString().equals(className)) {
            if (methodName == null) {
                methods.addAll(classOrInterfaceDeclaration.getMethods());
            } else {
                MethodDeclaration methodDeclaration = Extractor
                        .getMethodWithName(classOrInterfaceDeclaration.getMethods(), methodName, signature);
                if (methods != null)
                    methods.add(methodDeclaration);
            }

        }
        super.visit(classOrInterfaceDeclaration, methods);
    }
}
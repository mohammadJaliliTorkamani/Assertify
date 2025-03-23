package org.example.visitors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;

/**
 * visits every method and adds it into the list of MethodDeclarations
 */
public class MethodVisitor extends VoidVisitorAdapter<List<MethodDeclaration>> {


    //older version
//    @Override
//    public void visit(MethodDeclaration method, List<MethodDeclaration> methods) {
//        super.visit(method, methods);
//        methods.add(method);
//    }


    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, List<MethodDeclaration> methods) {
        super.visit(classOrInterfaceDeclaration, methods);
        if (classOrInterfaceDeclaration.isInterface()) {
            // Skip interfaces
            return;
        }
        methods.addAll(classOrInterfaceDeclaration.getMethods());

    }
}
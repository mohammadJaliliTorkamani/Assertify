package org.example;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MethodVisitor2 extends VoidVisitorAdapter<Void> {
    private final List<MethodDeclaration> enclosingMethodsForMethodCallExpr = new LinkedList<>();
    private final MethodDeclaration referenceMethod;
    private final CompilationUnitWrapper testCUW;
    private final String className;//new
    private final ResolvedMethodDeclaration resolvedMethodDeclaration;

    public MethodVisitor2(MethodDeclaration referenceMethod, String className, ResolvedMethodDeclaration resolvedMethodDeclaration, CompilationUnitWrapper testCUW) {
        this.referenceMethod = referenceMethod;
        this.testCUW = testCUW;
        this.className = className;//new
        this.resolvedMethodDeclaration = resolvedMethodDeclaration;
    }

//    @Override
//    public void visit(MethodCallExpr methodCallExpr, Void arg) {
//        if (resolvedReferenceMethodDeclaration != null && methodCallExpr.toString().contains(resolvedReferenceMethodDeclaration.getName())) {
//            super.visit(methodCallExpr, arg);
//            if (areEqualReferences(methodCallExpr)) {
//                MethodDeclaration enclosingMethod = methodCallExpr.findAncestor(MethodDeclaration.class).orElse(null);
//                enclosingMethodsForMethodCallExpr.add(enclosingMethod);
//            }
//        }
//    }

//    @Override
//    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Void arg) {
//        if (classOrInterfaceDeclaration.isInterface() || resolvedReferenceMethodDeclaration == null) {
//            return;
//        }
//
//        classOrInterfaceDeclaration.getMethods().forEach(methodDeclaration -> {
//            methodDeclaration.getBody().ifPresent(blockStmt -> {
//                if (blockStmt.toString().contains(resolvedReferenceMethodDeclaration.getName()+"(")) {
//                    for (MethodCallExpr methodCallExpr : methodDeclaration.findAll(MethodCallExpr.class)) {
//                        if (areEqualReferences(methodCallExpr)) {
//                            enclosingMethodsForMethodCallExpr.add(methodDeclaration);
//                            break;
//                        }
//                    }
//                }
//            });
//        });
//    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        if (methodDeclaration.getAnnotationByName("Test").isPresent()) {
            methodDeclaration.getBody().ifPresent(blockStmt -> {
                if (blockStmt.toString().contains(referenceMethod.getNameAsString() + "(")) {
                    if (methodDeclaration != null && methodDeclaration.findAll(MethodCallExpr.class).stream().anyMatch(methodCallExpr -> {
                        return areEqualReferences(methodCallExpr);
                    }))
                        enclosingMethodsForMethodCallExpr.add(methodDeclaration);
//                for (MethodCallExpr methodCallExpr : methodDeclaration.findAll(MethodCallExpr.class)) {
//                    if (areEqualReferences(methodCallExpr)) {
//                        enclosingMethodsForMethodCallExpr.add(methodDeclaration);
//                        break;
//                    }
//                }
                }
            });
        }
    }

    public List<MethodDeclaration> getEnclosingMethodsForMethodCallExpr() {
        return enclosingMethodsForMethodCallExpr;
    }

    private boolean areEqualReferences(MethodCallExpr methodCallExpr) {
        try {
            if (!methodCallExpr.getNameAsString().equals(referenceMethod.getNameAsString()))
                return false;

            if (methodCallExpr.getArguments().size() != referenceMethod.getParameters().size()) {
                return false;
            }


            String _arr2 = methodCallExpr.getScope().get().asNameExpr().resolve().getType().describe();
            String[] arr2 = _arr2.split("<")[0].split("\\."); //to filter generics
            return className.equals(arr2[arr2.length - 1]);
        } catch (Exception e) {
            return false;
        }
    }

    private String getLastItemOfDot(String str) {
        if (str == null || str.isBlank() || !str.contains("."))
            return str;
        String[] array = str.split("\\.");
        return array[array.length - 1];
    }

    private String extractMethodSignature(MethodCallExpr methodCallExpr) {
        try {
            // Extract method name
            String methodName = methodCallExpr.getName().getIdentifier();

            // Extract method arguments
            JavaParserFacade parserFacade = JavaParserFacade.get(new ReflectionTypeSolver());
            List<ResolvedType> argumentTypes = methodCallExpr.getArguments().stream()
                    .map(parserFacade::getType)
                    .collect(Collectors.toList());

            // Build the method signature
            StringBuilder signatureBuilder = new StringBuilder();
            signatureBuilder.append(methodName).append("(");
            boolean firstArg = true;
            for (ResolvedType argumentType : argumentTypes) {
                if (!firstArg) {
                    signatureBuilder.append(", ");
                }

                String[] argType = argumentType.describe().split("\\.");
                signatureBuilder.append(argType[argType.length - 1]);
                firstArg = false;
            }
            signatureBuilder.append(")");

            return signatureBuilder.toString();
        } catch (Exception e) {
        }
        return null;
    }
}
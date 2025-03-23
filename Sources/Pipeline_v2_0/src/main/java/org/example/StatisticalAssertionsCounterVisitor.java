package org.example;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class StatisticalAssertionsCounterVisitor extends VoidVisitorAdapter<Void> {
    /**
     * holds the information about the local variables (each has its nam, type, method signature and className).
     * in the associated file
     */
    private final Set<VariableInfo> variables = new HashSet<>();
    /**
     * holds the information of the methods in the associated file.
     */
    private final Set<String> methods = new HashSet<>();
    /**
     * hold the information of the classes in the associated file.
     */
    private final Set<String> classes = new HashSet<>();
    /**
     * holds the classNames in the associated file, which hold assertion(s).
     */
    private final Set<String> assertionContainingClasses = new HashSet<>();
    /**
     * holds the (methods+className) in the associated file, which hold assertion(s).
     */
    private final Set<String> assertionContainingMethods = new HashSet<>();
    /**
     * holds the internally defined methods signature in the associated file, which assertions invoke.
     */
    private final Set<String> methodsAssertionsWorkWith = new HashSet<>();
    /**
     * holds the internally defined class names in the associated file, which assertions work with.
     */
    private final Set<String> classesAssertionsWorkWith = new HashSet<>();
    /**
     * holds the local variables in the associated file, which assertions work with.
     */
    private final Set<VariableInfo> variablesAssertionsWorkWith = new HashSet<>();
    /**
     * counts the total number of lines that assertions occupy in the associated file.
     */
    private int assertionLineCount;
    /**
     * counts the total number of assertions (existing in methods and not constructor) in the associated file.
     */
    private int assertionCount;
    private String packageName = null;

    @Override
    public void visit(AssertStmt assertStmt, Void arg) {
        if (assertStmt.findAncestor(ConstructorDeclaration.class).isEmpty()) //because sometimes assertions may be present in constructors (which are not considered as method declarations)
        {
            assertionCount++;

            assertStmt.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                try {
                    ResolvedMethodDeclaration resolvedMethodDeclaration = methodCallExpr.resolve();
                    if (isInternalMethodCall(resolvedMethodDeclaration)) {
                        methodsAssertionsWorkWith.add(resolvedMethodDeclaration.getSignature());
                    }
                } catch (Exception e) {
                }

                methodCallExpr.getScope().ifPresent(scope -> {
                    String methodClassPackageName = scope.calculateResolvedType().describe();
                    if (methodClassPackageName.startsWith(packageName)) {
                        classesAssertionsWorkWith.add(methodClassPackageName);
                    }
                });
            });

            assertStmt.findAll(NameExpr.class).forEach(nameExpr -> {
                Optional<ClassOrInterfaceDeclaration> classOrInterfaceDeclarationOptional = nameExpr.findAncestor(ClassOrInterfaceDeclaration.class);
                Optional<MethodDeclaration> methodDeclarationOptional = nameExpr.findAncestor(MethodDeclaration.class);
                if (classOrInterfaceDeclarationOptional.isPresent() && !classOrInterfaceDeclarationOptional.get().isInterface() &&
                        methodDeclarationOptional.isPresent()) {
                    String variableName = nameExpr.getNameAsString();
                    String variableType = nameExpr.calculateResolvedType().describe();
                    String variableContainerClassName = classOrInterfaceDeclarationOptional.get().getNameAsString();
                    String variableContainerMethodSignature = methodDeclarationOptional.get().getSignature().toString();
                    VariableInfo variableInfo = new VariableInfo(variableName, variableType, variableContainerMethodSignature, variableContainerClassName);
                    variablesAssertionsWorkWith.add(variableInfo);
                    classesAssertionsWorkWith.add(variableType);
                }
            });

            if (assertStmt.getRange().isPresent()) {
                int numberOfOccupiedLines = assertStmt.getRange().get().end.line - assertStmt.getRange().get().begin.line + 1;
                assertionLineCount += (numberOfOccupiedLines);
            }
        }
        super.visit(assertStmt, arg);
    }

    private boolean isInternalMethodCall(ResolvedMethodDeclaration resolvedMethodDeclaration) {
        String _packageName = resolvedMethodDeclaration.getPackageName();
        return _packageName.startsWith(packageName);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Void arg) {
        extractPackageNameIfEmpty(classOrInterfaceDeclaration);
        if (!classOrInterfaceDeclaration.isInterface()) {
            classes.add(classOrInterfaceDeclaration.getNameAsString());

            classOrInterfaceDeclaration
                    .findAll(MethodDeclaration.class)
                    .stream()
                    .filter(methodDeclaration -> !methodDeclaration.findAll(AssertStmt.class).isEmpty())
                    .limit(1)
                    .forEach(methodDeclaration -> assertionContainingClasses.add(classOrInterfaceDeclaration.getNameAsString()));

            //we add it here because by overriding the methodDeclaration visit method, it does not detect properly
            classOrInterfaceDeclaration.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
                String method = classOrInterfaceDeclaration.getNameAsString() + methodDeclaration.getSignature().toString();
                methods.add(method);

                if (!methodDeclaration.findAll(AssertStmt.class).isEmpty())
                    assertionContainingMethods.add(method);

                //we add it here because by overriding the variableDeclarator visit method, it does not detect properly
                methodDeclaration.findAll(VariableDeclarator.class).forEach(variableDeclarator -> {
                    String variableName = variableDeclarator.getNameAsString();
                    String variableType = variableDeclarator.getTypeAsString();
                    String containerMethodSignature = methodDeclaration.getSignature().toString();
                    String containerClassName = classOrInterfaceDeclaration.getNameAsString();
                    VariableInfo variableInfo = new VariableInfo(variableName, variableType, containerMethodSignature, containerClassName);
                    variables.add(variableInfo);
                });

            });
        }
        super.visit(classOrInterfaceDeclaration, arg);
    }

    private void extractPackageNameIfEmpty(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        if (packageName != null && !packageName.isEmpty())
            return;

        packageName = classOrInterfaceDeclaration.resolve().getPackageName();
        String[] sections = packageName.split("\\.");
        packageName = String.join(".", Arrays.copyOfRange(sections, 0, Math.min(3, sections.length)));
    }

    public int getAssertionCount() {
        return assertionCount;
    }

    public int getAssertionLineCount() {
        return assertionLineCount;
    }

    public int getAssertionClassCount() {
        return assertionContainingClasses.size();
    }

    public int getAssertionMethodCount() {
        return assertionContainingMethods.size();
    }

    public int getMethodsCount() {
        return methods.size();
    }

    public int getClassesCount() {
        return classes.size();
    }

    public int getOneIfFileHasAnyAssertion() {
        return assertionCount > 0 ? 1 : 0;
    }

    public int getWorkingAssertionClassCount() {
        return classesAssertionsWorkWith.size();
    }

    public int getWorkingAssertionMethodsCount() {
        return methodsAssertionsWorkWith.size();
    }

    public int getVariablesCount() {
        return variables.size();
    }

    public int getWorkingAssertionVariablesCount() {
        return variablesAssertionsWorkWith.size();
    }
}
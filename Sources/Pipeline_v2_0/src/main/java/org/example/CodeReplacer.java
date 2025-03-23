package org.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.printer.configuration.Indentation;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.example.ComponentResponse.Status.OK;

public class CodeReplacer {
    public ComponentResponse replace(String className, String path, String response) {
        ComponentResponse componentResponse = new ComponentResponse();
        try {
            MethodDeclaration newMethodDeclaration = StaticJavaParser.parseMethodDeclaration(response);
            try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(path)) {
                CompilationUnit cu = cuw.getCompilationUnit();
                cu
                        .findAll(ClassOrInterfaceDeclaration.class)
                        .stream()
                        .filter(classOrInterfaceDeclaration -> !classOrInterfaceDeclaration.isInterface())
                        .filter(classOrInterfaceDeclaration -> classOrInterfaceDeclaration.getNameAsString().equals(className))
                        .limit(1)
                        .forEach(classOrInterfaceDeclaration ->
                                classOrInterfaceDeclaration
                                        .findAll(MethodDeclaration.class)
                                        .stream()
                                        .filter(method -> method.getDeclarationAsString().equals(newMethodDeclaration.getDeclarationAsString()))
                                        .findFirst()
                                        .ifPresent(methodDeclaration -> methodDeclaration.replace(newMethodDeclaration)));
                PrettyPrinterConfiguration configuration = new PrettyPrinterConfiguration();
                configuration.setIndentType(Indentation.IndentType.TABS);
                configuration.setIndentSize(1);

                return writeToFile(path, cu.toString(configuration));
            } catch (Exception e) {
                componentResponse.setCode(ComponentResponse.Status.EXCEPTION_OCCURRED);
                componentResponse.setMessage(e.getMessage());
                return componentResponse;
            }
        } catch (Exception e) {
            componentResponse.setCode(ComponentResponse.Status.EXCEPTION_OCCURRED);
            componentResponse.setMessage(e.getMessage());
            return componentResponse;
        }
    }

    private ComponentResponse writeToFile(String path, String content) throws Exception {
        ComponentResponse componentResponse = new ComponentResponse();
        // Clear the file content
        Files.write(Paths.get(path), new byte[0]);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, true))) {
            writer.write(content);
            componentResponse.setMessage(null);
            componentResponse.setCode(OK);
        }
        return componentResponse;
    }
}

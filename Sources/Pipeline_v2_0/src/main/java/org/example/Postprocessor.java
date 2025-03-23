//previous version just after statistics

//package org.example;
//
//import com.github.javaparser.StaticJavaParser;
//import com.github.javaparser.ast.NodeList;
//import com.github.javaparser.ast.body.MethodDeclaration;
//import com.github.javaparser.ast.comments.JavadocComment;
//import com.github.javaparser.ast.expr.AnnotationExpr;
//
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class Postprocessor {
//
//    /**
//     * @param content        the string to process
//     * @param annotations    the annotations list of the method (will be added after processing if exists)
//     * @param javadocComment the javadoc of the method (will be added after processing if exists)
//     *                       removes <JAVA></JAVA> tags and then process if and only if one java method signature found.
//     *                       Also, note that just because of post-processing does not mean that we don't have any parsing errors,
//     *                       because there could be parentheses, semicolon, multiple methods, or any other errors when we want to parse the output in the pipeline.
//     * @return processed content (or if we face error, the output of last the step in which we had no errors while post post-processing
//     */
//    public String apply(String content, NodeList<AnnotationExpr> annotations, JavadocComment javadocComment) {
//        if (content == null || content.isEmpty()) {
//            return null;
//        } else {
//            content = extractFromTAGs(content);
//            content = content
//                    .replace("\\n", "\n")
//                    .replace("\\t", "\t")
//                    .replace("\\r", "\r");
//
//            int n = countTotalNumberOfJavaSignatures(content);
//            if (n >= 1) {
//                int startIndex = getStartIndexOfJavaSignature(content);
//                if (content.contains("}")) {
//                    content = content.substring(startIndex, content.lastIndexOf("}") + 1);
//                    try {
//                        MethodDeclaration method = null;
//                        if (!annotations.isEmpty()) {
//                            method = StaticJavaParser.parseMethodDeclaration(content);
//                            for (AnnotationExpr annotationExpr : annotations) {
//                                method.addAnnotation(annotationExpr);
//                            }
//                        }
//
//                        if (javadocComment != null) {
//                            if (method == null)
//                                method = StaticJavaParser.parseMethodDeclaration(content);
//                            method.setJavadocComment(javadocComment);
//                        }
//                        return method == null ? content.trim() : method.toString().trim();
//                    } catch (Exception e) {
//                    }
//                }
//            }
//            return content.trim();
//        }
//    }
//
//    private int getStartIndexOfJavaSignature(String content) {
//        Pattern pattern = Pattern.compile("\\s*(public\\s|private\\s|protected\\s)?(\\w*<\\w*(?:,\\s*\\w*)*>)?\\s*(abstract\\s|default\\s|static\\s|synchronized\\s|final\\s|native\\s|transient\\s)*\\w+(\\.\\w+)*(<.*>)?(\\[.*\\])?\\s+\\w+\\([^)]*\\)");
//        Matcher matcher = pattern.matcher(content);
//        matcher.find();
//        return matcher.start();
//    }
//
//    private int countTotalNumberOfJavaSignatures(String content) {
//        Pattern pattern = Pattern.compile("\\s*(public\\s|private\\s|protected\\s)?(\\w*<\\w*(?:,\\s*\\w*)*>)?\\s*(abstract\\s|default\\s|static\\s|synchronized\\s|final\\s|native\\s|transient\\s)*\\w+(\\.\\w+)*(<.*>)?(\\[.*\\])?\\s+\\w+\\([^)]*\\)");
//        Matcher matcher = pattern.matcher(content);
//        return (int) matcher.results().count();
//    }
//
//    private String extractFromTAGs(String content) {
//        for (String tag : Constants.LLM_ASSISTANT_DELIMITER)
//            content = content.replace(tag, "");
//
//        Pattern pattern = Pattern.compile(Constants.LLM_ASSISTANT_DELIMITER[0] + "(.*?)" + Constants.LLM_ASSISTANT_DELIMITER[1]);
//        Matcher matcher = pattern.matcher(content);
//
//        if (matcher.find()) {
//            return matcher.group(1).trim();
//        } else {
//            return content.trim();
//        }
//    }
//}

package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Postprocessor {

    public static String addAssertions(String prunedMethod, String untaggedNumberedPairsOfAssertions) {
        Map<Integer, List<String>> assertionsMap = parseAssertions(untaggedNumberedPairsOfAssertions);
        String methodWithCurlyBraces = addCurlyBraces(prunedMethod);
        String toReturn = insertAssertions(assertionsMap, methodWithCurlyBraces);
        return toReturn;
    }

    public static String insertAssertions(Map<Integer, List<String>> assertionsMap, String prunedMethod) {
        String[] lines = prunedMethod.split("\n");
        StringBuilder modifiedMethod = new StringBuilder();
        for (int lineNumber = 1; lineNumber <= lines.length; lineNumber++) {
            if (assertionsMap.containsKey(lineNumber)) {
                for (String assertion : assertionsMap.get(lineNumber)) {
                    modifiedMethod.append(assertion).append("\n");
                }
            }
            modifiedMethod.append(lines[lineNumber - 1]).append("\n");
        }

        return modifiedMethod.toString();
    }

    public static String addCurlyBraces(String methodCode) {
        try {
            JavaParser parser = new JavaParser();
            CompilationUnit cu = parser.parse("class TempClass { " + methodCode + " }").getResult().orElse(null);

            if (cu == null) {
                throw new IllegalArgumentException("Failed to parse the method.");
            }

            MethodDeclaration method = null;
            for (TypeDeclaration<?> type : cu.getTypes()) {
                for (MethodDeclaration methodDeclaration : type.getMethods()) {
                    method = methodDeclaration;
                    method.accept(new AddCurlyBracesVisitor(), null);
                }
            }

            if (method == null) {
                throw new IllegalArgumentException("No method found in the provided code.");
            }
            PrettyPrinterConfiguration config = new PrettyPrinterConfiguration();
            config.setIndentSize(4);
            return method.toString(config);
        } catch (ParseProblemException e) {
            e.printStackTrace();
            return "Error parsing the method.";
        }
    }

    public static Map<Integer, List<String>> parseAssertions(String input) {
        Map<Integer, List<String>> resultMap = new HashMap<>();
        Pattern pattern = Pattern.compile("<JAVA>\\((\\d+),\\s*(.*?)\\)</JAVA>");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            Integer key = Integer.valueOf(matcher.group(1));
            String value = matcher.group(2);
            resultMap.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return resultMap;
    }

    public Pair<String, Exception> apply(String prunedMethod, String numberedPairOfAssertions, NodeList<AnnotationExpr> annotations, JavadocComment javadocComment) {
        String processedMethod = null;
        Exception exception = null;
        try {
            if (prunedMethod == null || prunedMethod.isEmpty()) {
                processedMethod = null;
            } else {
                String originalMethodWithAddedAssertions = addAssertions(prunedMethod, numberedPairOfAssertions);
                MethodDeclaration method = null;
                if (!annotations.isEmpty()) {
                    method = StaticJavaParser.parseMethodDeclaration(originalMethodWithAddedAssertions);
                    for (AnnotationExpr annotationExpr : annotations) {
                        if (method.getAnnotationByName(annotationExpr.getNameAsString()).isEmpty())
                            method.addAnnotation(annotationExpr);
                    }
                }

                if (javadocComment != null) {
                    if (method == null)
                        method = StaticJavaParser.parseMethodDeclaration(originalMethodWithAddedAssertions);
                    method.setJavadocComment(javadocComment);
                }

                PrettyPrinterConfiguration config = new PrettyPrinterConfiguration();
                config.setIndentSize(4);

                String value = method == null ? StaticJavaParser.parseMethodDeclaration(originalMethodWithAddedAssertions).toString(config).trim() : method.toString(config).trim();
                return Pair.of(purifyString(value), null);
            }
        } catch (Exception e) {
            exception = e;
        }
        return Pair.of(processedMethod, exception);
    }

    private String purifyString(String str) {
        return str.trim()
                .replaceAll("(?<!\\\\)\\n", "\n")
                .replaceAll("\\r", "\r")
                .replaceAll("\\t", "\t")
                .trim();
    }
}

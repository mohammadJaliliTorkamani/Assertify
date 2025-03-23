package org.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmbeddingsMiner {
    private EmbeddingsMiner() {
    }

    private static double calculateCosineSimilarity(List<Double> vector1, List<Double> vector2) {
        int dimensionality = Math.max(vector1.size(), vector2.size());
        if (dimensionality == 0) {
            throw new IllegalArgumentException("Both vectors are empty.");
        }

        double dotProduct = 0.0;
        double normVector1 = 0.0;
        double normVector2 = 0.0;

        for (int i = 0; i < dimensionality; i++) {
            double value1 = i < vector1.size() ? vector1.get(i) : 0.0;
            double value2 = i < vector2.size() ? vector2.get(i) : 0.0;

            dotProduct += value1 * value2;
            normVector1 += value1 * value1;
            normVector2 += value2 * value2;
        }

        normVector1 = Math.sqrt(normVector1);
        normVector2 = Math.sqrt(normVector2);

        if (normVector1 == 0 || normVector2 == 0) {
            return 0.0; // Return 0 to indicate no similarity if any vector is zero vector
        }

        return dotProduct / (normVector1 * normVector2);
    }

    /**
     * @param methodDeclaration            method to get similar methods
     * @param n                            number of similar methods
     * @param fslRecords                   FSL records
     * @param minimumAcceptableCosineValue minimum acceptable cosine value
     * @return Pair of  : Map of original method + same method with assertions    AND    its cost for computing embedding vectors
     */
    private static Pair<Map<String, String>, Double> getTopNSimilarMethods(LLM_Client client, Parser parser, MethodDeclaration methodDeclaration,
                                                                           int n, List<FSLRecord> fslRecords,
                                                                           double minimumAcceptableCosineValue) throws Exception {
        Map<String, String> map = new HashMap<>();
        double cost;
        if (methodDeclaration.getBody().isPresent()) {
            Pair<List<Double>, Double> methodEmbeddingsPair = client.calculateEmbeddings(methodDeclaration.getBody().get().toString().trim());
            cost = methodEmbeddingsPair.getSecond();
            fslRecords
                    .stream()
                    .filter(FSLRecord -> {
                        try { //because sometimes there could be some pattern (like instance of that Javaparser does not sopport it and throws exception)
                            StaticJavaParser.parseMethodDeclaration(FSLRecord.getMethodWithoutAssertions());
                            StaticJavaParser.parseMethodDeclaration(FSLRecord.getMethodWithAssertions());
                        } catch (Exception e) {
                            return false;
                        }
                        return FSLRecord.getEmbeddingVector() != null && !FSLRecord.getEmbeddingVector().isEmpty();
                    }) //because of character encoding problems on behalf of OpenAI, we have saved null when calculating embeddings. so to ensure they are not null, we discard corrupted records on reading
                    .map(FSLRecord -> {
                        double similarity = calculateCosineSimilarity(FSLRecord.getEmbeddingVector(), methodEmbeddingsPair.getFirst());
                        return new FSL_Pair(FSLRecord, similarity);
                    })
                    .sorted(Comparator.comparingDouble(pair -> ((FSL_Pair) pair).getSimilarity()).reversed())
                    .filter(fsl_pair -> fsl_pair.getSimilarity() >= minimumAcceptableCosineValue)
                    .limit(n)
                    .forEach(fsl_pair ->
                    {
                        MethodDeclaration methodWithoutAssertions = StaticJavaParser.parseMethodDeclaration(fsl_pair.getRecord().getMethodWithoutAssertions());
                        MethodDeclaration methodWithAssertions = StaticJavaParser.parseMethodDeclaration(fsl_pair.getRecord().getMethodWithAssertions());
                        try {
                            map.put(
                                    parser.getCommentLessAndJavadocLessMethodDeclaration(methodWithoutAssertions).toString(),
                                    parser.getCommentLessAndJavadocLessMethodDeclaration(methodWithAssertions).toString()
                            );
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        } else
            throw new Exception("no method body exists while calculating embeddings");

        return Pair.of(map, cost);
    }

    public static Pair<Map<String, String>, Double> mineSimilarMethods(LLM_Client client,
                                                                       Parser parser,
                                                                       int numberOfSimilarMinedExamples,
                                                                       MethodDeclaration methodDeclaration,
                                                                       List<FSLRecord> fslRecords,
                                                                       double minimumAcceptableCosineValue) throws Exception {
        return getTopNSimilarMethods(client, parser, methodDeclaration, numberOfSimilarMinedExamples, fslRecords,
                minimumAcceptableCosineValue);
    }
}

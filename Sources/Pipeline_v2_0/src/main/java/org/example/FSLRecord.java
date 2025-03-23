package org.example;

import com.github.javaparser.StaticJavaParser;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.example.Constants.EMBEDDING_BATCH_SIZE;

public class FSLRecord {
    private List<Double> embeddingVector;
    private String methodWithAssertions;
    private String methodWithoutAssertions;
    private String name;
    private String path;

    public FSLRecord(String methodWithAssertions, String methodWithoutAssertions, String name, String path) {
        this.methodWithAssertions = methodWithAssertions;
        this.methodWithoutAssertions = methodWithoutAssertions;
        this.name = name;
        this.path = path;
    }

    public FSLRecord(Method method, InputRecord record) {
        this(method.getOriginalContent(), method.getContentWithoutAssertion(), record.getName(), record.getPath());
    }

    /**
     * Due to infinity size of cache for current version, save and load to/from cache takes time and given that the size of fsl embeddings to call api is a lot (~ 48K) , we ignore caching them
     *
     * @param fslRecords fsl records to calculate embeddings by calling API
     * @param client     llm client to call API
     */
    public static double calculateEmbeddings(List<FSLRecord> fslRecords, LLM_Client client) throws Exception {
//        int counter = 0;
//        for (FSLRecord fslRecord : fslRecords) {
//            try {
//                String body = StaticJavaParser.parseMethodDeclaration(fslRecord.getMethodWithoutAssertions()).getBody().get().toString();
//                fslRecord.setEmbeddingVector(client.calculateEmbeddings(body));
//                System.out.printf("Calculated sample %d / %d%n", ++counter, fslRecords.size());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }

        double cost = 0;
        for (int i = 0; i < fslRecords.size(); i += EMBEDDING_BATCH_SIZE) {
            int endIndex = Math.min(i + EMBEDDING_BATCH_SIZE, fslRecords.size());
            List<FSLRecord> subList = fslRecords.subList(i, endIndex);
            System.out.printf("Calculating sample [%d-%d] / %d%n", i, endIndex, fslRecords.size());

            Pair<List<List<Double>>, Double> batchEmbeddings = client
                    .calculateEmbeddingsForBatchInputs(
                            subList
                                    .stream()
                                    .filter(fslRecord -> //to discard methods with 'instanceof' which are not supported by the JavaParser
                                            !fslRecord.getMethodWithoutAssertions().contains("instanceof") &&
                                                    !fslRecord.getMethodWithAssertions().contains("instanceof")
                                    ).
                                    map(fslRecord -> { //to discard Text Block Literals which are not supported by the JavaParser
                                        try {
                                            return StaticJavaParser.parseMethodDeclaration(fslRecord.getMethodWithoutAssertions()).getBody().get().toString();
                                        } catch (Exception e) {
                                            return null;
                                        }
                                    })
                                    .filter(Objects::nonNull)
                                    .map(parsedMethodBodyStr -> new LLM_InputContent(null, parsedMethodBodyStr, null))
                                    .collect(Collectors.toList()));
            for (int j = 0; j < batchEmbeddings.getFirst().size(); j++) {
                subList.get(j).setEmbeddingVector(batchEmbeddings.getFirst().get(j));
            }
            cost += batchEmbeddings.getSecond();
        }

        return cost;
    }

    @Override
    public String toString() {
        return "FSLRecord{" +
                "methodWithAssertions='" + methodWithAssertions + '\'' +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<Double> getEmbeddingVector() {
        return embeddingVector;
    }

    public void setEmbeddingVector(List<Double> embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public String getMethodWithAssertions() {
        return methodWithAssertions;
    }

    public void setMethodWithAssertions(String methodWithAssertions) {
        this.methodWithAssertions = methodWithAssertions;
    }

    public String getMethodWithoutAssertions() {
        return methodWithoutAssertions;
    }

    public void setMethodWithoutAssertions(String methodWithoutAssertions) {
        this.methodWithoutAssertions = methodWithoutAssertions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FSLRecord fslRecord = (FSLRecord) o;
        return Objects.equals(methodWithAssertions, fslRecord.methodWithAssertions) &&
                Objects.equals(methodWithoutAssertions, fslRecord.methodWithoutAssertions) &&
                Objects.equals(name, fslRecord.name) &&
                Objects.equals(path, fslRecord.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodWithAssertions, methodWithoutAssertions, name, path);
    }
}

package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.data_model.Dataset;
import org.example.data_model.Method;
import org.example.data_model.RepoFile;
import org.example.data_model.Repository;

import java.io.File;
import java.io.IOException;

public class RepoWriter {
    private final String repoDirPath;
    private final String dataFilePath;
    private final String outputFilePath;

    private final ObjectMapper objectMapper;

    public RepoWriter(String repoDirPath, String dataFilePath, String outputFilePath) {
        this.repoDirPath = repoDirPath;
        this.dataFilePath = dataFilePath;
        this.outputFilePath = outputFilePath;
        this.objectMapper = new ObjectMapper();
    }

    public String saveDataset() {
        try {
            Dataset dataset = new Dataset(repoDirPath, dataFilePath);
            System.out.println("\nPopulating ...\n");
            dataset.populate();
            System.out.println("\nPopulated!\n");
            File file = new File(outputFilePath);
            System.out.println("Saving ...");
            objectMapper.writeValue(file, dataset.getRepositories());
            System.out.println("The file saved at " + outputFilePath);
            int filesWithAssertions=0;
            int assertions=0;
            int methods = 0;
            int methodsWithAssertions = 0;
            for(Repository repository: dataset.getRepositories()) {
                filesWithAssertions+=repository.getFiles().size();
                for(RepoFile file1:repository.getFiles()){
                    methods+=file1.getMethods().size();
                    for(Method method:file1.getMethods()) {
                        if(!method.getAssertions().isEmpty()) {
                            methodsWithAssertions+=1;
                        }
                        assertions += method.getAssertions().size();
                    }
                }
            }

            System.out.println("You have "+ dataset.getRepositories().size() + " repositories, "+
                    methods+" methods, "+
                    methodsWithAssertions+" methods with assertions,"+
                    filesWithAssertions+" files with assertions, "+
                    assertions+" assertions loaded!");
            return objectMapper.writeValueAsString(dataset.getRepositories());
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }
}

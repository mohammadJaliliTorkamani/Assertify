package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class LLMCacheManager {

    private static String getModel(LLM_Client.LLM_Operation operation, LLM_Config config) {
        switch (operation) {
            case EMBEDDING:
                return config.getEmbeddingsModel().toString();
            case COMPLETION:
                return config.getCompletionModel().toString();
            default:
                return null;
        }
    }

    /**
     * search throughout the cache file and returns the corresponding response
     *
     * @param input     llm input content
     * @param operation the operation used when using llm
     * @return the corresponding response, or Null if file not exists or record was not found
     * @throws Exception if there were any exceptions in between
     */
    public Pair<String, Double> retrieve(LLM_InputContent input, LLM_Client.LLM_Operation operation, LLM_Config config) throws Exception {
        String model = getModel(operation, config);
        if (!FileUtils.fileExists(Constants.LLM_CACHE_FILE) || model == null)
            return null;

        double temperature = config.getTemperature();
        double top_p = config.getTop_p();
        double presence_penalty = config.getPresence_penalty();
        double frequency_penalty = config.getFrequency_penalty();

        List<LLM_CacheRecord> totalRecords = read();
        List<LLM_CacheRecord> cacheRecords = totalRecords
                .stream()
                .filter(llm_cacheRecord -> llm_cacheRecord.getContent().equals(input) &&
                        llm_cacheRecord.getOperation().equals(operation.toString()) &&
                        llm_cacheRecord.getModel().equals(model) &&
                        llm_cacheRecord.getTemperature() == temperature &&
                        llm_cacheRecord.getTop_p() == top_p &&
                        llm_cacheRecord.getPresence_penalty() == presence_penalty &&
                        llm_cacheRecord.getFrequency_penalty() == frequency_penalty
                )
                .collect(Collectors.toCollection(LinkedList::new));
        if (!cacheRecords.isEmpty())
            return Pair.of(cacheRecords.get(cacheRecords.size() - 1).getResponse(), cacheRecords.get(cacheRecords.size() - 1).getCost());
        return null;
    }

    public boolean store(LLM_InputContent input, LLM_Client.LLM_Operation operation, String response, LLM_Config config, double totalCost) throws Exception {
        if (response.startsWith("Traceback") || response.startsWith("Exceeded maximum trials"))
            return false;

        String model = getModel(operation, config);
        if (model == null)
            return false;

        double temperature = config.getTemperature();
        double top_p = config.getTop_p();
        double presence_penalty = config.getPresence_penalty();
        double frequency_penalty = config.getFrequency_penalty();

        LLM_CacheRecord cacheRecord = new LLM_CacheRecord(input, operation.toString(), model, response, temperature,
                top_p, presence_penalty, frequency_penalty, totalCost);
        List<LLM_CacheRecord> list = new LinkedList<>();
        list.add(cacheRecord);

        if (!FileUtils.fileExists(Constants.LLM_CACHE_FILE)) {
            return write(list);
        } else {
            List<LLM_CacheRecord> records = read();
            if (records.size() == Constants.MAX_CACHE_SIZE) {
                records.add(0, cacheRecord);
                records.remove(records.size() - 1);
            } else
                records.add(cacheRecord);
            return write(records);
        }
    }

    /**
     * reads records from LLM_CACHE_FILE and returns a list populated with that.
     *
     * @return a list populated with file records
     * @throws Exception if there were any exceptions in between
     */
    private List<LLM_CacheRecord> read() throws Exception {
        List<LLM_CacheRecord> records = new LinkedList<>();
        if (!FileUtils.fileExists(Constants.LLM_CACHE_FILE))
            return records;
        BufferedReader reader = new BufferedReader(new FileReader(Constants.LLM_CACHE_FILE));
        Gson gson = new Gson();
        records.addAll(Arrays.asList(gson.fromJson(reader, LLM_CacheRecord[].class)));

        return records;
    }

    private boolean write(List<LLM_CacheRecord> cacheRecords) throws Exception {
        //todo: for now, I just comment caching
//        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
//        FileWriter fileWriter = new FileWriter(Constants.LLM_CACHE_FILE);
//        gson.toJson(cacheRecords, fileWriter);
//        fileWriter.close();

        return true;
    }
}

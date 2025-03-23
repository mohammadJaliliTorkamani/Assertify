package org.example.filters;

import org.example.ApiClient;
import org.example.DefaultApiClient;
import org.example.Filter;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.DefaultApiClient.Operation.GET_FORKS;
import static org.example.Utils.getRepoName;
import static org.example.Utils.getRepoOwner;

public class ForksFilter implements Filter {
    private final int minimumForks;

    public ForksFilter(int minimumForks) {
        this.minimumForks = minimumForks;
    }

    @Override
    public Set<String> apply(Set<String> urls) {
        return urls.stream().filter(item -> {
            ApiClient client = new DefaultApiClient();
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Token Goes Here");
            try {
                Map<String, String> queryMap = new HashMap<>();
                JSONObject jsonObject = client.get(GET_FORKS, getRepoOwner(item), getRepoName(item), headers, queryMap).getJsonArray().getJSONObject(0);
                return jsonObject.getInt("forks") >= minimumForks;
            } catch (Exception e) {
                return false;
            }
        }).collect(Collectors.toSet());
    }
}

package org.example.filters;

import org.example.ApiClient;
import org.example.DefaultApiClient;
import org.example.Filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.DefaultApiClient.Operation.EXISTENCE;
import static org.example.Utils.getRepoName;
import static org.example.Utils.getRepoOwner;

public class ExistenceFilter implements Filter {
    @Override
    public Set<String> apply(Set<String> urls) {
        return urls.stream().filter(item -> {
            ApiClient client = new DefaultApiClient();
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Token Goes Here");
            try {
                Map<String, String> queryMap = new HashMap<>();
                int responseCode = client.get(EXISTENCE, getRepoOwner(item), getRepoName(item), headers, queryMap).getResponseCode();
                return responseCode == 200;
            } catch (Exception e) {
                return false;
//                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }
}

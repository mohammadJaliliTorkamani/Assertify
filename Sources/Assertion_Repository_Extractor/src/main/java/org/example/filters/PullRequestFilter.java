package org.example.filters;

import org.example.ApiClient;
import org.example.DefaultApiClient;
import org.example.Filter;
import org.example.RepositoriesInfo;
import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.DefaultApiClient.Operation.GET_PULL_REQUESTS;
import static org.example.Utils.*;

public class PullRequestFilter implements Filter {
    private final int minimumPR_ageInDays;

    public PullRequestFilter(int minimumPR_ageInDays) {
        this.minimumPR_ageInDays = minimumPR_ageInDays;
    }

    @Override
    public Set<String> apply(Set<String> urls) {
        return urls.stream().filter(item -> {
            ApiClient client = new DefaultApiClient();
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Token Goes Here");
            try {
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put("since", getLastN_DaysDateTime(minimumPR_ageInDays));
                JSONArray jsonArray = client.get(GET_PULL_REQUESTS, getRepoOwner(item), getRepoName(item),
                        headers, queryMap).getJsonArray();
                RepositoriesInfo.getInstance().putNumberOfPR(item, jsonArray.length());
                return jsonArray.length() > 0;
            } catch (Exception e) {
                return false;
//                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }
}

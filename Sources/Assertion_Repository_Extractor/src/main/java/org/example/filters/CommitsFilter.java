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

import static org.example.DefaultApiClient.Operation.GET_COMMITS;
import static org.example.Utils.*;

public class CommitsFilter implements Filter {
    private final int minimumCommitsAgeInDays;

    public CommitsFilter(int minimumCommitsAgeInDays) {
        this.minimumCommitsAgeInDays = minimumCommitsAgeInDays;
    }

    @Override
    public Set<String> apply(Set<String> urls) {
        return urls.stream().filter(item -> {
            ApiClient client = new DefaultApiClient();
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Token Goes Here");
            try {
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put("since", getLastN_DaysDateTime(minimumCommitsAgeInDays));
                JSONArray jsonArray = client.get(GET_COMMITS, getRepoOwner(item), getRepoName(item), headers, queryMap).getJsonArray();
                RepositoriesInfo.getInstance().putNumberOfCommits(item, jsonArray.length());
                return jsonArray.length() > 0;
            } catch (Exception e) {
                return false;
//                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }
}

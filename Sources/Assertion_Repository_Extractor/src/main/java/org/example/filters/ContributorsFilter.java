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

import static org.example.DefaultApiClient.Operation.GET_CONTRIBUTORS;
import static org.example.Utils.getRepoName;
import static org.example.Utils.getRepoOwner;

public class ContributorsFilter implements Filter {
    private final int minimumContributors;

    public ContributorsFilter(int minimumContributors) {
        this.minimumContributors = minimumContributors;
    }

    @Override
    public Set<String> apply(Set<String> urls) {
        assert minimumContributors <= 30 : "MINIMUM MUST BE LESS THAN 30 DUE TO GITHUB PAGE LIMITATION";
        return urls.stream().filter(item -> {
            ApiClient client = new DefaultApiClient();
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Token Goes Here");
            try {
                Map<String, String> queryMap = new HashMap<>();
                JSONArray jsonArray = client.get(GET_CONTRIBUTORS, getRepoOwner(item), getRepoName(item), headers, queryMap).getJsonArray();
                RepositoriesInfo.getInstance().putNumberOfContributors(item, jsonArray.length());
                return jsonArray.length() >= minimumContributors;
            } catch (Exception e) {
                return false;
//                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }
}

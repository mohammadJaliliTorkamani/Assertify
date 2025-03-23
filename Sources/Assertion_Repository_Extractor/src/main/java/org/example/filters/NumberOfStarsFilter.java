package org.example.filters;

import org.example.ApiClient;
import org.example.DefaultApiClient;
import org.example.Filter;
import org.example.RepositoriesInfo;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.DefaultApiClient.Operation.GET_STARS;
import static org.example.Utils.getRepoName;
import static org.example.Utils.getRepoOwner;

public class NumberOfStarsFilter implements Filter {
    private final int minimumStars;

    public NumberOfStarsFilter(int minimumStars) {
        this.minimumStars = minimumStars;
    }

    @Override
    public Set<String> apply(Set<String> urls) {
        return urls.stream().filter(item -> {
            System.out.println("Applying (of "+urls.size()+")...");
            ApiClient client = new DefaultApiClient();
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Token Goes Here");
            try {
                Map<String, String> map = new HashMap<>();
                JSONObject jsonObject = client.get(GET_STARS, getRepoOwner(item), getRepoName(item), headers, map).getJsonArray().getJSONObject(0);
                RepositoriesInfo.getInstance().putNumberOfStars(item, jsonObject.getInt("watchers_count"));
                return jsonObject.getInt("watchers_count") >= minimumStars;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
//                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }
}
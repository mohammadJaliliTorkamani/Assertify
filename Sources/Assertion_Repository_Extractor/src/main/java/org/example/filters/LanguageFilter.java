package org.example.filters;

import org.example.ApiClient;
import org.example.DefaultApiClient;
import org.example.Filter;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.DefaultApiClient.Operation.GET_LANGUAGE;
import static org.example.Utils.getRepoName;
import static org.example.Utils.getRepoOwner;

public class LanguageFilter implements Filter {
    private final String[] languages;

    public LanguageFilter(String[] languages) {
        this.languages = languages;
    }

    @Override
    public Set<String> apply(Set<String> urls) {
        return urls.stream().filter(item -> {
            ApiClient client = new DefaultApiClient();
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Token Goes Here");
            try {
                Map<String, String> map = new HashMap<>();
                JSONObject jsonObject = client.get(GET_LANGUAGE, getRepoOwner(item), getRepoName(item), headers, map).getJsonArray().getJSONObject(0);
                return Arrays.stream(languages).anyMatch(jsonObject::has);
            } catch (Exception e) {
                return false;
            }
        }).collect(Collectors.toSet());
    }
}

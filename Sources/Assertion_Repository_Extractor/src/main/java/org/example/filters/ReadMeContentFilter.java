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

import static org.example.DefaultApiClient.Operation.GET_README;
import static org.example.Utils.*;

public class ReadMeContentFilter implements Filter {
    private final String[] prohibitedKeywords;

    public ReadMeContentFilter(String[] prohibitedKeywords) {
        this.prohibitedKeywords = prohibitedKeywords;
    }

    @Override
    public Set<String> apply(Set<String> urls) {
        return urls.stream().filter(item -> {
            ApiClient client = new DefaultApiClient();
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Token Goes Here");
            try {
                Map<String, String> queryMap = new HashMap<>();
                JSONObject jsonObject = client.get(GET_README, getRepoOwner(item), getRepoName(item), headers, queryMap).getJsonArray().getJSONObject(0);
                String base64ReadMe = jsonObject.getString("content");
                base64ReadMe = base64ReadMe.replace("\n", "").trim();
                try {
                    RepositoriesInfo.getInstance().putReadmeKeywords(item, !hasAnyKeywords(decodeBase64(base64ReadMe), prohibitedKeywords));
                    return !hasAnyKeywords(decodeBase64(base64ReadMe), prohibitedKeywords);
                } catch (Exception e) {
                    RepositoriesInfo.getInstance().putReadmeKeywords(item, false);
                    return false;
                }
            } catch (Exception e) {
                return false;
//                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }
}

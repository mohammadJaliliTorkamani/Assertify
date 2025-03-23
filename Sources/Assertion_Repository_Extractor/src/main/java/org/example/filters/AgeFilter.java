package org.example.filters;

import org.example.ApiClient;
import org.example.DefaultApiClient;
import org.example.Filter;
import org.example.RepositoriesInfo;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.DefaultApiClient.Operation.GET_CREATED_AT;
import static org.example.Utils.getRepoName;
import static org.example.Utils.getRepoOwner;

public class AgeFilter implements Filter {
    private final int minimumAgeInYear;

    public AgeFilter(int minimumAgeInYear) {
        this.minimumAgeInYear = minimumAgeInYear;
    }

    @Override
    public Set<String> apply(Set<String> urls) {
        return urls.stream().filter(item -> {
            ApiClient client = new DefaultApiClient();
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Token Goes Here");
            try {
                Map<String, String> queryMap = new HashMap<>();
                JSONObject jsonObject = client.get(GET_CREATED_AT, getRepoOwner(item), getRepoName(item), headers, queryMap).getJsonArray().getJSONObject(0);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
                LocalDateTime now = LocalDateTime.now();
                LocalDate date2 = LocalDate.parse(jsonObject.getString("created_at"), formatter);
                RepositoriesInfo.getInstance().putAge(item, date2.until(now.toLocalDate()).getYears());
                return date2.until(now.toLocalDate()).getYears() >= minimumAgeInYear;
            } catch (Exception e) {
                return false;
//                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }
}

package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DefaultApiClient implements ApiClient {
    private static final String GET_LANGUAGE_BASE_URL = "https://api.github.com/repos";
    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final String GITHUB_ACCEPT = "application/vnd.github+json";
    private JSONArray jsonArray;
    private int responseCode;

    /**
     * returns the status (response) code of the web request
     *
     * @return the status (response) code (200 -> success)
     */
    public int getResponseCode() {
        return responseCode;
    }

    public JSONArray getJsonArray() {
        return jsonArray;
    }

    /**
     * performs a GET request to GitHub Apis and returns the desired output (detected by @operation)
     *
     * @param operation    operation to be done
     * @param repoOwner    owner of the repository's owner
     * @param repoName     name of the repository
     * @param headerParams headers (except 'Accept' and 'X-GitHub-Api-Version') to be included in GET request
     * @param queryParams  query parameters to be included in the GET request
     * @return the desired content
     * @throws Exception if there was a problem while opening the web connection
     */
    public DefaultApiClient get(Operation operation, String repoOwner, String repoName,
                                Map<String, String> headerParams, Map<String, String> queryParams) throws Exception {
        headerParams.put("Accept", GITHUB_ACCEPT);
        headerParams.put("X-GitHub-Api-Version", GITHUB_API_VERSION);

        String path = "";
        switch (operation) {
            case GET_COMMITS:
                path = "/commits";
                break;
            case GET_ISSUES:
                path = "/issues";
                break;
            case GET_PULL_REQUESTS:
                path = "/pulls";
                break;
            case GET_LANGUAGE:
                path = "/languages";
                break;
            case GET_README:
                path = "/readme";
                break;
            case GET_CONTRIBUTORS:
                path = "/contributors";
                break;
        }

        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        HttpURLConnection con = (HttpURLConnection) new URL(
                String.format("%s/%s/%s%s%s", GET_LANGUAGE_BASE_URL, repoOwner, repoName, path, queryParams.size() > 0 ? "?" + queryString : "")
        ).openConnection();

        con.setRequestMethod("GET");
//        htt/*/pcon.addRequestProperty("User-Agent", "Mozilla/4.76");
        con.setRequestProperty("Cookie", "foo=bar");

        for (Map.Entry<String, String> entry : headerParams.entrySet()) {
            con.setRequestProperty(entry.getKey(), entry.getValue());
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            if (response.toString().charAt(0) == '[') {
                jsonArray = new JSONArray(response.toString());
            } else {
                JSONObject jsonObject = new JSONObject(response.toString());
                jsonArray = new JSONArray();
                jsonArray.put(jsonObject);
            }
            responseCode = con.getResponseCode();
            con.disconnect();
            return this;
        }
    }

    public enum Operation {
        GET_LANGUAGE, GET_COMMITS, GET_README, GET_CREATED_AT, GET_ISSUES, GET_PULL_REQUESTS, GET_CONTRIBUTORS, GET_FORKS,
        EXISTENCE, GET_STARS
    }
}

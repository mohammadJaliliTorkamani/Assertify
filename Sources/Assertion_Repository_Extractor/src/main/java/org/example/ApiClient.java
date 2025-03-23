package org.example;

import java.util.Map;

public interface ApiClient {
    DefaultApiClient get(DefaultApiClient.Operation operation, String repoOwner, String repoName,
                         Map<String, String> headerParams, Map<String, String> queryParams) throws Exception;
}

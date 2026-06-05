package org.example.telgrambotaiassistant.client;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;


@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProxyApiClient {
    @Autowired
    RestClient restClient;

    public String post(String uri, Object body) {
        return restClient
                .post()
                .uri(uri)
                .body(body)
                .retrieve()
                .body(String.class);
    }
}

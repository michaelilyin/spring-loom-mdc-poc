package org.example;

import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


public class Commons {
    public static RequestEntity<Void> prepareRequest(String plainCreds, String method) {
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Creds);
        return RequestEntity.get("http://localhost:8080" + method)
                .headers(headers)
                .build();
    }
}

package org.example.loom;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestControllerTest {
    private final TestRestTemplate client;

    @Autowired
    TestControllerTest(TestRestTemplate client) {
        this.client = client;
    }

    @Test
    void callTestApi() throws Exception {
        var request1 = prepareRequest("user:password", "/api/test");
        var request2 = prepareRequest("other:password", "/api/test");
        var request3 = prepareRequest("user:password", "/api/test");

        var response1 = client.exchange(request1, String.class);
        var response2 = client.exchange(request2, String.class);
        var response3 = client.exchange(request3, String.class);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void callScopeApi() throws Exception {
        var request1 = prepareRequest("user:password", "/api/scope");

        var response1 = client.exchange(request1, String.class);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void callAsyncApi() throws Exception {
        var request1 = prepareRequest("user:password", "/api/async");

        var response1 = client.exchange(request1, String.class);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static @NotNull RequestEntity<Void> prepareRequest(String plainCreds, String method) {
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", STR."Basic \{base64Creds}");
        var request = RequestEntity.get(method)
                .headers(headers)
                .build();
        return request;
    }
}

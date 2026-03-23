package me.soknight.minigram.chats.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProfileServiceAuthInterceptorTest {

    private static final String BASE_URL = "http://localhost:5001";
    private static final String AUTHORIZATION = "Bearer test-jwt-token";

    private MockRestServiceServer server;
    private RestClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestInterceptor(new ProfileServiceAuthInterceptor());

        server = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();

        var request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, AUTHORIZATION);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void intercept_forwardsAuthorizationHeader() {
        server.expect(requestTo(BASE_URL + "/api/v1/profiles/relations/00000000-0000-0000-0000-000000000002?type=Outgoing"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andRespond(withSuccess("""
                        {
                          "status": "Friend",
                          "profile": {
                            "id": "00000000-0000-0000-0000-000000000002",
                            "name": "Bob",
                            "photoUrl": null
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        client.get()
                .uri("/api/v1/profiles/relations/{receiverId}?type=Outgoing", "00000000-0000-0000-0000-000000000002")
                .retrieve()
                .toBodilessEntity();

        server.verify();
    }

}

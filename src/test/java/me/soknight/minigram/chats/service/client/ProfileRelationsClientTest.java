package me.soknight.minigram.chats.service.client;

import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.model.attribute.RelationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProfileRelationsClientTest {

    private static final String BASE_URL = "http://localhost:5001";
    private static final UUID RECEIVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private MockRestServiceServer server;
    private ProfileRelationsClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ProfileRelationsClient(builder.build());
    }

    @Test
    void getRelations_returnsProfilePage() throws ApiException {
        server.expect(requestTo(BASE_URL + "/api/v1/profiles/relations?status=Accepted&Page=2&PerPage=50"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "count": 1,
                          "data": [
                            {
                              "userId": "%s",
                              "name": "Bob",
                              "photoUrl": "https://example.com/avatar.png"
                            }
                          ]
                        }
                        """.formatted(RECEIVER_ID), MediaType.APPLICATION_JSON));

        var response = client.getRelations(RelationStatus.ACCEPTED, 2, 50);

        assertThat(response.count()).isEqualTo(1);
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().userId()).isEqualTo(RECEIVER_ID);
        assertThat(response.data().getFirst().name()).isEqualTo("Bob");
        server.verify();
    }

    @Test
    void getRelation_returnsRelation() throws ApiException {
        server.expect(requestTo(BASE_URL + "/api/v1/profiles/relations/" + RECEIVER_ID))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "status": "Accepted",
                          "profile": {
                            "userId": "%s",
                            "name": "Bob",
                            "photoUrl": null
                          }
                        }
                        """.formatted(RECEIVER_ID), MediaType.APPLICATION_JSON));

        var relation = client.getRelation(RECEIVER_ID);

        assertThat(relation.status()).isEqualTo(RelationStatus.ACCEPTED);
        assertThat(relation.profile().userId()).isEqualTo(RECEIVER_ID);
        server.verify();
    }

    @Test
    void setRelation_sendsPostRequest() throws ApiException {
        server.expect(requestTo(BASE_URL + "/api/v1/profiles/relations/" + RECEIVER_ID + "?status=Blocked"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withNoContent());

        client.setRelation(RECEIVER_ID, RelationStatus.BLOCKED);

        server.verify();
    }

    @Test
    void deleteRelation_sendsDeleteRequest() throws ApiException {
        server.expect(requestTo(BASE_URL + "/api/v1/profiles/relations/" + RECEIVER_ID))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        client.deleteRelation(RECEIVER_ID);

        server.verify();
    }

    @Test
    void getRelation_whenProfileServiceFails_throwsApiException() {
        server.expect(requestTo(BASE_URL + "/api/v1/profiles/relations/" + RECEIVER_ID))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getRelation(RECEIVER_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    var apiException = (ApiException) ex;
                    assertThat(apiException.getStatusCode().value()).isEqualTo(502);
                    assertThat(apiException.constructModel().errorCode()).isEqualTo("profile_service_unavailable");
                });

        server.verify();
    }

}

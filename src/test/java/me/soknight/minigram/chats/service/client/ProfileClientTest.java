package me.soknight.minigram.chats.service.client;

import me.soknight.minigram.chats.exception.ApiException;
import me.soknight.minigram.chats.service.client.model.attribute.RelationStatus;
import me.soknight.minigram.chats.service.client.model.attribute.RelationType;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProfileClientTest {

    private static final String BASE_URL = "http://localhost:5001";
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private MockRestServiceServer server;
    private ProfileClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ProfileClient(builder.build());
    }

    @Test
    void getProfile_returnsProfile() throws ApiException {
        server.expect(requestTo(BASE_URL + "/api/v1/profiles/" + USER_ID))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "%s",
                          "name": "Bob",
                          "photoUrl": "https://example.com/avatar.png"
                        }
                        """.formatted(USER_ID), MediaType.APPLICATION_JSON));

        var profile = client.getProfile(USER_ID);

        assertThat(profile.userId()).isEqualTo(USER_ID);
        assertThat(profile.name()).isEqualTo("Bob");
        assertThat(profile.photoUrl()).isEqualTo("https://example.com/avatar.png");
        server.verify();
    }

    @Test
    void getRelations_returnsProfilePage() throws ApiException {
        server.expect(requestTo(BASE_URL + "/api/v1/profiles/relations?status=Friend&type=Outgoing&Page=2&PerPage=50"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "count": 1,
                          "data": [
                            {
                              "id": "%s",
                              "name": "Bob",
                              "photoUrl": "https://example.com/avatar.png"
                            }
                          ]
                        }
                        """.formatted(USER_ID), MediaType.APPLICATION_JSON));

        var response = client.getRelations(RelationStatus.FRIEND, RelationType.OUTGOING, 2, 50);

        assertThat(response.count()).isEqualTo(1);
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().userId()).isEqualTo(USER_ID);
        assertThat(response.data().getFirst().name()).isEqualTo("Bob");
        server.verify();
    }

    @Test
    void getRelation_returnsRelation() throws ApiException {
        server.expect(requestTo(BASE_URL + "/api/v1/profiles/relations/" + USER_ID + "?type=Outgoing"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "status": "Friend",
                          "profile": {
                            "id": "%s",
                            "name": "Bob",
                            "photoUrl": null
                          }
                        }
                        """.formatted(USER_ID), MediaType.APPLICATION_JSON));

        var relation = client.getRelation(USER_ID, RelationType.OUTGOING);

        assertThat(relation.status()).isEqualTo(RelationStatus.FRIEND);
        assertThat(relation.profile().userId()).isEqualTo(USER_ID);
        server.verify();
    }

    @Test
    void getRelation_whenProfileServiceFails_throwsApiException() {
        server.expect(requestTo(BASE_URL + "/api/v1/profiles/relations/" + USER_ID + "?type=Outgoing"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getRelation(USER_ID, RelationType.OUTGOING))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    var apiException = (ApiException) ex;
                    assertThat(apiException.getStatusCode().value()).isEqualTo(502);
                    assertThat(apiException.constructModel().errorCode()).isEqualTo("profile_service_unavailable");
                });

        server.verify();
    }

}

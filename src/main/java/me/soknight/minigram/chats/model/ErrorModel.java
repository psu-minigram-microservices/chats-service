package me.soknight.minigram.chats.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorModel(
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_message") String errorMessage,
        @JsonProperty("payload") @JsonInclude(JsonInclude.Include.NON_NULL) Object payload
) {

    public ErrorModel(String errorCode, String errorMessage) {
        this(errorCode, errorMessage, null);
    }

    public ErrorModel(String errorCode, String errorMessage, Object payload) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "ErrorModel{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", payload=" + payload +
                '}';
    }

}
package me.soknight.minigram.chats.model.attribute;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RelationStatus {

    PENDING     ("Pending"),
    ACCEPTED    ("Accepted"),
    REJECTED    ("Rejected"),
    BLOCKED     ("Blocked");

    private final @NonNull String key;

    @JsonValue
    public @NonNull String getKey() {
        return key;
    }

    @JsonCreator
    public static @NonNull RelationStatus ofKey(@NonNull String key) {
        return Arrays.stream(values())
                .filter(status -> status.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown relationship status: " + key));
    }

}
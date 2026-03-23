package me.soknight.minigram.chats.model.attribute;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RelationType {

    INCOMING("Incoming"),
    OUTGOING("Outgoing");

    private final @NonNull String key;

    @JsonValue
    public @NonNull String getKey() {
        return key;
    }

    @JsonCreator
    public static @NonNull RelationType ofKey(@NonNull String key) {
        return Arrays.stream(values())
                .filter(type -> type.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown relation type: " + key));
    }

}

package me.soknight.minigram.chats.model.attribute;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

@Getter
@AllArgsConstructor
public enum ChatMemberRole {

    OWNER   ("owner"),
    MEMBER  ("member"),
    ;

    @JsonValue
    private final @NonNull String key;

}
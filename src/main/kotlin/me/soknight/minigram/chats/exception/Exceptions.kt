@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.minigram.chats.exception

import io.ktor.http.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed class AppException(
    val statusCode: HttpStatusCode,
    val errorCode: String,
    message: String
) : Exception(message)

class ChatNotFoundException(id: Long)
    : AppException(HttpStatusCode.NotFound, "chat_not_found", "Chat $id not found")

class MemberNotFoundException(chatId: Long, userId: Uuid)
    : AppException(HttpStatusCode.NotFound, "member_not_found", "User $userId is not a member of chat $chatId")

class MessageNotFoundException(chatId: Long, messageId: Long)
    : AppException(HttpStatusCode.NotFound, "message_not_found", "Message $messageId not found in chat $chatId")

class AccessDeniedException
    : AppException(HttpStatusCode.Forbidden, "access_denied", "Access denied")

class InvalidChatMembersException(reason: String)
    : AppException(HttpStatusCode.BadRequest, "invalid_chat_members", reason)

class InvalidChatTitleException
    : AppException(HttpStatusCode.BadRequest, "invalid_chat_title", "Group chat must have a title")

class RelationNotAcceptedException
    : AppException(HttpStatusCode.Forbidden, "relation_not_accepted", "Users are not friends")

class ChatInviteNotSupportedException
    : AppException(HttpStatusCode.Conflict, "chat_invite_not_supported", "Cannot invite to this chat type")

class ChatEditNotSupportedException
    : AppException(HttpStatusCode.Conflict, "chat_edit_not_supported", "Cannot edit this chat type")

class ChatKickNotSupportedException
    : AppException(HttpStatusCode.Conflict, "chat_kick_not_supported", "Cannot kick from this chat type")

class CannotLeaveChatException
    : AppException(HttpStatusCode.Conflict, "cannot_leave_chat", "Cannot leave this chat type")

class OwnerCannotLeaveChatException
    : AppException(HttpStatusCode.Conflict, "owner_cannot_leave_chat", "Owner cannot leave the chat")

class CannotKickSelfException
    : AppException(HttpStatusCode.Conflict, "cannot_kick_self", "Cannot kick yourself")

class MemberAlreadyExistsException
    : AppException(HttpStatusCode.Conflict, "member_already_exists", "User is already a member")

class ProfileServiceUnavailableException
    : AppException(HttpStatusCode.BadGateway, "profile_service_unavailable", "Profile service is unavailable")

class ProfileServiceInvalidResponseException
    : AppException(HttpStatusCode.BadGateway, "profile_service_invalid_response", "Profile service returned an invalid response")

class ValidationException(message: String)
    : AppException(HttpStatusCode.BadRequest, "incorrect_field_value", message)

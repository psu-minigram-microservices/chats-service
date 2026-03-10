-- Drop FK that references chat_members (chat_id, user_id) from chat_messages
alter table chat_messages drop constraint fk_chat_messages_sender;

-- Convert user ID columns from bigint to uuid
alter table chats alter column owner_id set data type uuid using null;
alter table chat_members alter column user_id set data type uuid using null;
alter table chat_messages alter column sender_id set data type uuid using null;

-- Recreate FK
alter table chat_messages add constraint fk_chat_messages_sender
    foreign key (chat_id, sender_id) references chat_members (chat_id, user_id);

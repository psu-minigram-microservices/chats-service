create table user_public_keys (
    user_id           uuid                     not null primary key,
    public_key        text                     not null,
    backup_salt       text,
    backup_iv         text,
    backup_ciphertext text,
    created_at        timestamp with time zone not null,
    updated_at        timestamp with time zone not null
);

alter table chat_messages
    alter column content type text;

alter table chat_messages
    add column encrypted boolean not null default false;

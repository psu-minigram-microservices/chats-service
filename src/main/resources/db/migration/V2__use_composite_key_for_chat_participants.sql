create table chat_participants_new (
    chat_id bigint not null,
    user_id bigint not null,
    role varchar(16) not null,
    joined_at timestamp with time zone not null,
    constraint pk_chat_participants primary key (chat_id, user_id),
    constraint fk_chat_participants_chat
        foreign key (chat_id) references chats (id) on delete cascade
);

insert into chat_participants_new (chat_id, user_id, role, joined_at)
select chat_id, user_id, role, joined_at
from chat_participants;

drop table chat_participants;

alter table chat_participants_new
    rename to chat_participants;

-- Drop FK that tied message sender to current chat membership.
-- sender_id is a reference to an external user service, not an internal membership record,
-- so messages must survive member removal (leave/kick).
alter table chat_messages drop constraint fk_chat_messages_sender;

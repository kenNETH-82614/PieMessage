package com.ericchee.bboyairwreck.piemessage;

/**
 * Created by eric on 11/13/15.
 */
public class Message implements Comparable<Message> {
    private long id;
    private String msg;
    private long date;
    private String sender;
    private MessageStatus messageStatus;
    private MessageType messageType;
    private boolean read;
    private String chatId;
    private String chatName; // will only be used if chat doesn't yet exist and for notifications

    Message(String msg, String chatId, String chatName) {
        this.id = 0;
        this.msg = msg;
        this.date = Constants.getNowEpochSeconds();
        this.sender = Constants.ME;
        this.messageStatus = MessageStatus.IN_PROGRESS;
        this.messageType = MessageType.SENT;
        this.read = true;
        this.chatId = chatId;
        this.chatName = chatName;
    }

    Message(long id, String msg, long date, String sender, int isSent, int isFromMe, int isRead, String chatId, String chatName) {
        this(id, msg, date, sender, MessageStatus.SUCCESSFUL, MessageType.SENT, isRead == 1, chatId, chatName);
        if (isSent == 0) {
            messageStatus = MessageStatus.UNSUCCESSFUL;
        }

        if (isFromMe == 0) {
            messageType = MessageType.RECEIVED;
        }
    }

    private Message(long id, String msg, long date, String sender, MessageStatus messageStatus, MessageType messageType, boolean read, String chatId, String chatName) {
        this.id = id;
        this.msg = msg;
        this.date = date;
        this.sender = sender;
        this.messageStatus = messageStatus;
        this.messageType = messageType;
        this.read = read;
        this.chatId = chatId;
        this.chatName = chatName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDate() {
        return date;
    }

    void setDate(long date) {
        this.date = date;
    }

    String getMsg() {
        return msg;
    }

    String getSender() {
        if (messageType == MessageType.SENT) return Constants.ME;
        return sender;
    }

    MessageType getMessageType() {
        return messageType;
    }

    MessageStatus getMessageStatus() {
        return messageStatus;
    }

    void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    boolean isRead() {
        return read || messageType == MessageType.SENT;
    }

    void setRead(boolean read) {
        this.read = read;
        if (read) {
            PieMessageApplication.getInstance().getServerBridge().markAsRead(id);
        }
    }

    String getChatId() {
        return chatId;
    }

    String getChatName() {
        if (chatName.equals("")) return sender;
        return chatName;
    }

    Chat getChat() {
        return PieMessageApplication.getInstance().getChats().get(chatId);
    }

    @Override
    public int compareTo(Message m) {
        return (int) (id - m.getId());
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", date=" + date +
                ", msg='" + msg + '\'' +
                ", sender='" + sender + '\'' +
                ", messageType=" + messageType +
                ", messageStatus=" + messageStatus +
                ", chatId='" + chatId + '\'' +
                '}';
    }
}

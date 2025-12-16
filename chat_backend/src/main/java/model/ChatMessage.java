package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;

import java.time.Instant;
import java.util.Objects;

public class ChatMessage {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String sender;
    private String content;
    private long timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String sender, String content, long timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public static ChatMessage fromJson(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, ChatMessage.class);
    }

    public String toJson() throws JsonProcessingException {
        return MAPPER.writeValueAsString(this);
    }

    public Document toDocument() {
        return new Document()
                .append("sender", sender)
                .append("content", content)
                .append("timestamp", timestamp);
    }

    public static ChatMessage fromDocument(Document document) {
        return new ChatMessage(
                document.getString("sender"),
                document.getString("content"),
                document.getLong("timestamp")
        );
    }

    public void ensureTimestamp() {
        if (timestamp == 0L) {
            timestamp = Instant.now().toEpochMilli();
        }
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChatMessage that)) {
            return false;
        }
        return timestamp == that.timestamp &&
                Objects.equals(sender, that.sender) &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, content, timestamp);
    }
}

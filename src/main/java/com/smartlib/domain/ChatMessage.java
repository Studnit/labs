package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "chat_message", indexes = @Index(columnList = "room_id"))
public class ChatMessage extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private ChatRoom room;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser sender;
    @Column(length = 2000, nullable = false) private String content;
    @Column(nullable = false) private LocalDateTime sentAt = LocalDateTime.now();

    public ChatRoom getRoom() { return room; }
    public void setRoom(ChatRoom room) { this.room = room; }

    public AppUser getSender() { return sender; }
    public void setSender(AppUser sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}

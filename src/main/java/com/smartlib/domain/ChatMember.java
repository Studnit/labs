package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "chat_member", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
public class ChatMember extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private ChatRoom room;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser user;

    public ChatRoom getRoom() { return room; }
    public void setRoom(ChatRoom room) { this.room = room; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
}

package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "chat_room", indexes = @Index(columnList = "library_id"))
public class ChatRoom extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ChatType type = ChatType.GROUP;
    private String directKey;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ChatType getType() { return type; }
    public void setType(ChatType type) { this.type = type; }

    public String getDirectKey() { return directKey; }
    public void setDirectKey(String directKey) { this.directKey = directKey; }
}

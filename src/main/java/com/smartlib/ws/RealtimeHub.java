package com.smartlib.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/** Registry of live WebSocket sessions per user; used for chat delivery and push notifications. */

@Component

public class RealtimeHub {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RealtimeHub.class);
    private final ObjectMapper mapper;

    public RealtimeHub(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    void register(Long userId, WebSocketSession s) { sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(s); }
    void unregister(Long userId, WebSocketSession s) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set != null) { set.remove(s); if (set.isEmpty()) sessions.remove(userId); }
    }

    public void sendToUser(Long userId, Object payload) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null || set.isEmpty()) return;
        String json;
        try { json = mapper.writeValueAsString(payload); } catch (Exception e) { log.warn("serialize failed", e); return; }
        for (WebSocketSession s : set) {
            synchronized (s) {
                try { if (s.isOpen()) s.sendMessage(new TextMessage(json)); }
                catch (Exception e) { log.debug("send failed: {}", e.getMessage()); }
            }
        }
    }
    public void sendToUsers(Collection<Long> userIds, Object payload) { userIds.forEach(id -> sendToUser(id, payload)); }
}

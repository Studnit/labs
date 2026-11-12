package com.smartlib.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlib.domain.Feature;
import com.smartlib.feature.FeatureService;
import com.smartlib.security.AppUserDetails;
import com.smartlib.service.ChatService;
import com.smartlib.web.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * Single multiplexed WebSocket (/ws): pushes notifications and delivers chat messages.
 * Incoming: {"type":"chat","roomId":1,"content":"hi"}. The Chat module switch is re-checked on every message.
 */

@Component

public class RealtimeHandler extends TextWebSocketHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RealtimeHandler.class);
    private final RealtimeHub hub;
    private final ChatService chat;
    private final FeatureService features;
    private final ObjectMapper mapper;


    public RealtimeHandler(RealtimeHub hub, ChatService chat, FeatureService features, ObjectMapper mapper) {
        this.hub = hub;
        this.chat = chat;
        this.features = features;
        this.mapper = mapper;
    }
    /** Max 20 chat messages per 10 seconds per user. */
    private final java.util.Map<Long, java.util.ArrayDeque<Long>> recent = new java.util.concurrent.ConcurrentHashMap<>();
    boolean allow(Long userId, long nowMs) {
        var q = recent.computeIfAbsent(userId, k -> new java.util.ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && nowMs - q.peekFirst() > 10_000) q.pollFirst();
            if (q.size() >= 20) return false;
            q.addLast(nowMs);
            return true;
        }
    }

    private AppUserDetails user(WebSocketSession s) {
        return s.getPrincipal() instanceof Authentication a && a.getPrincipal() instanceof AppUserDetails d ? d : null;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        AppUserDetails u = user(session);
        if (u == null) { session.close(CloseStatus.POLICY_VIOLATION); return; }
        hub.register(u.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        AppUserDetails u = user(session);
        if (u == null) { session.close(CloseStatus.POLICY_VIOLATION); return; }
        try {
            JsonNode n = mapper.readTree(message.getPayload());
            if (!"chat".equals(n.path("type").asText())) return;
            if (!allow(u.getId(), System.currentTimeMillis())) throw new BusinessException("You are sending messages too fast");
            if (!features.enabled(u.getLibraryId(), Feature.CHAT)) throw new BusinessException("Chat is disabled for this library");
            ChatService.Sent sent = chat.send(n.path("roomId").asLong(), u.getId(), n.path("content").asText(""));
            hub.sendToUsers(sent.recipients(), Map.of("type", "chat", "message", sent.message()));
        } catch (BusinessException | ResponseStatusException e) {
            String msg = e instanceof BusinessException ? e.getMessage() : "Not allowed";
            hub.sendToUser(u.getId(), Map.of("type", "error", "message", msg));
        } catch (Exception e) {
            log.warn("ws message failed: {}", e.toString());
            hub.sendToUser(u.getId(), Map.of("type", "error", "message", "Could not send message"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        AppUserDetails u = user(session);
        if (u != null) hub.unregister(u.getId(), session);
    }
}

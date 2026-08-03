package com.bidding.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class AuctionWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Maps inspectionId -> Set of active WebSocket sessions
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long inspectionId = getInspectionIdFromSession(session);
        if (inspectionId == null) {
            log.warn("Connection rejected: missing or invalid inspectionId parameter");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        roomSessions.computeIfAbsent(inspectionId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("User joined auction room. InspectionId: {}, SessionId: {}", inspectionId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long inspectionId = getInspectionIdFromSession(session);
        if (inspectionId != null) {
            Set<WebSocketSession> sessions = roomSessions.get(inspectionId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessions.remove(inspectionId);
                }
            }
        }
        log.info("User disconnected. SessionId: {}", session.getId());
    }

    public void broadcast(Long inspectionId, Object message) {
        Set<WebSocketSession> sessions = roomSessions.get(inspectionId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonPayload);
            
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.error("Failed to send message to session {}", session.getId(), e);
                    }
                }
            }
            log.info("Broadcast sent for inspectionId: {}", inspectionId);
        } catch (Exception e) {
            log.error("Failed to serialize broadcast payload for inspectionId {}", inspectionId, e);
        }
    }

    private Long getInspectionIdFromSession(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return null;
        }

        String query = uri.getQuery();
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && "inspectionId".equals(pair[0])) {
                try {
                    return Long.parseLong(pair[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}

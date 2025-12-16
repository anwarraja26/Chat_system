package websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import db.MongoDBClient;
import model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ServerEndpoint("/chat")
public class ChatEndpoint {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChatEndpoint.class);
	private static final Set<Session> SESSIONS = Collections.synchronizedSet(new HashSet<>());

	@OnOpen
	public void onOpen(Session session) {
		SESSIONS.add(session);
		LOGGER.info("Session {} connected", session.getId());
		sendRecentHistory(session);
	}

	@OnMessage
	public void onMessage(String raw, Session session) {
		try {
			ChatMessage message = ChatMessage.fromJson(raw);
			message.ensureTimestamp();
			MongoDBClient.save(message);
			broadcast(message);
		} catch (JsonProcessingException ex) {
			LOGGER.warn("Session {} sent invalid payload: {}", session.getId(), ex.getMessage());
			sendError(session, "Invalid message format");
		}
	}

	@OnClose
	public void onClose(Session session) {
		SESSIONS.remove(session);
		LOGGER.info("Session {} disconnected", session.getId());
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		if (session != null) {
			LOGGER.error("WebSocket error on session {}", session.getId(), throwable);
		} else {
			LOGGER.error("WebSocket error", throwable);
		}
	}

	private void broadcast(ChatMessage message) {
		try {
			String payload = message.toJson();
			synchronized (SESSIONS) {
				for (Session session : SESSIONS) {
					if (session.isOpen()) {
						session.getAsyncRemote().sendText(payload);
					}
				}
			}
		} catch (JsonProcessingException ex) {
			LOGGER.error("Failed to broadcast message", ex);
		}
	}

	private void sendRecentHistory(Session session) {
		List<ChatMessage> history = MongoDBClient.findRecentMessages(25);
		for (ChatMessage message : history) {
			try {
				session.getAsyncRemote().sendText(message.toJson());
			} catch (JsonProcessingException ex) {
				LOGGER.warn("Failed to serialize history message for session {}", session.getId());
			}
		}
	}

	private void sendError(Session session, String error) {
		if (session == null || !session.isOpen()) {
			return;
		}
		try {
			session.getBasicRemote().sendText("{\"type\":\"error\",\"message\":\"" + error + "\"}");
		} catch (IOException ex) {
			LOGGER.warn("Failed to send error message to session {}", session != null ? session.getId() : "unknown", ex);
		}
	}
}
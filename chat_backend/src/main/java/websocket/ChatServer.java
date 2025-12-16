package websocket;

import org.glassfish.tyrus.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.DeploymentException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class ChatServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServer.class);

    private ChatServer() {
    }

    public static void main(String[] args) throws Exception {
        String host = getenvOrDefault("CHAT_SERVER_HOST", "0.0.0.0");
        int port = Integer.parseInt(getenvOrDefault("CHAT_SERVER_PORT", "8080"));
        String contextPath = getenvOrDefault("CHAT_SERVER_CONTEXT", "/chat-backend");

        Server server = new Server(host, port, contextPath, null, ChatEndpoint.class);

        try {
            server.start();
            LOGGER.info("Chat server started at ws://{}:{}{}/chat", host, port, contextPath);
            addShutdownHook(server);
            waitForExit();
        } catch (DeploymentException ex) {
            LOGGER.error("Unable to start WebSocket server", ex);
            throw ex;
        } finally {
            server.stop();
        }
    }

    private static void addShutdownHook(Server server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown requested, stopping server...");
            server.stop();
        }));
    }

    private static void waitForExit() throws Exception {
        LOGGER.info("Press ENTER to stop the server");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            reader.readLine();
        }
    }

    private static String getenvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}

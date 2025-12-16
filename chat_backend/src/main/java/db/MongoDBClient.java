package db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import model.ChatMessage;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MongoDBClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBClient.class);
	private static final String DEFAULT_URI = "mongodb://localhost:27017";
	private static final String DEFAULT_DATABASE = "chatdb";
	private static final String DEFAULT_COLLECTION = "messages";

	private static MongoCollection<Document> collection;

	static {
		initialise();
	}

	private MongoDBClient() {
	}

	private static void initialise() {
		String uri = Optional.ofNullable(System.getenv("MONGODB_URI")).orElse(DEFAULT_URI);
		String databaseName = Optional.ofNullable(System.getenv("CHAT_DB_NAME")).orElse(DEFAULT_DATABASE);
		String collectionName = Optional.ofNullable(System.getenv("CHAT_COLLECTION_NAME")).orElse(DEFAULT_COLLECTION);

		try {
			MongoClientSettings settings = MongoClientSettings.builder()
					.applyConnectionString(new ConnectionString(uri))
					.build();
			MongoClient client = MongoClients.create(settings);
			MongoDatabase database = client.getDatabase(databaseName);
			collection = database.getCollection(collectionName);
			LOGGER.info("Connected to MongoDB collection {}.{}", databaseName, collectionName);
		} catch (Exception ex) {
			collection = null;
			LOGGER.warn("MongoDB connection disabled: {}", ex.getMessage());
		}
	}

	public static void save(ChatMessage message) {
		if (collection == null) {
			return;
		}
		try {
			collection.insertOne(message.toDocument());
		} catch (Exception ex) {
			LOGGER.error("Failed to persist chat message", ex);
		}
	}

	public static List<ChatMessage> findRecentMessages(int limit) {
		if (collection == null) {
			return List.of();
		}
		try {
			List<ChatMessage> messages = new ArrayList<>();
			collection.find(Filters.exists("sender"))
					.sort(Sorts.descending("timestamp"))
					.limit(limit)
					.forEach(document -> messages.add(ChatMessage.fromDocument(document)));
			messages.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
			return messages;
		} catch (Exception ex) {
			LOGGER.error("Failed to retrieve chat history", ex);
			return List.of();
		}
	}
}
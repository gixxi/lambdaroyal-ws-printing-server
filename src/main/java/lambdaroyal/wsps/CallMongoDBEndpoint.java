package lambdaroyal.wsps;

import static com.mongodb.client.model.Filters.eq;

import java.util.HashMap;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Repository
public class CallMongoDBEndpoint {
	
    public static String websocketUrlCheck(HashMap<String, String> mongoDBUrlDetails, String serverName) {
        String uri = mongoDBUrlDetails.get("url");
        String databaseName = mongoDBUrlDetails.get("databaseName");
        String collectionName = mongoDBUrlDetails.get("collectionName");
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            Document doc = collection.find(eq("server", serverName)).first();
            String wsUrl = doc.get("ws-urls").toString();
            
          

            
            return wsUrl;
        }
    }
}

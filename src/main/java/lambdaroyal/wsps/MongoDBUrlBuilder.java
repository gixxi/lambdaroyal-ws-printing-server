package lambdaroyal.wsps;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Repository
public class MongoDBUrlBuilder {
	
    public static HashMap<String, String> mongoDBConfigRequest(String sURL) {

        URL url;
		try {
			url = new URL(sURL);
	        URLConnection request = url.openConnection();
	        request.connect();

	        JsonParser jp = new JsonParser(); 
	        JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
	        JsonObject jsonObject = root.getAsJsonObject();
	        HashMap<String, String> fullUrl = generateUrl(jsonObject);
	        return fullUrl;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new HashMap<String, String>();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new HashMap<String, String>();
		}

    }
    
    private static HashMap<String, String> generateUrl(JsonObject jsonObject) {
    	String password = jsonObject.get("password").getAsString();
    	String username = jsonObject.get("username").getAsString();
    	String preMongoDBUrl = jsonObject.get("pre-mongoDBUrl").getAsString();
    	String postMongoDBUrl = jsonObject.get("post-mongoDBUrl").getAsString();
    	String databaseName = jsonObject.get("databaseName").getAsString();
    	String collectionName = jsonObject.get("collectionName").getAsString();
    	
    	HashMap<String, String> mongoDBConfigs = new HashMap<String, String>();
    	mongoDBConfigs.put("url", preMongoDBUrl + username + ":" + password + postMongoDBUrl + databaseName);
    	mongoDBConfigs.put("collectionName", collectionName);
    	mongoDBConfigs.put("databaseName", databaseName);

    	return mongoDBConfigs;
    }
    
}

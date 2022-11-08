package lambdaroyal.wsps.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.stereotype.Repository;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This class uses the url param from the context to get some JSON from
 * Planet-Rocklog's /system/info service which includes the websocket endpoint
 * 
 * @author gix
 *
 */
@Repository
public class SystemInfoParser {
	public static String getWebsocketEndpoint(String sURL) throws IOException {

		URL url;
		url = new URL(sURL);
		URLConnection request = url.openConnection();
		request.addRequestProperty("User-Agent", "Mozilla");
		request.setReadTimeout(5000);
		request.setConnectTimeout(5000);

		request.connect();

		JsonParser jp = new JsonParser();
		JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
		JsonObject jsonObject = root.getAsJsonObject();
		JsonObject general = jsonObject.get("general").getAsJsonObject();
		JsonElement wssEndpoint = general.get("wss-endpoint");
		if (wssEndpoint == null) {
			throw new IllegalStateException(String.format(
					"JSON document from %s MUST contain the websocket URL wss://... that is used to establish a websocket connection to Planet-Rocklog",
					sURL));
		}
		return wssEndpoint.getAsString();
	}
}

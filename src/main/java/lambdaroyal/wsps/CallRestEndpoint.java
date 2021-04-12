package lambdaroyal.wsps;
import java.io.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketMessageHandler;

/**
 * receives print requests from the server and tries to assign them to printing
 * services
 * 
 * @author gix
 *
 */
@Repository
public class CallRestEndpoint implements IWebsocketMessageHandler {
	private static final Logger logger = LoggerFactory.getLogger(CallRestEndpoint.class);
	
	@Autowired
	private Context context;
	
	@Override
	public void onMessage(String message) {
		ObjectMapper om = new ObjectMapper();
		try {
			HashMap<String, Object> map = om.readValue(message, HashMap.class);
			if ("call-internal-proxy-endpoint".equals(map.get("fn"))) {

				String urlString =  (String) map.get("url");
				String method =  (String) map.get("method");
				String uid = (String) map.get("uid");
				URL url = new URL(urlString);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod(method);
				int code = con.getResponseCode();
				logger.info(String.format("received rest call request and returned status code: %d uid: %s",
						code, uid));
		
				Map<String, String> req = new HashMap<>();
				req.put("fn", "ws-handler");
				req.put("event", "internal-proxy-handler");
				req.put("sessionId", Context.getSessionId());
				req.put("jwt", context.getWebtoken());
				req.put("uid", uid);
				req.put("code", "" + code);
				try {
					context.websocketClientEndpoint
							.sendMessage(om.writeValueAsString(req));
				} catch (JsonProcessingException e) {
					logger.error("Failed to generate request", e);
				}

			}
		} catch (IOException e) {
			logger.error("IO Exception occured", e);
		}
	}
}

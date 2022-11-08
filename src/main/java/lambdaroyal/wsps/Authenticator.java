package lambdaroyal.wsps;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketMessageHandler;

/**
 * checks every 5 minutes whether the JSON webtoken is still valid
 * @author gix
 *
 */
@Repository
public class Authenticator extends TimerTask implements IWebsocketMessageHandler {
	private static final Logger logger = LoggerFactory.getLogger(Authenticator.class);

	@Autowired
	private Context context;

	private boolean authenticated = false;

	public void start() {
		new Timer(true).schedule(this, 15000, 60000);
	}

	@Override
	public void run() {
		// we only authenticate if necessary
		if (context.getWebtoken() != null) {
			//reset authentication status - jwt might be outdated
			authenticated = false;
			if (context.getWebsocketClientEndpoint() != null) {
				logger.info(String.format("Try to authenticate session [%s] using webtoken", context.getSessionId()));
				ObjectMapper om = new ObjectMapper();
				Map<String, String> req = new HashMap<>();
				req.put("fn", "register-jwt");
				req.put("event", "user");
				req.put("sessionId", Context.getSessionId());
				req.put("jwt", context.getWebtoken());
				try {
					context.getWebsocketClientEndpoint()
							.sendMessage(om.writeValueAsString(req));
				} catch (JsonProcessingException e) {
					logger.error("Failed to generate request", e);
				}
			}
		}
		
		//if we don't have a webtoken, we assume no authentication is necessary
		authenticated = true;
	}

	@Override
	public void onMessage(String message) {
		ObjectMapper om = new ObjectMapper();
		try {
			HashMap<String, Object> map = om.readValue(message, HashMap.class);
			if("user".equals(map.get("event")) && "register-jwt-response".equals(map.get("fn")) && context.getSessionId().equals(map.get("sessionId"))) {
				logger.info("JSON Webtoken is valid. Session authenticated.");
				authenticated = true;
			}
		} catch (IOException e) {
		}
	}
}

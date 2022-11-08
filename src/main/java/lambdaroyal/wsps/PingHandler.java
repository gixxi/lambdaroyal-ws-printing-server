package lambdaroyal.wsps;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketMessageHandler;

@Repository
public class PingHandler implements IWebsocketMessageHandler  {
	private static final Logger logger = LoggerFactory.getLogger(PingHandler.class);

	@Autowired
	private Context context;
	
	@Autowired 
	private Queue<String> queue;
	
	@Override
	public void onMessage(String message) {
		ObjectMapper om = new ObjectMapper();
		HashMap<String, Object> map;
		try {
			map = om.readValue(message, HashMap.class);
			if ("printing-server-ping".equals(map.get("fn"))) {	
				System.out.println("Called");
				Map<String, String> req = new HashMap<>();
				req.put("queue-size", String.valueOf(queue.size()));
				req.put("sessionId", Context.getSessionId());

				try {
					context.getWebsocketClientEndpoint().sendMessage(om.writeValueAsString(req));
				} catch (JsonProcessingException e) {
					logger.error("Failed to generate request", e);
				}
			}
		} catch (IOException e) {
			logger.error("IO Exception occured", e);
		}
	}
}
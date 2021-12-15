package lambdaroyal.wsps;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class Connector extends TimerTask {
	private static final Logger logger = LoggerFactory.getLogger(Connector.class);

	@Autowired
	private Context context;

	@Autowired
	private WebsocketMessageHandler websocketMessageHandler;

	private boolean connected = false;

	public void start() {
		new Timer(true).schedule(this, 0, 10000);

	}

	public boolean isConnected() {
		return connected;
	}

	@Override
	public void run() {
		String currentWebSocketUrl = context.getWebsocketUrl();
		String newWebSocketUrl = context.getNewWebSocketUrl();
		if (context.websocketClientEndpoint == null || !currentWebSocketUrl.equals(newWebSocketUrl)) {
			try {
				lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketHandler handler = new lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketHandler() {

					@Override
					public void onOpen(Session userSession) {
						logger.info("Connected to " + newWebSocketUrl);
						connected = true;
					}

					@Override
					public void onMessage(String message) {
						websocketMessageHandler.onMessage(message);
					}

					@Override
					public void onClose(Session userSession, CloseReason reason) {
						logger.warn("Disconnected from " + newWebSocketUrl);
						context.websocketClientEndpoint = null;
						connected = false;
					}
				};

				context.websocketClientEndpoint = new WebsocketClientEndpoint(new URI(newWebSocketUrl),
						handler);
				context.setWebsocketUrl(newWebSocketUrl);
				
			} catch (URISyntaxException e) {
				logger.error(String.format("%s has wrong URI syntax"));
				System.exit(-1);
			} catch (RuntimeException e) {
				logger.error("Failed to connect to " + newWebSocketUrl, e);
			}
		}
	}
}

package lambdaroyal.wsps;

import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class Connector implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(Connector.class);

	@Autowired
	private Context context;
	
	@Autowired
	private WebsocketMessageHandler websocketMessageHandler;

	private boolean connected = false;

	public void start() {
		new Thread(this).start();

	}

	public boolean isConnected() {
		return connected;
	}
	
	@Override
	public void run() {
		while (true) {
			if (context.websocketClientEndpoint == null) {
				try {
					logger.info("Try to connect to " + context.getWebsocketUrl());
					lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketHandler handler = new lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketHandler() {

						@Override
						public void onOpen(Session userSession) {
							logger.info("Connected to " + context.getWebsocketUrl());
							connected = true;
						}

						@Override
						public void onMessage(String message) {
							websocketMessageHandler.onMessage(message);
						}

						@Override
						public void onClose(Session userSession, CloseReason reason) {
							logger.warn("Disconnected from " + context.getWebsocketUrl());
							context.websocketClientEndpoint = null;
							connected = false;
						}
					};

					context.websocketClientEndpoint = new WebsocketClientEndpoint(new URI(context.getWebsocketUrl()),
							handler);
				} catch (URISyntaxException e) {
					logger.error(String.format("%s has wrong URI syntax"));
					System.exit(-1);
				} catch (RuntimeException e) {
					logger.error("Failed to connect to " + context.getWebsocketUrl(), e);
				}
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

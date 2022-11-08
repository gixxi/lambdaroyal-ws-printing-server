package lambdaroyal.wsps;

import java.io.IOException;
import java.net.URI;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChatServer Client
 *
 */
@ClientEndpoint
public class WebsocketClientEndpoint {
	private Session userSession = null;
	private static final Logger logger = LoggerFactory.getLogger(WebsocketClientEndpoint.class);
	final IWebsocketHandler handler;	
	
	public Session getUserSession() {
		return userSession;
	}

	public void setUserSession(Session userSession) {
		this.userSession = userSession;
	}

	public interface IWebsocketMessageHandler {
		public void onMessage(String message);

	}

	public interface IWebsocketHandler extends IWebsocketMessageHandler {
		void onOpen(Session userSession);

		public void onClose(Session userSession, CloseReason reason);

	}


	public WebsocketClientEndpoint(URI endpointURI, IWebsocketHandler handler) {
		// fail fast
		if (handler == null) {
			throw new IllegalArgumentException("Cannot instantiate WebsocketClientEndpoint without IWebsocketHandler");
		}
		try {
			this.handler = handler;
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			userSession = container.connectToServer(this, endpointURI);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void close() {
		try {
			if (getUserSession() != null) {
				getUserSession().close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		
	}

	/**
	 * Callback hook for Connection open events.
	 *
	 * @param userSession
	 *            the userSession which is opened.
	 */
	@OnOpen
	public void onOpen(Session userSession) {
		logger.info("onOpen");
		this.userSession = userSession;
		handler.onOpen(userSession);
	}

	/**
	 * Callback hook for Connection close events.
	 *
	 * @param userSession
	 *            the userSession which is getting closed.
	 * @param reason
	 *            the reason for connection close
	 */
	@OnClose
	public void onClose(Session userSession, CloseReason reason) {
		logger.info("onClose");
		this.userSession = null;
		handler.onClose(userSession, reason);
	}

	/**
	 * Callback hook for Message Events. This method will be invoked when a client
	 * send a message.
	 *
	 * @param message
	 *            The text message
	 */
	@OnMessage
	public void onMessage(String message) {
		handler.onMessage(message);
	}

	/**
	 * Send a message.
	 *
	 * @param message
	 */
	public void sendMessage(String message) {
		synchronized (this) {
			this.userSession.getAsyncRemote().sendText(message);
		}
	}

}
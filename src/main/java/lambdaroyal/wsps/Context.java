package lambdaroyal.wsps;

import java.util.Random;

import org.springframework.stereotype.Repository;

import lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketHandler;

@Repository
final class Context {
	private static String sessionId;
	private String websocketUrl;
	private String serverName;
	private String webtoken;
	private long printerFetchInterval;
	private String rocklogServerName;
	private String proxyUrl;
	private String newWebSocketUrl;
	
	public String getNewWebSocketUrl() {
		return newWebSocketUrl;
	}

	public void setNewWebSocketUrl(String newWebSocketUrl) {
		this.newWebSocketUrl = newWebSocketUrl;
	}

	public String getProxyUrl() {
		return proxyUrl;
	}

	public void setProxyUrl(String proxyUrl) {
		this.proxyUrl = proxyUrl;
	}

	public String getRocklogServerName() {
		return rocklogServerName;
	}

	public void setRocklogServerName(String rocklogServerName) {
		this.rocklogServerName = rocklogServerName;
	}

	WebsocketClientEndpoint websocketClientEndpoint;

	static {
		String candidateChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		Random rand = new Random();
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < 17; i++) {
			int randIndex = rand.nextInt(candidateChars.length());
			res.append(candidateChars.charAt(randIndex));
		}
		sessionId = res.toString();
	}

	public static String getSessionId() {
		return sessionId;
	}

	public String getWebsocketUrl() {
		return websocketUrl;
	}

	public void setWebsocketUrl(String websocketUrl) {
		this.websocketUrl = websocketUrl;
	}

	public String getWebtoken() {
		return webtoken;
	}

	public void setWebtoken(String webtoken) {
		this.webtoken = webtoken;
	}

	public long getPrinterFetchInterval() {
		return printerFetchInterval;
	}

	public void setPrinterFetchInterval(long printerFetchInterval) {
		this.printerFetchInterval = printerFetchInterval;
	}

	public WebsocketClientEndpoint getWebsocketClientEndpoint() {
		return websocketClientEndpoint;
	}

	public void setWebsocketClientEndpoint(WebsocketClientEndpoint websocketClientEndpoint) {
		this.websocketClientEndpoint = websocketClientEndpoint;
	}

	public IWebsocketHandler getWebsocketMessageHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getServerName() {
		return serverName;
	}

}

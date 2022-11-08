package lambdaroyal.wsps;

import java.util.Random;

import org.springframework.stereotype.Repository;

import lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketHandler;

@Repository
public final class Context {
	private static String sessionId;
	private String mostRecentPrintingStream;
	private String websocketUrl;
	private String serverName;
	private String systemInfoUrl;
	private String webtoken;
	private long printerFetchInterval;
	private WebsocketClientEndpoint websocketClientEndpoint;
	private boolean shutdownRequested = false;	
	
	public Context() {
    	Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
            	System.out.println("Shutdown requested");
            	Context.this.shutdownRequested = true;
            }
        });

	}
	
	public boolean isShutdownRequested() {
		return shutdownRequested;
	}
	
	public String getSystemInfoUrl() {
		return systemInfoUrl;
	}

	public void setSystemInfoUrl(String systemInfoUrl) {
		this.systemInfoUrl = systemInfoUrl;
	}


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
		return null;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getServerName() {
		return serverName;
	}

	public boolean getWebsocketConnectionEstablished() {
		return this.websocketClientEndpoint != null && this.websocketClientEndpoint.getUserSession() != null;
	}

	public String getMostRecentPrintingStream() {
		return mostRecentPrintingStream;
	}

	public void setMostRecentPrintingStream(String mostRecentPrintingStream) {
		this.mostRecentPrintingStream = mostRecentPrintingStream;
	}
}

package lambdaroyal.wsps;

import java.util.LinkedList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketMessageHandler;

@Repository
public class WebsocketMessageHandler implements IWebsocketMessageHandler {
	@Autowired
	private Authenticator authenticator;
	
	@Autowired 
	private PrinterSpooler printerSpooler;
	@Autowired
	private CallRestEndpoint callRestEndpoint;
	
	@Autowired
	private PrintServiceRepository printServiceRepository;
	
	@Autowired
	private TelegramServer telegramServer;
	
	@Autowired
	private PingHandler pingHandler;
	
	
	
	@Override
	public void onMessage(String message) {
		LinkedList<IWebsocketMessageHandler> handlers = new LinkedList<>();
		handlers.add(authenticator);
		handlers.add(printerSpooler);
		handlers.add(callRestEndpoint);
		handlers.add(printServiceRepository);
		handlers.add(telegramServer);
		handlers.add(pingHandler);
		handlers.stream().forEach(x -> x.onMessage(message));
	}

}

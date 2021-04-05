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
	private PrintServiceRepository printServiceRepository;
	
	
	
	@Override
	public void onMessage(String message) {
		LinkedList<IWebsocketMessageHandler> handlers = new LinkedList<>();
		handlers.add(authenticator);
		handlers.add(printerSpooler);
		handlers.add(printServiceRepository);
		handlers.stream().forEach(x -> x.onMessage(message));
	}

}

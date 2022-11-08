package lambdaroyal.wsps.config;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import lambdaroyal.wsps.Context;
import lambdaroyal.wsps.WebsocketClientEndpoint;

/**
 * Fetches frequently the websocketUrl from the /system/info webservice, once changed closes the current connection and and resets the {@link WebsocketClientEndpoint} in order
 * to force the {@link Connector} to reestablish the connection
 * @author gix
 *
 */
@Repository	
public class WebsocketUrlPolling {	
		@Autowired
		private Context context;
		@Autowired
		private SystemInfoParser systemInfoParser;
		private static final Logger logger = LoggerFactory.getLogger(WebsocketUrlPolling.class);
		public void start() {
			
			new Thread(new Runnable() {
												
				@Override
				public void run() {
					while (!context.isShutdownRequested()) {
						String websocketEndpoint;
						try {
							websocketEndpoint = systemInfoParser.getWebsocketEndpoint(context.getSystemInfoUrl());
							if(!websocketEndpoint.isEmpty() && (context.getWebsocketUrl() == null || !websocketEndpoint.equals(context.getWebsocketUrl()))) {
								context.setWebsocketUrl(websocketEndpoint);
								if(context.getWebsocketClientEndpoint() != null) {
									context.getWebsocketClientEndpoint().close();
								}
							}
						} catch (IOException e) {
							logger.error(String.format("Failed to get a websocket URL from Planet-Rocklog's /system/info webservice %s", context.getSystemInfoUrl()));
							e.printStackTrace();
							try {
								Thread.sleep(10000);
							} catch (InterruptedException e1) {
								logger.error("Failed to sleep", e1);
							}
						}
					}
					logger.info("shutdown WebsocketUrlPolling");
				}
			}).start();
		}


}

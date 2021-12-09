package lambdaroyal.wsps;


import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class WebsocketPolling {
		@Autowired
		private Context context;

		
		private String ws;
	
		public void start() {
			
			new Thread(new Runnable() {
				
				private WebsocketPolling websocketPolling;
				
		        public Runnable init(WebsocketPolling websocketPolling) {
		            this.websocketPolling = websocketPolling;
		            return this;
		        }
				
				@Override
				public void run() {
					while (true) {
						HashMap<String, String> mongoDBUrlDetails = MongoDBUrlBuilder.mongoDBConfigRequest(context.getProxyUrl());
						if(!mongoDBUrlDetails.isEmpty()) {
							String receivedWs = CallMongoDBEndpoint.websocketUrlCheck(mongoDBUrlDetails, context.getRocklogServerName(), context.getRocklogSystemUid());
							if (ws != receivedWs && receivedWs != "Error") {
								websocketPolling.ws = receivedWs;
								context.setNewWebSocketUrl(receivedWs);
							}
							try {
								Thread.sleep(30000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			}.init(this)).start();
		}


}

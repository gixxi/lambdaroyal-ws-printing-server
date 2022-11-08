package lambdaroyal.wsps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

// Need to make sure accept() socket must be in a loop with expectation handling\
// Once we get a socket we need to register this socket
// We need a a callback for when the socket is closed.
// Once the telegram server gets something from Rocklog, we need to send it to all the sockets that are open. (usually just one socket)
// Need to make port configurable, pass it in from arguments in Configuration.

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Base64Utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketMessageHandler;

@Repository
public class TelegramServer implements IWebsocketMessageHandler {
	@Autowired
	private Context context;
	@Autowired
	private Queue<String> queue;
	@Autowired
	private Queue<String> telegramServerQueue;
	
	// Queue taking in messages received from the socket and later on sent
	// asynchrounously to Rocklog

	private static final Logger logger = LoggerFactory.getLogger(TelegramServer.class);
	private ServerSocket serverSocket;
	private TelegramClientHandler telegramClientHandler;
	private AtomicInteger runningIndex = new AtomicInteger(1);

	public void start(int telegramPort) {

		// Thread for queue that handles requests from TelegramServer to Rocklog.
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					String head = queue.peek();

					if (head != null) {
						if (processQueueContent(head)) {
							queue.poll();
						} else {
							try {
								logger.debug("[Telegram Server to Rocklog queue] Failed to process queue content");
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								logger.error("Error in Server to Rocklog queue");
								e.printStackTrace();
							}	
						}
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					

				}
			}
		}).start();
		
		// Thread for queue that handles requests from Rocklog to Telegram Server		
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!context.isShutdownRequested()) {
					String head = telegramServerQueue.peek();

					if (head != null) {
						if (processTelegramServerQueueContent(head)) {
							telegramServerQueue.poll();
						} else {
							try {
								logger.debug("[Rocklog to Telegram Server queue] Failed to process queue content");
								Thread.sleep(10000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								logger.error("Error in Server to Rocklog queue");
								e.printStackTrace();
							}	
						}
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					

				}
			}
		}).start();
		
		try {
			serverSocket = new ServerSocket(telegramPort);
		} catch (IOException e) {
			logger.error("Failed to startup TCP/IP for server socket for port ", e);
		}

		Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
            	try {
					serverSocket.close();
				} catch (IOException e) {
					//e.printStackTrace();
				}
            }
        });		
		
		// start a thread that accepts incoming requests
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// Constantly listen for incoming connections, register the latest one we
				// register to be used to send back response to SPS.
				while (!context.isShutdownRequested()) {
					acceptSocketConnections();
				}
						
				logger.info("shutdown TelegramServer");		
			}
		});
		
	}
	
	private synchronized void acceptSocketConnections() {
		Socket newSocket;
		try {
			newSocket = serverSocket.accept();
			try {
				if (telegramClientHandler != null && telegramClientHandler.clientSocket != null) {
					telegramClientHandler.clientSocket.close();
				}
			} catch (IOException e) {
				logger.error("Closing the old socket failed", e);
			}
			telegramClientHandler = new TelegramClientHandler(newSocket, queue);
			telegramClientHandler.run();

		} catch (IOException e1) {
			logger.error("Accepting connection failed", e1);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static class TelegramClientHandler extends Thread {
		private Socket clientSocket;
		private Queue<String> queue;

		public TelegramClientHandler(Socket socket, Queue<String> queue) {
			this.clientSocket = socket;
			this.queue = queue;
		}

		public void run() {
			try {
				logger.info("Client Socket established");

				boolean connectionClosed = false;
				while (!connectionClosed) {
					byte[] inputLine = new byte[100];
					int i = 0;
					int readValue;
					
					try {
						BufferedInputStream in = new BufferedInputStream(clientSocket.getInputStream(), 100);
						for (i = 0; i < 100; i++) {
							readValue = in.read();
							if (readValue == -1) {
								connectionClosed = true;
							}

							if (readValue == 13 || i == 100 || readValue == -1) {
								break;
							}

							inputLine[i] = (byte) readValue;
						}

					} catch (IOException e1) {
						logger.error("Buffer stream failed.", e1);
						logger.info("Closing Connection, waiting for a new one");
						break;
					}
					// Check that we have a valid message to forward to the queue
					if (i > 0) {
						logger.info("Value of inputLine is " + new String(inputLine));
						String encodedBytes = Base64Utils.encodeToString(inputLine);
						insertIntoQueue(encodedBytes, queue);
					}
				}
				
				clientSocket.close();
			} catch (IOException e) {
				logger.error("Forcing client connection to closed failed", e);
				e.printStackTrace();
			} finally {
				logger.info("Client Socket closed");
				clientSocket = null;
			}
		}
	}

	private static void insertIntoQueue(String input, Queue<String> queue) {
		// input into queue
		queue.add(input);
	}

	private boolean processQueueContent(String data) {
		boolean result = false;
		if (context.getWebsocketClientEndpoint() != null) {
			ObjectMapper om = new ObjectMapper();
			Map<String, Object> req = new HashMap<>();
			req.put("fn", "telegram-request");
			req.put("event", "telegram-server-handler");
			req.put("jwt", context.getWebtoken());
			req.put("server", context.getServerName());
			req.put("base64-telegram-data", data);
			req.put("sessionId", Context.getSessionId());
			req.put("running-index", runningIndex.get());

			try {
				context.getWebsocketClientEndpoint().sendMessage(om.writeValueAsString(req));
				logger.info("[Telegram Server -> Rocklog] data sent " + data);
				runningIndex.getAndIncrement();
				result = true;
			} catch (JsonProcessingException e) {
				logger.error("[processQueueContent] Failed to generate request", e);
				result = false;
			}
		}
		return result;
	}
	
	/**
	 * @param data
	 * @return
	 */
	private boolean processTelegramServerQueueContent(String data) {
		boolean result = false;
		if (telegramClientHandler != null && telegramClientHandler.clientSocket != null) {
			ObjectMapper om = new ObjectMapper();
			HashMap<String, Object> map;
			try {
				map = om.readValue(data, HashMap.class);
				String telegramData = (String) map.get("base64-telegram-data");
				BufferedOutputStream out = new BufferedOutputStream(
					telegramClientHandler.clientSocket.getOutputStream());
				byte[] bytes = Base64Utils.decodeFromString(telegramData);
				out.write(bytes);
				out.flush();
				logger.info(String.format("[Rocklog -> Telegram Server] data sent %s", data));
				result = true;

			} catch (JsonParseException e) {
				logger.error("[processTelegramServerQueueContent] Failed to parse Json from message " + data , e);
			} catch (JsonMappingException e) {
				logger.error("[processTelegramServerQueueContent] Failed to map from Json from message " + data , e);
			} catch (IOException e) {
				logger.error("[processTelegramServerQueueContent] Failed to operate on stream from message " + data , e);
			}
		}
		return result;
	}

	@Override
	public void onMessage(String message) {
		// check whether a client is already connected
		ObjectMapper om = new ObjectMapper();
		HashMap<String, Object> map;
		try {
			map = om.readValue(message, HashMap.class);
			if ("telegram-request".equals(map.get("fn"))) {
				telegramServerQueue.add(message);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}

package lambdaroyal.wsps;

import java.io.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketMessageHandler;

/**
 * receives print requests from the server and tries to assign them to printing
 * services
 * 
 * @author gix
 *
 */
@Repository
public class CallRestEndpoint implements IWebsocketMessageHandler {
	private static final Logger logger = LoggerFactory.getLogger(CallRestEndpoint.class);

	@Autowired
	private Context context;

	@Override
	public void onMessage(String message) {
		ObjectMapper om = new ObjectMapper();
		try {
			HashMap<String, Object> map = om.readValue(message, HashMap.class);
			if ("call-internal-proxy-endpoint".equals(map.get("fn"))) {

				String urlString = (String) map.get("url");

				String method = (String) map.get("method");
				String uid = (String) map.get("uid");
				String queryParamsString = (String) map.get("query-params");
				String headerStrings = (String) map.get("headers");
				String body = (String) map.get("body");

				StringBuilder response = new StringBuilder();
				
				logger.info(String.format("url is %s method is %s queryParams is %s headers is %s body is %s", urlString, method, queryParamsString, headerStrings, body));
				
				int code = 200;
				HttpURLConnection con;

				if (method.equals("GET")) {
					URL url = new URL(urlString);
					con = (HttpURLConnection) url.openConnection();
					HashMap<String, String> queryParams = convertStringToHashMap(queryParamsString);
					urlString = assignQueryParametersToUrl(queryParams, urlString);
					url = new URL(urlString);
					con = (HttpURLConnection) url.openConnection();
					con.setRequestMethod(method);

					HashMap<String, String> headers = convertStringToHashMap(headerStrings);
					setHeadersToRequest(con, headers);

					code = con.getResponseCode();
				} else {
					URL url = new URL(urlString);
					con = (HttpURLConnection) url.openConnection();
					con.setRequestMethod(method);
					con.setDoOutput(true);
					HashMap<String, String> headers = convertStringToHashMap(headerStrings);
					setHeadersToRequest(con, headers);
					
					assignBodyToRequest(con, body);
					code = con.getResponseCode();
					try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
						String responseLine = null;
						while ((responseLine = br.readLine()) != null) {
							response.append(responseLine.trim());
						}
						logger.info(response.toString());
					}
				}

				logger.info(
						String.format("received rest call request and returned status code: %d uid: %s", code, uid));

		
				String contentType = con.getContentType();
						
				Map<String, String> req = new HashMap<>();
				req.put("fn", "ws-handler");
				req.put("event", "internal-proxy-handler");
				req.put("sessionId", Context.getSessionId());
				req.put("jwt", context.getWebtoken());
				req.put("uid", uid);
				req.put("code", "" + code);
				req.put("response", response.toString());
				req.put("response-content-type", contentType);
				// add mime-type
				try {
					context.websocketClientEndpoint.sendMessage(om.writeValueAsString(req));
				} catch (JsonProcessingException e) {
					logger.error("Failed to generate request", e);
				}

			}
		} catch (IOException e) {
			logger.error("IO Exception occured", e);
		}
	}

	private String assignQueryParametersToUrl(HashMap<String, String> parameters, String url) {

		if (parameters.isEmpty()) {
			return url;
		} else {
			String queryParams = "?";

			for (String i : parameters.keySet()) {
				queryParams = queryParams + i + "=" + parameters.get(i) + "&";
			}
			return url + queryParams.substring(0, queryParams.length() - 1);
		}

	}

	private void assignBodyToRequest(HttpURLConnection con, String parameters) {
		try (OutputStream os = con.getOutputStream()) {
			byte[] input = parameters.getBytes("utf-8");
			os.write(input, 0, input.length);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private HashMap<String, String> convertStringToHashMap(String string) {
		string = string.substring(1, string.length() - 1);
		if (string.isEmpty()) {
			return new HashMap<>();
		} else {

			String[] keyValuePairs = string.split(" ");
			HashMap<String, String> map = new HashMap<>();

			for (int i = 0; i < keyValuePairs.length; i += 2) {
				map.put(keyValuePairs[i].substring(1, keyValuePairs[i].length()), keyValuePairs[i + 1]);
			}

			return map;
		}
	}

	private void setHeadersToRequest(HttpURLConnection con, HashMap<String, String> requestParameters) {
		for (String key : requestParameters.keySet()) {
			con.setRequestProperty(key, requestParameters.get(key));
		}

	}
}

package lambdaroyal.wsps;

import java.io.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
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
				String[] methodsRequiringBody = new String[]{"POST", "PUT", "PATCH"};
				
				String urlString = (String) map.get("url");
				String method = (String) map.get("method");
				String uid = (String) map.get("uid");
				String queryParamsString = (String) map.get("query-params");
				String headerStrings = (String) map.get("headers");
				String body = (String) map.get("body");
				

				StringBuilder response = new StringBuilder();
				
				
				int code = 200;
				HttpURLConnection con;
				
				HashMap<String, String> queryParams = convertStringToHashMap(queryParamsString);
				HashMap<String, String> headers = convertStringToHashMap(headerStrings);
				

				URL url = new URL(urlString);
				urlString = assignQueryParametersToUrl(queryParams, urlString);
				
				con = (HttpURLConnection) url.openConnection();
				
				setHeadersToRequest(con, headers);

				
				con.setRequestMethod(method);
				if (Arrays.asList(methodsRequiringBody).contains(method)) {
					con.setDoOutput(true);
					assignBodyToRequest(con, body);
	
					try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
						String responseLine = null;
						while ((responseLine = br.readLine()) != null) {
							response.append(responseLine.trim());
						}
						logger.info(response.toString());
					}
				}
				
				code = con.getResponseCode();
				logger.info("" + code);
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
				try {
					context.getWebsocketClientEndpoint().sendMessage(om.writeValueAsString(req));
				} catch (JsonProcessingException e) {
					logger.error("Failed to generate request", e);
				}

			}
		} catch (IOException e) {
			logger.error("IO Exception occured", e);
			ObjectMapper omErr = new ObjectMapper();
			Map<String, String> errReq = new HashMap<>();
			errReq.put("errors", e.toString());
			try {
			   context.getWebsocketClientEndpoint().sendMessage(omErr.writeValueAsString(errReq));
			} catch (JsonProcessingException jsonErr) {
				logger.error("Failed to generate request", jsonErr);
			}
			
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
			e.printStackTrace();
		}
	}

	private HashMap<String, String> convertStringToHashMap(String string) {
		if (string == null) {
			return new HashMap<>();
		}
		
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

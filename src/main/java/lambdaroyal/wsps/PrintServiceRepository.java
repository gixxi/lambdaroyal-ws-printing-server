package lambdaroyal.wsps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lambdaroyal.wsps.WebsocketClientEndpoint.IWebsocketMessageHandler;

@Repository
public class PrintServiceRepository extends TimerTask implements IWebsocketMessageHandler {
	private static final Logger logger = LoggerFactory.getLogger(PrintServiceRepository.class);

	private ConcurrentHashMap<String, ConcurrentHashMap<String, PrintService>> printServices = new ConcurrentHashMap<>();

	//this gets started once the session is authenticated
	private Timer timer = null;
	
	@Autowired
	private Context context;

	{
		printServices.put(DocFlavor.BYTE_ARRAY.AUTOSENSE.toString(), new ConcurrentHashMap<>());
		printServices.put(DocFlavor.INPUT_STREAM.PDF.toString(), new ConcurrentHashMap<>());
	}

	public void start() {
	}

	PrintService getPrintService(DocFlavor docFlavor, String printerName) {
		
		ConcurrentHashMap<String, PrintService> xs = printServices.get(docFlavor.toString());
		if(xs != null) {
			return xs.get(printerName);			
		}
		return null;
	}
	
	public List<String> getAllPrinters() {
		return Arrays.asList(DocFlavor.BYTE_ARRAY.AUTOSENSE, DocFlavor.INPUT_STREAM.PDF).stream()
		.map(x -> {
			return Arrays.asList(PrintServiceLookup.lookupPrintServices(x, null)).stream()
					.map(PrintService::getName).collect(Collectors.toList());
		})
		.filter(x->x != null)
		.reduce(new ArrayList<String>(), (acc, z) -> {acc.addAll(z);
			return acc;}).stream()
		.distinct()
		.sorted()
		.toList();
	}
	
	@Override
	public void run() {
		Arrays.asList(DocFlavor.BYTE_ARRAY.AUTOSENSE, DocFlavor.INPUT_STREAM.PDF).stream().parallel()
				.forEach(docFlavor -> {
					PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();
					attributeSet.add(new Copies(1));
					Map<String, PrintService> services = Arrays
							.asList(PrintServiceLookup.lookupPrintServices(docFlavor, null)).stream()
							.collect(Collectors.toMap(PrintService::getName, Function.identity()));

					ConcurrentHashMap<String, PrintService> bucket = printServices.get(docFlavor.toString());

					// remove all those from bucket that are not resolved
					bucket.entrySet().stream().filter(entry -> !services.containsKey(entry.getKey())).forEach(x -> {
						logger.info("Removing printer service for " + x.getKey());
						bucket.remove(x.getKey());
					});

					// add all others
					services.entrySet().stream().forEach(x -> {
						if (!bucket.containsKey(x.getKey())) {
							logger.info(String.format("Adding new printer service flavor: %s name: %s ",
									docFlavor.toString(), x.getKey()));

						}
						bucket.put(x.getKey(), x.getValue());
					});
					
					//register all printers with the host at once, since the host needs to know which printers are obsolete
					if (context.getWebsocketClientEndpoint() != null) {
						ObjectMapper om = new ObjectMapper();
						Map<String, Object> req = new HashMap<>();
						req.put("fn", "register-printers");
						req.put("event", "printing");
						req.put("sessionId", Context.getSessionId());
						req.put("jwt", context.getWebtoken());
						req.put("server", context.getServerName());						
						
						List<Map<String, String>> printers = new LinkedList<>();
						printServices.entrySet().stream().forEach(entry -> {
							String mimeType = entry.getKey();
							entry.getValue().entrySet().stream().forEach(x -> {
								Map<String, String> printer = new HashMap<>();
								printer.put("doc-flavor", mimeType);
								printer.put("name", x.getValue().getName());
								printers.add(printer);
							});
						});
						req.put("printers", printers);
						
						try {
							context.getWebsocketClientEndpoint().sendMessage(om.writeValueAsString(req));
						} catch (JsonProcessingException e) {
							logger.error("Failed to generate request", e);
						}
					}

					
				});
	}

	@Override
	public void onMessage(String message) {
		ObjectMapper om = new ObjectMapper();
		try {
			HashMap<String, Object> map = om.readValue(message, HashMap.class);
			if("user".equals(map.get("event")) && "register-jwt-response".equals(map.get("fn")) && context.getSessionId().equals(map.get("sessionId"))) {
				logger.info("Session authenticated. Register printers.");
				synchronized (this) {
					if(timer == null) {
						timer = new Timer(true);
						timer.schedule(this, 0, context.getPrinterFetchInterval() * 1000);
					}					
				}
			}
		} catch (IOException e) {
		}

		
	}

}

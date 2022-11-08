package lambdaroyal.wsps;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.SimpleDoc;
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

/**
 * receives print requests from the server and tries to assign them to printing
 * services
 * 
 * @author gix
 *
 */
@Repository
public class PrinterSpooler implements IWebsocketMessageHandler {
	private static final Logger logger = LoggerFactory.getLogger(PrinterSpooler.class);
	
	private enum PRINTING_TYPES {
			LABELPRINTER_VLIC("Labelprinter Language", DocFlavor.BYTE_ARRAY.AUTOSENSE), 
			PDF_VLIC("LaTeX/PDF", DocFlavor.INPUT_STREAM.PDF), 
			LABELPRINTER_JAVA(DocFlavor.BYTE_ARRAY.AUTOSENSE.toString(), DocFlavor.BYTE_ARRAY.AUTOSENSE), 
			PDF_JAVA(DocFlavor.INPUT_STREAM.PDF.toString(), DocFlavor.INPUT_STREAM.PDF);

		static PRINTING_TYPES parse(String key) {
			for (PRINTING_TYPES x : PRINTING_TYPES.values()) {
				if (x.key.equals(key)) {
					return x;
				}
			}
			return null;
		}

		PRINTING_TYPES(String key, DocFlavor flavor) {
			this.key = key;
			this.flavor = flavor;
		}

		private final String key;
		private final DocFlavor flavor;

	}

	@Autowired
	PrintServiceRepository printServiceRepository;

	@Autowired
	private Context context;

	private void sendError(String job, String msg) {
		ObjectMapper om = new ObjectMapper();
		Map<String, String> req = new HashMap<>();
		req.put("fn", "printing-failed");
		req.put("event", "printing");
		req.put("sessionId", Context.getSessionId());
		req.put("jwt", context.getWebtoken());
		req.put("job", job);
		req.put("msg", msg);
		try {
			context.getWebsocketClientEndpoint()
					.sendMessage(om.writeValueAsString(req));
		} catch (JsonProcessingException e) {
			logger.error("Failed to generate request", e);
		}
	}

	private void sendOk(String job) {
		ObjectMapper om = new ObjectMapper();
		Map<String, String> req = new HashMap<>();
		req.put("fn", "printing-ok");
		req.put("event", "printing");
		req.put("sessionId", Context.getSessionId());
		req.put("jwt", context.getWebtoken());
		req.put("job", job);
		try {
			context.getWebsocketClientEndpoint()
					.sendMessage(om.writeValueAsString(req));
		} catch (JsonProcessingException e) {
			logger.error("Failed to generate request", e);
		}
	}

	@Override
	public void onMessage(String message) {
		ObjectMapper om = new ObjectMapper();
		try {
			HashMap<String, Object> map = om.readValue(message, HashMap.class);
			if ("printing".equals(map.get("event")) && "print-to-server".equals(map.get("fn"))
					&& context.getSessionId().equals(map.get("sessionId")) && map.containsKey("data")
					&& map.containsKey("printer") && map.containsKey("printing-type")) {
				String job = (String) map.get("job");

				String data = (String) map.get("data");
				logger.info(String.format("received print request printer: %s doc-flavor: %s number of characters: %d",
						map.get("printer").toString(), map.get("printing-type"), data.length()));

				context.setMostRecentPrintingStream(data);
				
				// try map printing type
				String reqPrintingType = map.get("printing-type").toString();
				PRINTING_TYPES type = PRINTING_TYPES.parse(reqPrintingType);

				if (type == null) {
					String msg = "unknown printing-type " + reqPrintingType + ". Cannot match DocFlavor.";
					logger.error(msg);
					sendError(job, msg);
					return;
				}

				// typ to resolve printing service based on printer name
				String reqPrinterName = map.get("printer").toString();
				PrintService printService = printServiceRepository.getPrintService(type.flavor, reqPrinterName);
				if (printService == null) {
					String msg = "unknown printer " + reqPrinterName;
					logger.error(msg);
					sendError(job, msg);
					return;
				}

				// try to print
				
				logger.info(new StringBuilder("data").append("\n").append(data).toString());
				DocPrintJob printJob = printService.createPrintJob();
				SimpleDoc doc = new SimpleDoc(data.getBytes(), type.flavor, null);
				PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();
				attributeSet.add(new Copies(1));
				try {
					printJob.print(doc, attributeSet);
				} catch (PrintException e) {
					logger.error(e.getMessage(), e);
					sendError(job, 	e.getMessage());
				}
				sendOk(job);

			}
		} catch (IOException e) {
			logger.error("IO Exception occured", e);
		}
	}
}

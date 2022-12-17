package lambdaroyal.wsps;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import lambdaroyal.wsps.config.Connector;
import lambdaroyal.wsps.config.WebsocketUrlPolling;
import lambdaroyal.wsps.controller.InfoController;



@SpringBootApplication
public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);	
	private static CommandLine cmd;
	
	@Autowired
	private Context context;
	
	@Autowired
	private Connector connector;
	
	@Autowired
	private Authenticator authenticator;
	
	@Autowired
	private PrintServiceRepository printServiceRepository;
	
	@Autowired
	private TelegramServer telegramServer;
	
	@Autowired
	private WebsocketUrlPolling websocketPolling;
	
	@Autowired
	private InfoController infoController;
	
	/**
	 * parsing all the programm arguments and starting the state machine (CONNECT -> AUTHORIZE -> WAIT)
	 * @throws ParseException 
	 * @throws IOException 
	 */
	private static void cli(String[] args) throws ParseException, IOException {
    	Options options = new Options();
    	options.addOption("s", "servername", true, "logical printer server name");
    	options.addOption("i", "interval", true, "time (sec) after all printers are checked again for availability");
    	options.addOption("jwt", "jsonwebtoken", true, "file containing a JSON webtoken that might be used to check authorisation by the server");
    	options.addOption("tsp", "telegramserverport", true, "Port number to receive telegram requests");
    	options.addOption("url", "systeminfourl", true, "url to fetch the /system/info from a Planet-Rocklog server that contains the websocket endpoint");
    	options.addOption("pe", "printerencoding", true, "encoding (e.g. ISO-8859-1, UTF-8) used to get a byte stream send to a printer");
    	
    	CommandLineParser parser = new DefaultParser();		
    	cmd = parser.parse( options, args);    	
    	
        SpringApplication.run(Main.class, args);	
	}
	
	@PostConstruct
	private void postConstruct() throws IOException {
		String serverName = cmd.getOptionValue("s", InetAddress.getLocalHost().getHostName());	
		String systemInfoUrl = cmd.getOptionValue("url");
		
		context.setServerName(serverName);
		context.setSystemInfoUrl(systemInfoUrl);
    	logger.info(String.format("using server name ", serverName));
		
    	long interval = Long.parseLong(cmd.getOptionValue("i", "60"));
    	logger.info(String.format("refreshing list of active printers every %d seconds", interval));
		context.setPrinterFetchInterval(interval);
    	logger.info(String.format("using websocket URL: %s", cmd.getOptionValue("ws")));
    	String jwtFileName = cmd.getOptionValue("jwt");
    	if(jwtFileName != null) {
    		logger.info(String.format("using JSON webtoken from: %s existing: %b", jwtFileName, new File(jwtFileName).exists()));
    	}
		
    	byte[] encoded = jwtFileName != null ? Files.readAllBytes(Paths.get(jwtFileName)) : null;
    	String jwt = encoded != null ? new String(encoded, "UTF-8") : null;
    	
    	
		// Create the encoder and decoder for ISO-8859-1
		Charset charset = Charset.forName(cmd.getOptionValue("pe", "UTF-8"));
		logger.info(String.format("using printer byte array encoding %s", charset.displayName()));
		CharsetEncoder encoder = charset.newEncoder();
		context.setPrinterByteArrayEncoder(encoder);

    	
		context.setWebtoken(jwt);
		websocketPolling.start();
		connector.start();
		authenticator.start();
		printServiceRepository.start();

		
		String telegramServerPort = cmd.getOptionValue("tsp");
		if (telegramServerPort != null) {
			telegramServer.start((Integer.parseInt(telegramServerPort)));
		}
	}
	
    public static void main(String[] args) throws ParseException, IOException {
    	cli(args);
    	
    }
}

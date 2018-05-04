package lambdaroyal.wsps;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

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
	
	
	/**
	 * parsing all the programm arguments and starting the state machine (CONNECT -> AUTHORIZE -> WAIT)
	 * @throws ParseException 
	 * @throws IOException 
	 */
	private static void cli(String[] args) throws ParseException, IOException {
    	Options options = new Options();
    	options.addOption("s", "servername", true, "logical printer server name");
    	options.addRequiredOption("ws", "websocketurl", true, "URL of the websocket where we get artifacts to print");
    	options.addOption("i", "interval", true, "time (sec) after all printers are checked again for availability");
    	options.addOption("jwt", "jsonwebtoken", true, "file containing a JSON webtoken that might be used to check authorisation by the server");
    		
    	CommandLineParser parser = new DefaultParser();		
    	cmd = parser.parse( options, args);    	

        SpringApplication.run(Main.class, args);	
	}
	
	@PostConstruct
	private void postConstruct() throws IOException {
		String serverName = cmd.getOptionValue("s", InetAddress.getLocalHost().getHostName());	
		context.setServerName(serverName);
    	logger.info(String.format("using server name ", serverName));
		
    	long interval = Long.parseLong(cmd.getOptionValue("i", "5"));
    	logger.info(String.format("refreshing list of active printers every %d seconds", interval));
		context.setPrinterFetchInterval(interval);
    	logger.info(String.format("using websocket URL: %s", cmd.getOptionValue("ws")));
    	String jwtFileName = cmd.getOptionValue("jwt");
    	if(jwtFileName != null) {
    		logger.info(String.format("using JSON webtoken from: %s existing: %b", jwtFileName, new File(jwtFileName).exists()));
    	}
		context.setWebsocketUrl(cmd.getOptionValue("ws"));
    	byte[] encoded = jwtFileName != null ? Files.readAllBytes(Paths.get(jwtFileName)) : null;
    	String jwt = encoded != null ? new String(encoded, "UTF-8") : null;
    	
		context.setWebtoken(jwt);
		
		connector.start();
		authenticator.start();
		printServiceRepository.start();
	}
	
    public static void main(String[] args) throws ParseException, IOException {
    	cli(args);
    }
}

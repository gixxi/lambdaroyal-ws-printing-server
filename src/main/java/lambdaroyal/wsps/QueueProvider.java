package lambdaroyal.wsps;

import java.util.LinkedList;
import java.util.Queue;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;

@Repository
public class QueueProvider {
	private Queue<String> queue = new LinkedList();
	private Queue<String> telegramServerQueue = new LinkedList();
	
	@Bean 
	Queue<String> queue() {
		return queue;
	}
	@Bean 	
	private Queue<String> telegramServerQueue() {
		return telegramServerQueue;
	}
}

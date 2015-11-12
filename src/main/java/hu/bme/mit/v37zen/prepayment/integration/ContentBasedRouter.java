package hu.bme.mit.v37zen.prepayment.integration;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.xml.xpath.XPathException;
import org.springframework.xml.xpath.XPathParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Kiss Dániel
 *
 */
@ManagedResource(
        objectName="bean:name=ContentBasedRouter",
        description="Route xml dom documents.")
public class ContentBasedRouter {
	
	public final static Logger logger = LoggerFactory.getLogger(ContentBasedRouter.class);

	//private NamespaceHandler namespaceHandler;
	
	private Map<String, RoutingRule> routingTable = new HashMap<String, RoutingRule>();
	
	private ThreadPoolTaskExecutor taskExecutor;
	
	private static class MessageSender implements Runnable {
		MessageChannel channel;
		Document document;
		
		public MessageSender(MessageChannel channel, Document document) {
			super();
			this.channel = channel;
			this.document = document;
		}

		public void run() {
			channel.send(new GenericMessage<Document>(document));
		}
	}
	
	public void route(Document document) {
		
		//String xml = (new DOMNodeToString()).nodeToString(document);
		logger.info("XML message has arrived.");
		//logger.debug('\n' + xml);
		
		document.getDocumentElement().normalize();
		Node node = document;
			
		for (RoutingRule rr : this.routingTable.values()) {
			try {
				boolean isContentMatch = rr.evaluate(node);
				if(isContentMatch){
					logger.debug("Routing: " + rr.getContentSelectorString() + " == " + rr.getExceptedContent().toString());
					taskExecutor.execute(new MessageSender(rr.getRoute(), document));
				}
			} catch (XPathParseException e) {
				logger.error(e.getMessage());
			} catch (XPathException e) {
				logger.error(e.getMessage());
			}
		}	
	}
	
	public ContentBasedRouter() {
	}
	
	public ContentBasedRouter(Map<String, RoutingRule> routingTable) {
		if(routingTable == null ){
			this.routingTable = new HashMap<String, RoutingRule>();
		}
		this.routingTable = routingTable;
	}
	
	public RoutingRule putRoutingRule(String ruleName, RoutingRule rule){
		return this.routingTable.put(ruleName, rule);
	}
	
	public RoutingRule removeRoutingRule(String ruleName){
		return this.routingTable.remove(ruleName);
	}

	@ManagedAttribute
	public Map<String, RoutingRule> getRoutingTable() {
		return routingTable;
	}

	public ThreadPoolTaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	
}
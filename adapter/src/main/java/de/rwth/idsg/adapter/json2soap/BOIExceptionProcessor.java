package de.rwth.idsg.adapter.json2soap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import de.rwth.idsg.adapter.soap2json.ResponseObjectCreator;

public class BOIExceptionProcessor implements Processor {
	@Override
	public void process(Exchange exchange) throws Exception {
		ResponseObjectCreator roc = new ResponseObjectCreator();
		byte[] error = roc.createErrorResponse(
				-32601, 
				"Method not found", 
				null, 
				exchange.getProperty("jsonrpc-id"));	
		exchange.getOut().setBody(error);
	}
}

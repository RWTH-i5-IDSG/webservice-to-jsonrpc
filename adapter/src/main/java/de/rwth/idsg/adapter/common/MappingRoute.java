package de.rwth.idsg.adapter.common;

import java.util.HashMap;

import org.apache.camel.builder.RouteBuilder;
import org.apache.cxf.binding.soap.SoapFault;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.idsg.adapter.json2soap.BOIExceptionProcessor;
import de.rwth.idsg.adapter.json2soap.RequestProcessor;
import de.rwth.idsg.adapter.manage.InitConfiguration;
import de.rwth.idsg.adapter.soap2json.ResponseProcessor;
import de.rwth.idsg.adapter.soap2json.SoapFaultProcessor;

/**
 * This class describes the mapping route from client to WS and back to client.
 */
public class MappingRoute extends RouteBuilder {
	
	// Since MappingRoute class is a singleton of Spring,
	// define and store the instances that will always be used 
	// throughout the lifetime of the adapter.
	// ObjectMapper is thread-safe: http://wiki.fasterxml.com/JacksonFAQThreadSafety
	public static String WS_NAMESPACE;
	public static final ObjectMapper JSON_MAPPER = new ObjectMapper();
	
	// Operation name and request message payload name might be different.
	// We store a map of operation names and their request message payload names 
	// for lookup (for document/literal wrapped style).
	public static HashMap<String, String> OPERATIONS_MAP = new HashMap<String, String>();
	
	@Override
	public void configure() {
				
		// If handled is true, then the thrown exception will be handled
		// and Camel will not continue routing in the original route, but break out.	
				
		// Catch JSON-RPC syntax errors
		onException(JsonRpcSyntaxException.class)
			.handled(true);
		
		// Catch a Soap Fault
		onException(SoapFault.class)	
			.handled(true)
			.process(new SoapFaultProcessor());
		
		// Catch the error when method does not exist at WS
		onException(IllegalArgumentException.class)
			.handled(true)
			.onWhen(exceptionMessage().contains("BindingOperationInfo"))
			.process(new BOIExceptionProcessor());
		
		// Normal route
		from(InitConfiguration.getServletEndpoint())
			.process(new RequestProcessor())
			.to(InitConfiguration.getServiceEndpoint())
			.process(new ResponseProcessor())
			.stop();
	}	
}
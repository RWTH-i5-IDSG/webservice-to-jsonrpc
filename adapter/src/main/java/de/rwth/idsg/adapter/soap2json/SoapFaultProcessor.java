package de.rwth.idsg.adapter.soap2json;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.cxf.binding.soap.SoapFault;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * This class handles a SOAP fault and sends a JSON-RPC error response to client.
 * Since the table at http://www.jsonrpc.org/specification#error_object is
 * not enough for our case, the error code is taken from
 * http://xmlrpc-epi.sourceforge.net/specs/rfc.fault_codes.php and
 * message is extended for an easier distinction from other 
 * JSON-RPC-specific errors.
 *  
 */
public class SoapFaultProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws JsonProcessingException{

		SoapFault fault = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, SoapFault.class);
		
		String[] data = {fault.getCodeString("",""),	// faultcode
						fault.getReason(),				// faultString
						fault.getRole()};				// faultActor
		
		ResponseObjectCreator roc = new ResponseObjectCreator();
		
		byte[] error = roc.createErrorResponse(
				-32500,
				"Application Error: SOAP Fault",
				data, 
				exchange.getProperty("jsonrpc-id"));

		exchange.getOut().setBody(error);
		
		// CLEAR VARIABLES
		fault = null;
		data = null;
		error = null;

	}

}

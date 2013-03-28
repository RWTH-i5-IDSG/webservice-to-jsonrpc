package de.rwth.idsg.adapter.soap2json;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.cxf.CxfPayload;
import org.apache.cxf.binding.soap.SoapHeader;
import org.w3c.dom.Element;

/**
 * Converts a SOAP response payload into a JSON-RPC response
 */
public class ResponseProcessor implements Processor {
	
	@Override
	public void process(Exchange exchange) throws IOException, Exception {

		// Read the exchange into a SOAP payload
		@SuppressWarnings("unchecked")
		CxfPayload<SoapHeader> inputPayload = exchange.getIn().getBody(CxfPayload.class);
			
		// Assumption: Since JSON-RPC can send only one method request at a time,
		// the soap body should contain only one response element.
		// Get the body element of payload
		Element inputBodyElement = inputPayload.getBody().get(0);
		
		ResponseObjectCreator roc = new ResponseObjectCreator();

		// Soap headers may exist. Proceed accordingly
		List<SoapHeader> inputHeadersList = inputPayload.getHeaders();		
		if( !inputHeadersList.isEmpty() ){
			
			Element inputHeadersElement = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder().newDocument().createElement("SOAP-HEADER");

			// Headers might have multiple elements
			for(int i=0; i<inputHeadersList.size(); i++){
				Element item = (Element) inputHeadersList.get(i).getObject();
				inputHeadersElement.appendChild(item);
			}	
			
			// Set the output body to JSON-RPC response WITH HEADER 
			exchange.getOut().setBody(roc.createNormalResponse(
					inputHeadersElement, 
					inputBodyElement, 
					exchange.getProperty("jsonrpc-id")));
			
			// CLEAR VARIABLE
			inputHeadersElement = null;
			
		}else{
			// Set the output body to JSON-RPC response WITHOUT HEADER
			exchange.getOut().setBody(roc.createNormalResponse(
					inputBodyElement, 
					exchange.getProperty("jsonrpc-id")));
			
		}
		
		// CLEAR VARIABLES
		inputPayload = null;
		inputBodyElement = null;
		inputHeadersList = null;
	}
	

}

package de.rwth.idsg.adapter.json2soap;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.cxf.CxfPayload;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.cxf.binding.soap.SoapHeader;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rwth.idsg.adapter.common.JsonRpcSyntaxException;
import de.rwth.idsg.adapter.common.MappingRoute;
import de.rwth.idsg.adapter.soap2json.ResponseObjectCreator;

/**
 * The main class that handles Camel exchanges to initiate 
 * and manage the mapping process of requests.
 * 
 */
public class RequestProcessor implements Processor {

	@Override
	public void process(Exchange exchange) 
			throws IOException, XMLStreamException, SAXException, JsonRpcSyntaxException {
		
		// Read the exchange input.
		InputStream is = exchange.getIn().getBody(InputStream.class);
		
		JsonNode inputJson = null;
		try{
			// Parse InputStream into JSON content.
			inputJson = MappingRoute.JSON_MAPPER.readTree(is);
		}catch (JsonProcessingException e) {
			// When parsing fails, send a JSON-RPC error response.
			// id is null, but still better than returning a generic error.
			ResponseObjectCreator roc = new ResponseObjectCreator();
			byte[] error =  roc.createErrorResponse(-32700, "Parse error", null, null);
			exchange.getIn().setBody(error);
			throw new JsonRpcSyntaxException();
		}finally{
			is.close();
		}		
		
		RequestUtils reqUtil = new RequestUtils();
		
		// Validate input JSON-RPC syntax.
		byte[] error = reqUtil.validateRequest(inputJson);
		if ( error != null ){
			exchange.getIn().setBody(error);
			throw new JsonRpcSyntaxException();
		}

		// Read required JSON members.
		String inMethodName	= inputJson.get("method").textValue();
		JsonNode inParamsNode	= inputJson.get("params");
		JsonNode inHeaderNode = null;
		if (inParamsNode != null) {
			inHeaderNode	= inParamsNode.get("SOAP-HEADER");	
		}

		// If "SOAP-HEADER" exists, create a Soap header.
		// Then delete "SOAP-HEADER" from params, so that the body can be created correctly.
		List<SoapHeader> outHeader = null;
		if( inHeaderNode != null ){
			outHeader = reqUtil.processHeader(inHeaderNode);
			((ObjectNode) inParamsNode).remove("SOAP-HEADER");
		}
		
		// Find the request message payload name for the given method.
		String reqMsgPayloadName = "";
		for (Entry<String, String> entry : MappingRoute.OPERATIONS_MAP.entrySet()) {
			if (entry.getKey().equals(inMethodName)) {
				reqMsgPayloadName = entry.getValue();
			}
		}
		
		// If the payload name could not be found in the map, set it to the method name as the last resort.
		if (reqMsgPayloadName.isEmpty()) reqMsgPayloadName = inMethodName;

		// Create Soap body.
		List<Element> outBody = reqUtil.processBody(inParamsNode, reqMsgPayloadName);

		// Create a CXF payload. Set exchange body to it.
		CxfPayload<SoapHeader> outputPayload = new CxfPayload<SoapHeader>(outHeader, outBody);
		exchange.getOut().setBody(outputPayload);
		
		// Set headers required by CXF.
		exchange.getOut().setHeader(CxfConstants.OPERATION_NAME, inMethodName);
		exchange.getOut().setHeader(CxfConstants.OPERATION_NAMESPACE, MappingRoute.WS_NAMESPACE);
		
		// Save the id as a property to be passed to ResponseProcessor.
		exchange.setProperty("jsonrpc-id", inputJson.get("id"));
		
		// CLEAR VARIABLES
		inputJson = null;
		inParamsNode = null;
		inHeaderNode = null;
		outHeader = null;
		outBody = null;
		outputPayload = null;
	}
}
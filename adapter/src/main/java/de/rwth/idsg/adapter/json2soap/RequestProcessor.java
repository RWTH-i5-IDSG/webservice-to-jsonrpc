package de.rwth.idsg.adapter.json2soap;

import java.io.InputStream;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.cxf.CxfPayload;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.cxf.binding.soap.SoapHeader;

import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rwth.idsg.adapter.common.JsonRpcSyntaxException;
import de.rwth.idsg.adapter.common.MappingRoute;

public class RequestProcessor implements Processor {

	/**
	 * Converts a JSON-RPC request into a SOAP request payload
	 */
	@Override
	public void process(Exchange exchange) throws Exception {
		
		// Read the exchange input
		JsonNode inputJson = MappingRoute.JSON_MAPPER.readTree(exchange.getIn().getBody(InputStream.class));
		
		RequestUtils reqUtil = new RequestUtils();
		
		// Validate input JSON-RPC syntax
		byte[] error = reqUtil.validateRequest(inputJson);
		if ( error != null ){
			exchange.getIn().setBody(error);
			throw new JsonRpcSyntaxException();
		}

		// Read required objects
		String inMethodName		= inputJson.get("method").textValue();
		JsonNode inParamsNode	= inputJson.get("params");
		JsonNode inHeaderNode	= inParamsNode.get("SOAP-HEADER");	

		// If "SOAP-HEADER" exists, create a Soap header
		// Then delete "SOAP-HEADER" from params, so that the body can be created correctly
		List<SoapHeader> outHeader = null;
		if( inHeaderNode != null ){
			outHeader = reqUtil.processHeader(inHeaderNode);
			((ObjectNode) inParamsNode).remove("SOAP-HEADER");
		}

		// Create Soap body
		List<Element> outBody = reqUtil.processBody(inParamsNode, inMethodName);

		// Create a Soap payload. Set exchange body to it.
		// Save the id as property to be passed to ResponseProcessor.
		CxfPayload<SoapHeader> outputPayload = new CxfPayload<SoapHeader>(outHeader, outBody);
		exchange.getOut().setBody(outputPayload);
		exchange.getOut().setHeader(CxfConstants.OPERATION_NAME, inMethodName);
		exchange.getOut().setHeader(CxfConstants.OPERATION_NAMESPACE, MappingRoute.WS_NAMESPACE);
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

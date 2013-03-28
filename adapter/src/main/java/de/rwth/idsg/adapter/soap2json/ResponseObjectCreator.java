package de.rwth.idsg.adapter.soap2json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Element;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.odysseus.staxon.json.JsonXMLOutputFactory;
import de.odysseus.staxon.json.stream.jackson.JacksonStreamFactory;
import de.rwth.idsg.adapter.common.MappingRoute;

public class ResponseObjectCreator {
	
	/**
	 * Writes a JSON-RPC 2.0 response object WITHOUT HEADER when everything goes okay
	 * 
	 * @param body			The soap body element
	 * @param idHeader		JSON-RPC id
	 * @throws Exception 
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 */
	public byte[] createNormalResponse(Element body, Object idHeader) throws JsonProcessingException, IOException, Exception  {

		// Convert the SOAP body element to JSON
		JsonNode resultNode = convertToJson(body).get(body.getNodeName());
		
		// If the result tree has only one child node, get its value
		if ( resultNode.size() == 1 ) resultNode = resultNode.elements().next();
		
		// Create the response JSON object
		ObjectNode outputJson = MappingRoute.JSON_MAPPER.createObjectNode();
		outputJson.put("jsonrpc", "2.0");
		outputJson.put("result", resultNode);
		outputJson.put("id", (JsonNode) idHeader);

		return MappingRoute.JSON_MAPPER.writeValueAsBytes(outputJson);
	}
	
	/**
	 * Writes a JSON-RPC 2.0 response object WITH HEADER when everything goes okay
	 * 
	 * @param header		The soap header element
	 * @param body			The soap body element
	 * @param idHeader		JSON-RPC id
	 * @throws Exception 
	 * @throws IOException 
	 */
	public byte[] createNormalResponse(Element header, Element body, Object idHeader) throws IOException, Exception {

		// Convert the SOAP header element to JSON
		ObjectNode headerNode = convertToJson(header);

		// Convert the SOAP body element to JSON
		ObjectNode resultNode = convertToJson(body);
		
		// Put header and body into the result member
		ObjectNode resultObject = MappingRoute.JSON_MAPPER.createObjectNode();
		resultObject.putAll(headerNode);
		resultObject.putAll(resultNode);
		
		// Create the response JSON object
		ObjectNode outputJson = MappingRoute.JSON_MAPPER.createObjectNode();
		outputJson.put("jsonrpc", "2.0");
		outputJson.put("result", resultObject);
		outputJson.put("id", (JsonNode) idHeader);

		return MappingRoute.JSON_MAPPER.writeValueAsBytes(outputJson);
	}

	/**
	 * Writes a JSON-RPC 2.0 response object when a RPC call encounters an error
	 * 
	 * @param code		Error code
	 * @param message	Error message
	 * @param data		Details about error (if any) 
	 * @param idHeader	JSON-RPC id
	 * @throws JsonProcessingException 
	 */
	public byte[] createErrorResponse(int code, String message, String[] data, Object idHeader) throws JsonProcessingException {

		// Create the error member
		ObjectNode errorObject = MappingRoute.JSON_MAPPER.createObjectNode();
		errorObject.put("code", code);
		errorObject.put("message", message);
		
		// If present, put the error details into data member and
		// then data into error
		if ( data != null ) {
			ObjectNode dataNode = MappingRoute.JSON_MAPPER.createObjectNode();
			dataNode.put("faultcode" , data[0]);
			dataNode.put("faultString" , data[1]);
			dataNode.put("faultActor" , data[2]);		
			errorObject.put("data", dataNode);
		}

		// Create the error response JSON object
		ObjectNode outputJson = MappingRoute.JSON_MAPPER.createObjectNode();
		outputJson.put("jsonrpc", "2.0");
		outputJson.put("error", errorObject);
		outputJson.put("id", (JsonNode) idHeader);

		return MappingRoute.JSON_MAPPER.writeValueAsBytes(outputJson);
	}
	
	/**
	 * Converts a Xml element into a Json ObjectNode
	 * 
	 * @param xml	Xml element to be converted
	 * 
	 * @throws TransformerException 
	 * @throws ParserConfigurationException 
	 * @throws FactoryConfigurationError 
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	private ObjectNode convertToJson(Element xml) 
			throws ParserConfigurationException, TransformerException, XMLStreamException, 
			FactoryConfigurationError, JsonParseException, JsonMappingException, IOException {
		
		// Set the input and output
		DOMSource input = new DOMSource(xml);
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		// Configure the converter
		JsonXMLOutputFactory factory = new JsonXMLOutputFactory(new JacksonStreamFactory());
		factory.setProperty(JsonXMLOutputFactory.PROP_NAMESPACE_DECLARATIONS, Boolean.FALSE);
		factory.setProperty(JsonXMLOutputFactory.PROP_AUTO_ARRAY, Boolean.TRUE);
		factory.setProperty(JsonXMLOutputFactory.PROP_AUTO_PRIMITIVE, Boolean.TRUE);
		factory.setProperty(JsonXMLOutputFactory.PROP_VIRTUAL_ROOT, null);
		factory.setProperty(JsonXMLOutputFactory.PROP_PRETTY_PRINT, Boolean.TRUE);
		
		// Create reader (XML).
		XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(input);

		// Create writer (JSON).
		XMLEventWriter writer = factory.createXMLEventWriter(output);

		// Copy events from reader to writer.
		writer.add(reader);

		// Close reader/writer.
		reader.close();
		writer.close();
		
		return MappingRoute.JSON_MAPPER.readValue(output.toByteArray(), ObjectNode.class);
	}
}

package de.rwth.idsg.adapter.soap2json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
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


/**
 * This class is responsible of syntactically converting
 * SOAP response messages into JSON-RPC response messages.
 *
 */
public class ResponseObjectCreator {
	
	/**
	 * Writes a JSON-RPC 2.0 response object WITHOUT HEADER when everything goes okay.
	 * 
	 * @param body			The SOAP Body element
	 * @param idProperty		JSON-RPC id
	 * 
	 * @throws IOException 
	 * @throws FactoryConfigurationError 
	 * @throws XMLStreamException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 * 
	 */
	public byte[] createNormalResponse(Element body, Object idProperty) 
			throws JsonParseException, JsonMappingException, XMLStreamException, FactoryConfigurationError, IOException {

		// Convert the SOAP body element to JSON
		JsonNode resultNode = convertToJson(body).get(body.getNodeName());
		
		// If the result tree has only one child node, get its value
		if ( resultNode.size() == 1 ) resultNode = resultNode.elements().next();
		
		// Create the response JSON object
		ObjectNode outputJson = MappingRoute.JSON_MAPPER.createObjectNode();
		outputJson.put("jsonrpc", "2.0");
		outputJson.put("result", resultNode);
		outputJson.put("id", (JsonNode) idProperty);

		return MappingRoute.JSON_MAPPER.writeValueAsBytes(outputJson);
	}
	
	/**
	 * Writes a JSON-RPC 2.0 response object WITH HEADER when everything goes okay.
	 * 
	 * @param header		The SOAP Header element
	 * @param body			The SOAP Body element
	 * @param idProperty		JSON-RPC id
	 * 
	 * @throws IOException 
	 * @throws FactoryConfigurationError 
	 * @throws XMLStreamException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 * 
	 */
	public byte[] createNormalResponse(Element header, Element body, Object idProperty) 
			throws JsonParseException, JsonMappingException, XMLStreamException, FactoryConfigurationError, IOException {

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
		outputJson.put("id", (JsonNode) idProperty);

		return MappingRoute.JSON_MAPPER.writeValueAsBytes(outputJson);
	}

	/**
	 * Writes a JSON-RPC 2.0 response object when a RPC call encounters an error.
	 * 
	 * @param code		Error code
	 * @param message	Error message
	 * @param data		Details about error (if any) 
	 * @param idProperty	JSON-RPC id
	 * 
	 * @throws JsonProcessingException 
	 * 
	 */
	public byte[] createErrorResponse(int code, String message, String[] data, Object idProperty) 
			throws JsonProcessingException {

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
		outputJson.put("id", (JsonNode) idProperty);
		
		return MappingRoute.JSON_MAPPER.writeValueAsBytes(outputJson);
	}
	
	/**
	 * Converts a XML element into a JSON ObjectNode.
	 * 
	 * @param xml	XML element to be converted
	 * 
	 * @throws FactoryConfigurationError 
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	private ObjectNode convertToJson(Element xml) 
			throws XMLStreamException, FactoryConfigurationError, JsonParseException, JsonMappingException, IOException {

		// Set the input and output.
		DOMSource input = new DOMSource(xml);
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		// Configure the converter.
		JsonXMLOutputFactory factory = new JsonXMLOutputFactory(new JacksonStreamFactory());
		factory.setProperty(JsonXMLOutputFactory.PROP_NAMESPACE_DECLARATIONS, Boolean.FALSE);
		factory.setProperty(JsonXMLOutputFactory.PROP_AUTO_ARRAY, Boolean.TRUE);
		factory.setProperty(JsonXMLOutputFactory.PROP_AUTO_PRIMITIVE, Boolean.TRUE);
		//factory.setProperty(JsonXMLOutputFactory.PROP_VIRTUAL_ROOT, null);

		// Create reader (XML).
		XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(input);

		// Create writer (JSON).
		XMLEventWriter writer = factory.createXMLEventWriter(output);

		// Copy events from reader to writer.
		writer.add(reader);

		// Close reader/writer.
		reader.close();
		writer.close();

		// Parse output as an ObjectNode and return it.
		return MappingRoute.JSON_MAPPER.readValue(output.toByteArray(), ObjectNode.class);
	}
}

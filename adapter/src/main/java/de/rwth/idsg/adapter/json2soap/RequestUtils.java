package de.rwth.idsg.adapter.json2soap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.cxf.binding.soap.SoapHeader;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.odysseus.staxon.json.JsonXMLInputFactory;
import de.odysseus.staxon.json.stream.jackson.JacksonStreamFactory;
import de.rwth.idsg.adapter.common.MappingRoute;
import de.rwth.idsg.adapter.soap2json.ResponseObjectCreator;

public class RequestUtils {
	
	DocumentBuilderFactory factory;
	DocumentBuilder builder;
	
	RequestUtils(){
		try {
			factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks the incoming request for syntax errors according to JSON-RPC 2.0 Specification.
	 * If an error is found, a JSON-RPC error response is created.
	 * 
	 * @throws JsonProcessingException 
	 */
	public byte[] validateRequest(JsonNode inputJson) throws JsonProcessingException{	
			
		if(!inputJson.isObject()){
			ResponseObjectCreator roc = new ResponseObjectCreator();
			return roc.createErrorResponse(-32700, "Parse error", null, inputJson.get("id"));
		}	
		if ( !inputJson.has("jsonrpc") || !inputJson.get("jsonrpc").textValue().equals("2.0") ) {
			ResponseObjectCreator roc = new ResponseObjectCreator();
			return roc.createErrorResponse(-32600, "Invalid Request", null, inputJson.get("id"));
		}
		if ( !inputJson.has("method") || !inputJson.get("method").isTextual() ){
			ResponseObjectCreator roc = new ResponseObjectCreator();
			return roc.createErrorResponse(-32600, "Invalid Request", null, inputJson.get("id"));
		//}else if(){
			//TODO: Check if method available at WS
			//Collection<BindingOperationInfo> bois = client.getEndpoint().getEndpointInfo().getBinding().getOperations();
		}
		if ( inputJson.has("params") && !inputJson.get("params").isObject() ){
			ResponseObjectCreator roc = new ResponseObjectCreator();
			return roc.createErrorResponse(-32600, "Invalid Request", null, inputJson.get("id"));
		}
		
		//TODO: Check if params are correct for method
		
		return null;
	}
	
	/**
	 * Converts each element in "SOAP-HEADER" JsonNode into a SoapHeader,
	 * and adds each SoapHeader to a list.
	 * 
	 * !Namespace/prefix for each element must be provided by client!
	 * 
	 * @throws ParserConfigurationException 
	 * @throws XMLStreamException 
	 * @throws Exception 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws JsonProcessingException 
	 */
	public List<SoapHeader> processHeader(JsonNode input) 
			throws XMLStreamException, IOException, SAXException, ParserConfigurationException {
		
		// Create a list for output headers
		List<SoapHeader> output = new ArrayList<SoapHeader>();

		// SOAP-HEADER might have multiple elements in it
		Iterator<Entry<String, JsonNode>> elements = input.fields();
		while (elements.hasNext()) {

			Entry<String, JsonNode> temp = elements.next();
			String tempKey = temp.getKey();
			JsonNode tempValue = temp.getValue();

			// Extract the value and delete the node
			String headerUnderstand = tempValue.path("@mustUnderstand").textValue();
			((ObjectNode) tempValue).remove("@mustUnderstand");
			
			//String headerNs = tempValue.path("@xmlns").getTextValue();
			//((ObjectNode) tempValue).remove("@mxmlns");
			//QName qname = new QName(headerNs, tempKey);

			Element headerElement = convertToXml(tempValue, tempKey);
			
			setDefaultNamespace(headerElement);		
			
			// Since the output of StaxonConverter already has the namespace/prefix
			// or default namespace is set in previous step, new QName("") is just 
			// there because SoapHeader constructor needs one. It does not
			// change headerElement.
			SoapHeader newSoapHeader = new SoapHeader(new QName(""), headerElement);

			// Set the mustUnderstand attribute of header
			if (headerUnderstand.equals("1")){
				newSoapHeader.setMustUnderstand(true);	
			}else {
				newSoapHeader.setMustUnderstand(false);
			}

			// Add the header to output
			output.add(newSoapHeader);
		}
		return output;		
	}

	/**
	 * Creates the SOAP body. Because JSON-RPC can send only 
	 * one method request at a time, there is no possibility of multiple body elements.
	 * 
	 * Reminder: According to JSON-RPC 2.0 Specification params node MAY be omitted.
	 * 
	 * @throws ParserConfigurationException 
	 * @throws DOMException 
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws XMLStreamException 
	 */
	public List<Element> processBody(JsonNode params, String method) 
			throws DOMException, ParserConfigurationException, XMLStreamException, IOException, SAXException {

		Element body = null;
		if ( params == null ){
			body = builder.newDocument().createElement(method);	
		}else{
			body = convertToXml(params, method);			  	
		}

		setDefaultNamespace(body);
		List<Element> output = new ArrayList<Element>(1);
		output.add(body);
		return output;
	}
	
	/**
	 *  If client did not provide a namespace for the element, 
	 *  set to default namespace of Web Service
	 */
	private void setDefaultNamespace(Element element){
		if ( element.getNamespaceURI() == null ) {
			element.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, MappingRoute.WS_NAMESPACE);
		}			
	}
	
	/**
	 * Converts a JsonNode into Xml element
	 * 
	 * @param node	JsonNode to be converted
	 * @param str	String to be written as the root for Xml
	 * 
	 * @throws XMLStreamException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws IOException 
	 */
	private Element convertToXml(JsonNode node, String str) 
			throws XMLStreamException, IOException, SAXException, ParserConfigurationException {
		
		// Prepare the node for input. Then set the input and output.
		byte[] nodeBytes = MappingRoute.JSON_MAPPER.writeValueAsBytes(node);		
		ByteArrayInputStream input = new ByteArrayInputStream(nodeBytes);
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		// Configure the converter
		JsonXMLInputFactory factory = new JsonXMLInputFactory(new JacksonStreamFactory());
		factory.setProperty(JsonXMLInputFactory.PROP_MULTIPLE_PI, Boolean.FALSE);

		// Define whether the root element is written or not
		if (str != null) factory.setProperty(JsonXMLInputFactory.PROP_VIRTUAL_ROOT, str);

		// Create reader (JSON).
		XMLEventReader reader = factory.createXMLEventReader(input);

		// Create writer (XML).
		XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(output);

		// Copy events from reader to writer.
		writer.add(reader);

		// Close reader/writer.
		reader.close();
		writer.close();
		
		return builder.parse(new ByteArrayInputStream(output.toByteArray())).getDocumentElement();
	}
}

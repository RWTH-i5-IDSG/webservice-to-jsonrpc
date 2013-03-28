package de.rwth.idsg.adapter.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.apache.camel.BytesSource;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.odysseus.staxon.json.JsonXMLInputFactory;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import de.odysseus.staxon.json.stream.JsonStreamFactory;
import de.odysseus.staxon.json.stream.jackson.JacksonStreamFactory;

public class StaxonConverter2 {
	
	static XMLInputFactory xmlInFactory = XMLInputFactory.newInstance();
	static XMLOutputFactory xmlOutFactory = XMLOutputFactory.newInstance();
	static JsonStreamFactory streamFactory = new JacksonStreamFactory();
	static JsonXMLInputFactory jsonInFactory = new JsonXMLInputFactory(streamFactory);
	static JsonXMLOutputFactory jsonOutFactory = new JsonXMLOutputFactory(streamFactory);
		
	StaxonConverter2(){	
		// Configure the converter for Xml -> Json
		jsonOutFactory.setProperty(JsonXMLOutputFactory.PROP_NAMESPACE_DECLARATIONS, Boolean.FALSE);
		jsonOutFactory.setProperty(JsonXMLOutputFactory.PROP_AUTO_ARRAY, Boolean.TRUE);
		jsonOutFactory.setProperty(JsonXMLOutputFactory.PROP_AUTO_PRIMITIVE, Boolean.TRUE);
		jsonOutFactory.setProperty(JsonXMLOutputFactory.PROP_VIRTUAL_ROOT, null);
		jsonOutFactory.setProperty(JsonXMLOutputFactory.PROP_PRETTY_PRINT, Boolean.TRUE);

		// Configure the converter for Json -> Xml
		jsonInFactory.setProperty(JsonXMLInputFactory.PROP_MULTIPLE_PI, Boolean.FALSE);
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
	public synchronized ObjectNode convertToJson(Element xml) 
			throws ParserConfigurationException, TransformerException, XMLStreamException, 
			FactoryConfigurationError, JsonParseException, JsonMappingException, IOException {
		
		// Set the input and output
		DOMSource input = MappingRoute.XML_CONVERTER.toDOMSource(xml);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		// Create reader (XML).
		XMLEventReader reader = xmlInFactory.createXMLEventReader(input);

		// Create writer (JSON).
		XMLEventWriter writer = jsonOutFactory.createXMLEventWriter(output);

		// Copy events from reader to writer.
		writer.add(reader);

		// Close reader/writer.
		reader.close();
		writer.close();
		
		return MappingRoute.JSON_MAPPER.readValue(output.toByteArray(), ObjectNode.class);
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
	public synchronized Element convertToXml(JsonNode node, String str) 
			throws XMLStreamException, IOException, SAXException, ParserConfigurationException {
		
		// Prepare the node for input. Then set the input and output.
		byte[] nodeBytes = MappingRoute.JSON_MAPPER.writeValueAsBytes(node);		
		BytesSource input = MappingRoute.XML_CONVERTER.toBytesSource(nodeBytes);
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		// Define whether the root element is written or not
		if (str != null) jsonInFactory.setProperty(JsonXMLInputFactory.PROP_VIRTUAL_ROOT, str);

		// Create reader (JSON).
		XMLEventReader reader = jsonInFactory.createXMLEventReader(input);

		// Create writer (XML).
		XMLEventWriter writer = xmlOutFactory.createXMLEventWriter(output);

		// Copy events from reader to writer.
		writer.add(reader);

		// Close reader/writer.
		reader.close();
		writer.close();
		
		// Set the factory value to default
		jsonInFactory.setProperty(JsonXMLInputFactory.PROP_VIRTUAL_ROOT, null);
		
		return MappingRoute.XML_CONVERTER.toDOMDocument(output.toByteArray()).getDocumentElement();
	}
}

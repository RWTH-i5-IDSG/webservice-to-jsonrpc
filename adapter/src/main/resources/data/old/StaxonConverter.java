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
import de.odysseus.staxon.json.stream.jackson.JacksonStreamFactory;

public class StaxonConverter {

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
	public static ObjectNode convertToJson(Element xml) 
			throws ParserConfigurationException, TransformerException, XMLStreamException, 
			FactoryConfigurationError, JsonParseException, JsonMappingException, IOException {
		
		// Set the input and output
		DOMSource input = MappingRoute.XML_CONVERTER.toDOMSource(xml);
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
	public static Element convertToXml(JsonNode node, String str) 
			throws XMLStreamException, IOException, SAXException, ParserConfigurationException {
		
		// Prepare the node for input. Then set the input and output.
		byte[] nodeBytes = MappingRoute.JSON_MAPPER.writeValueAsBytes(node);		
		BytesSource input = MappingRoute.XML_CONVERTER.toBytesSource(nodeBytes);
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
		
		return MappingRoute.XML_CONVERTER.toDOMDocument(output.toByteArray()).getDocumentElement();
	}
}
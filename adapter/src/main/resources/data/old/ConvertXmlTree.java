package de.rwthaachen.idsg.adapter.soap2json;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ConvertXmlTree {


	/**
	 * Creates JsonNode from Xml result node
	 * Not finished yet
	 * 
	 */
	public static JsonNode traverse(Node n, ObjectMapper mapper) {
		
		JsonNode currentNode = null;
		
		if (n.getNodeType() == Node.ELEMENT_NODE) {
			
			System.out.println(n.getNodeName());
			NodeList nl = n.getChildNodes();
			
			//currentNode = mapper.createObjectNode();
			//((ObjectNode) currentNode).putObject(n.getNodeName());
			
			for (int i = 0; i < nl.getLength(); i++){				
				//((ObjectNode) currentNode).putAll(traverse(nl.item(i), mapper));
				traverse(nl.item(i), mapper);
			}
		}
		
		if (n.getNodeType() == Node.TEXT_NODE && !n.getNodeValue().trim().isEmpty()){		
			try {
				System.out.println(n.getNodeValue());		
				//currentNode = mapper.readValue(n.getNodeValue(), JsonNode.class);
			} catch (Exception e) {
				System.out.println ("Something bad happened.");
			}
		}
		return currentNode;
	}
}

package de.rwthaachen.idsg.adapter.soap2json;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.json.JSON;
import net.sf.json.xml.XMLSerializer;

import org.w3c.dom.Node;

public class Converter {
	
	private static String nodeToString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			t.setOutputProperty(OutputKeys.INDENT, "no");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			System.out.println("nodeToString Transformer Exception");
		}
		return sw.toString();
	}
	
	public static JSON convertXMLtoJSON(Node node){
		String xml = nodeToString(node);
		XMLSerializer xmlSerializer = new XMLSerializer(); 
		xmlSerializer.setSkipNamespaces(true);
		xmlSerializer.setForceTopLevelObject(false);
        JSON json = xmlSerializer.read( xml );
		return json;
	}

}

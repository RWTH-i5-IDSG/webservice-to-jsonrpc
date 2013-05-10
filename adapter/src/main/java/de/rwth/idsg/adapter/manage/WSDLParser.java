package de.rwth.idsg.adapter.manage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.util.CastUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.rwth.idsg.adapter.common.MappingRoute;

/**
 * This class calls methods during initialization of the adapter to
 * obtain the WSDL interface from the URL, parse it and extract 
 * required information to specify endpoint details. Additionally, it creates 
 * a HTML page to help clients create JSON-RPC request messages. 
 *
 */
public class WSDLParser  {

	final static Logger LOG = LoggerFactory.getLogger(WSDLParser.class);

	public String wsdlUrl;
	public String serviceUrl;
	public String serviceName;
	public String wsNamespace;
	public String soapPortName;

	/**
	 * Main method that starts the WSDL parsing process and calls other helper methods.
	 */
	public void readWSDL(){
		try {
			// Read the WSDL URL and docBase from config file
			InitialContext ctx = new InitialContext();
			wsdlUrl = (String) ctx.lookup("java:comp/env/wsdlUrl");
			LOG.info("WSDL URL: " + wsdlUrl);
			String dirPath = (String) ctx.lookup("java:comp/env/dirPath");
			ctx.close();

			// Set up the reader
			WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
			reader.setFeature("javax.wsdl.verbose", false);
			reader.setFeature("javax.wsdl.importDocuments", true);

			// Get WSDL as a document
			Document wsdlDoc = getWSDLAsDocument();

			// Convert the WSDL document to a WSDL definition
			Definition def = reader.readWSDL(null, wsdlDoc);

			// Initialize the necessary variables for Cxf
			initializeVariables(def);

			// Create a HTML page from the WSDL document
			if(dirPath != null) convertWSDLtoHTML(dirPath, wsdlDoc);

		} catch (WSDLException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the WSDL content from URL and creates a document from it.
	 */
	private Document getWSDLAsDocument(){
		LOG.info("Retrieving document from the WSDL URL...");
		Document doc = null;
		try{
			// Get the content from URL
			InputStream inputStream = new URL(wsdlUrl).openStream();
			InputSource inputSource = new InputSource(inputStream);
			inputSource.setSystemId(wsdlUrl);
			if (inputStream == null) throw new IllegalArgumentException("No content at URL.");

			// Set up the factory
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);

			// Read the content into a document
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(inputSource);
			inputStream.close();
		
		} catch (RuntimeException e){
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
	    } catch (IOException e) {
			e.printStackTrace();
		}
		return doc;
	}
	
	/**
	 * Reads the details from the WSDL definition.
	 */
	private void initializeVariables(Definition def){
		
		// Read the WSDL namespace 
		wsNamespace = def.getTargetNamespace();
		LOG.info("Service namespace: " + wsNamespace);

		// Set the constants for processing of SOAP requests
		MappingRoute.WS_NAMESPACE = wsNamespace;

		// Get the details to specify the service endpoint
		Collection<Service> services = CastUtils.cast(def.getAllServices().values());
		for (Service service : services) {
			Collection<Port> ports = CastUtils.cast(service.getPorts().values());
			for (Port port : ports) {
				List<?> extensions = port.getExtensibilityElements();
				for (Object extension : extensions) {
					if (extension instanceof SOAPAddress) {	
						serviceUrl = ((SOAPAddress) extension).getLocationURI();
						serviceName = service.getQName().getLocalPart();
						soapPortName = port.getName();							
						LOG.info("Service address: " + serviceUrl);
						LOG.info("Service name: " + serviceName);
						LOG.info("Port name: " + soapPortName);
					}
				}
			}
		}
	}

	/**
	 * Creates a HTML page for clients using XSLT that displays information 
	 * how to access/use the Web service.
	 */
	private void convertWSDLtoHTML(String dirPath, Document wsdlDoc){
		LOG.info("Creating the HTML page from WSDL...");
		try {
			// Read the XSLT file
			InputStream xsltStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("wsdl-viewer.xsl");			
			if (xsltStream == null){
				LOG.info("XSLT file could not be read. Skipping the creation of the HTML page.");
				return;
			}
			Source xsltSource = new StreamSource(xsltStream);
			xsltSource.setSystemId(wsdlUrl);		
			
			// Create a transformer from the XSLT
			Transformer transformer = TransformerFactory.newInstance().newTransformer(xsltSource);
			
			// Do the transformation
			DOMSource domSource = new DOMSource(wsdlDoc);
			transformer.transform(domSource, new StreamResult(new File(dirPath, "service-details.html")));
			xsltStream.close();
			
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
}


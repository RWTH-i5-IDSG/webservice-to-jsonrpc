package de.rwth.idsg.adapter.manage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.camel.util.CastUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.ibm.wsdl.extensions.schema.SchemaImpl;

public class WSDLParser {

	final static Logger LOG = LoggerFactory.getLogger(WSDLParser.class);

	public String wsdlUrl;
	public String serviceUrl;
	public String serviceName;
	public String wsNamespace;

	private Port soapPort;
	private XmlSchema schema;

	//	public WSDLParser(){
	//		try {
	//			// Read the WSDL URL from <env-entry>
	//			InitialContext ctx = new InitialContext();
	//			wsdlUrl = (String) ctx.lookup("java:comp/env/wsdlUrl"); 
	//			LOG.info("Retrieving WSDL from address: " + wsdlUrl);
	//			ctx.close();
	//		} catch (NamingException e) {
	//			e.printStackTrace();
	//		}
	//	}

	public static void main(String[] args){
		WSDLParser wp = new WSDLParser();
		wp.wsdlUrl = "http://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl";
		//wp.wsdlUrl = "http://www.webservicex.net/ConvertSpeed.asmx?WSDL";
		//wp.wsdlUrl = "http://soap.amazon.com/schemas2/AmazonWebServices.wsdl";
		wp.readWSDL();
		wp.getOperations();
	}

	public void readWSDL(){
		try {
			WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
			reader.setFeature("javax.wsdl.verbose", false);
			reader.setFeature("javax.wsdl.importDocuments", true);

			// Get the whole WSDL file
			Definition def = reader.readWSDL(wsdlUrl);

			// Initialize the variables necessary for Cxf
			initializeVariables(def);

			// Get the XML Schema
			List<?> extensions = def.getTypes().getExtensibilityElements();
			for (Object extension : extensions) {			
				if (extension instanceof SchemaImpl){
					Element schElement = ((SchemaImpl) extension).getElement();
					schema = new XmlSchemaCollection().read(schElement);			
				}
			}
		} catch (WSDLException e) {
			e.printStackTrace();
		}
	}

	public void initializeVariables(Definition def){

		wsNamespace = def.getTargetNamespace();
		LOG.info("Service namespace: " + wsNamespace);

		// Set the constants for processing of SOAP requests
		//MappingRoute.WS_NAMESPACE = wsNamespace;

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
						soapPort = port;							
						LOG.info("Service address: " + serviceUrl);
						LOG.info("Service name: " + serviceName);
						LOG.info("Port name: " + soapPort.getName());
					}
				}
			}
		}
	}

	/**
	 * Gets the operations defined for SOAP Port
	 */
	private void getOperations(){

		LOG.info("**** Listing methods ****");
		List<?> operations = soapPort.getBinding().getPortType().getOperations();
		int counter = 0;
		for (Object item : operations) {
			counter++;
			Operation operation = (Operation) item;
			LOG.info(counter + ".Method name: " + operation.getName());
			LOG.info("Method details:");

			Collection<Part> inParts = CastUtils.cast(operation.getInput().getMessage().getParts().values());
			for (Part inPart : inParts) {
//				LOG.info("Part name: " + inPart.getName());	
//				LOG.info("Part type name: " + inPart.getTypeName());
//				LOG.info("Part element name: " + inPart.getElementName());
				getOperationDetails(inPart.getName(), inPart.getElementName(), inPart.getTypeName());	
			}
		}
	}

	private void getOperationDetails(String name, QName elementName, QName typeName){

		XmlSchemaType elemType = null;	
		if (elementName != null){
			elemType = schema.getElementByName(elementName).getSchemaType();
		}else{
			elemType = schema.getTypeByName(typeName);
		}

		if (elemType instanceof XmlSchemaComplexType) {
			processComplexType(elemType);
		}else if (elemType instanceof XmlSchemaSimpleType) {
			System.out.print(name + ": ");
			processSimpleType(elemType);
		}else{
			System.out.println(name + ": " + typeName.getLocalPart());
		}
	}

	private void processComplexType(XmlSchemaType elemType) {

		XmlSchemaParticle particle = ((XmlSchemaComplexType) elemType).getParticle();
		List<?> elementList = null;
		if ( particle instanceof XmlSchemaSequence ) {
			elementList = ((XmlSchemaSequence) particle).getItems();     
		}else if( particle instanceof XmlSchemaAll ){
			elementList = ((XmlSchemaAll) particle).getItems();
		}
		
		//if (elementList == null) return;

		for (Object member : elementList) {
			if (member instanceof XmlSchemaElement) {
				XmlSchemaElement innerElement = ((XmlSchemaElement) member);
				XmlSchemaType innerEleType = innerElement.getSchemaType();
				if (innerEleType instanceof XmlSchemaComplexType) {
					processComplexType(innerEleType);
				} else if(innerEleType instanceof XmlSchemaSimpleType){
					
					System.out.print(innerElement.getName());
					if (isRequired(innerElement)){
						System.out.print(" {required}: ");
					}else{
						System.out.print(": ");
					}
					processSimpleType(innerEleType);
				}
			}
		}
	}

	private void processSimpleType(XmlSchemaType innerEleType) {
					
		XmlSchemaSimpleType type = (XmlSchemaSimpleType) innerEleType;
		if( isEnumeration(type) ){
			System.out.println(enumeratorValues(type));
		}else{
			System.out.println(type.getName());
		}

	}

	/**
	 * Return true if a simple type is a straightforward XML Schema representation of an enumeration.
	 * If we discover schemas that are 'enum-like' with more complex structures, we might
	 * make this deal with them.
	 * 
	 * @param type Simple type, possible an enumeration.
	 * @return true for an enumeration.
	 */
	public static boolean isEnumeration(XmlSchemaSimpleType type) {
		XmlSchemaSimpleTypeRestriction restriction = (XmlSchemaSimpleTypeRestriction) type.getContent();
		List<XmlSchemaFacet> facets = restriction.getFacets();
		for (XmlSchemaFacet facet : facets) {
			if (facet instanceof XmlSchemaEnumerationFacet) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Retrieve the string values for an enumeration.
	 * 
	 * @param type
	 * @return
	 */  
	private static List<String> enumeratorValues(XmlSchemaSimpleType type) {
		XmlSchemaSimpleTypeRestriction restriction = (XmlSchemaSimpleTypeRestriction) type.getContent();
		List<XmlSchemaFacet> facets = restriction.getFacets();
		List<String> values = new ArrayList<String>(); 
		for (XmlSchemaFacet facet : facets) {
			XmlSchemaEnumerationFacet enumFacet = (XmlSchemaEnumerationFacet) facet;
			values.add(enumFacet.getValue().toString());
		}
		return values;
	}

	private static boolean isRequired(XmlSchemaElement element) {
		return (element.getMinOccurs() != 0);
	}

	public String getPortName(){
		return soapPort.getName();		
	}
}

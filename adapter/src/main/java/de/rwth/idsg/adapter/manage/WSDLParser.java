package de.rwth.idsg.adapter.manage;

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

import org.apache.camel.util.CastUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwth.idsg.adapter.common.MappingRoute;

public class WSDLParser {

	final static Logger LOG = LoggerFactory.getLogger(WSDLParser.class);

	public String wsdlUrl;
	public String serviceUrl;
	public String serviceName;
	public String wsNamespace;
	public String soapPortName;

	public void readWSDL(){
		try {
			// Read the WSDL URL from <env-entry>
			InitialContext ctx = new InitialContext();
			wsdlUrl = (String) ctx.lookup("java:comp/env/wsdlUrl"); 
			LOG.info("Retrieving WSDL from address: " + wsdlUrl);
			ctx.close();

			WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
			reader.setFeature("javax.wsdl.verbose", false);
			reader.setFeature("javax.wsdl.importDocuments", true);

			// Get the whole WSDL file
			Definition def = reader.readWSDL(wsdlUrl);

			// Initialize the variables necessary for Cxf
			initializeVariables(def);

		} catch (WSDLException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	public void initializeVariables(Definition def){

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
}


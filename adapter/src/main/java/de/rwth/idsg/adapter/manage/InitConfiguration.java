package de.rwth.idsg.adapter.manage;


/**
 * This class provides the two endpoint URIs for the Camel route
 * that is defined in the MappingRoute class.
 */
public class InitConfiguration {
	
	/**
	 * Creates the endpoint URI for the Web service.
	 */
	public static String getServiceEndpoint() {
		
		WSDLParser wp = new WSDLParser();
		wp.readWSDL();
		
		return "cxf://" + wp.serviceUrl
				+ "?wsdlURL=" + wp.wsdlUrl
				+ "&serviceName={" + wp.wsNamespace + "}" + wp.serviceName
				+ "&portName={" + wp.wsNamespace + "}" + wp.soapPortName
				+ "&dataFormat=PAYLOAD" 
				+ "&loggingFeatureEnabled=true";		
	}
	
	/**
	 * Creates the endpoint URI for the Java servlet
	 * to consume HTTP requests sent by clients.
	 */
	public static String getServletEndpoint(){
		return "servlet:///?servletName=AdapterServlet";
	}
}

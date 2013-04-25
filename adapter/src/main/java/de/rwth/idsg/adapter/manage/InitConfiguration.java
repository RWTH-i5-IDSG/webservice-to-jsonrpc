package de.rwth.idsg.adapter.manage;


/**
 * Sets the initial configuration.
 */
public class InitConfiguration {
	
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
	
	public static String getServletEndpoint(){
		return "servlet:///?servletName=AdapterServlet";
	}
}

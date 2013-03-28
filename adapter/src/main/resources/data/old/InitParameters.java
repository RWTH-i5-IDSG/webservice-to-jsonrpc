package de.rwth.idsg.adapter.manage;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;

public class InitParameters extends CamelHttpTransportServlet {
	
	private static final long serialVersionUID = 7334832181589258789L;
	
	String WSDLurl = getServletConfig().getInitParameter("WSDLurl");


}

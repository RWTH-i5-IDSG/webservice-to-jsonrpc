package de.rwth.idsg.manager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.manager.ManagerServlet;
import org.apache.catalina.util.ContextName;
import org.apache.tomcat.util.res.StringManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AdapterManagerServlet extends ManagerServlet {

	private static final long serialVersionUID = 4296334930323641322L;
	
    /**
     * Process a GET request for the specified resource.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws IOException, ServletException{

		printPage(response, "Everything OK");
	}
	
    /**
     * Process a POST request for the specified resource.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		StringManager smClient = getStringManager(request);
		
		// Get the request details
		String command 		= request.getPathInfo();
		String path 		= request.getParameter("path");
		String pathTail		= request.getParameter("pathTail");
		String deployPath	= "/adapter-" + pathTail;
		String deployWar	= request.getParameter("deployWar");
		String wsdlUrl		= request.getParameter("wsdlUrl");	
		
		ContextName cn = null;
		if (path != null) {
			cn = new ContextName(path, request.getParameter("version"));
		}

		ContextName deployCn = null;
		if (deployPath != null) {
			deployCn = new ContextName(deployPath, request.getParameter("deployVersion")); 
		}
		
		String message = "";

		if (command == null || command.length() == 0) {
			// Do nothing and print the page later anyway
			message = "Everything OK";
		} else if (command.equals("/start")) {
			message = start(cn, smClient);
		} else if (command.equals("/stop")) {
			message = stop(cn, smClient);
		} else if (command.equals("/reload")) {
			message = reload(cn, smClient);
		} else if (command.equals("/undeploy")) {
			message = undeploy(cn, smClient);
		} else if (command.equals("/deploy")) {
			String deployConfig = prepareConfigFile(deployPath, wsdlUrl);
			message = deployInternal(deployConfig, deployCn, deployWar, smClient);
		}
		printPage(response, message);
	}

    /**
     * Create a temporary XML config file based on the user input. 
     * While deploying its content will be read and saved in Tomcat's conf directory.
     *
     * @param deployPath Path to deploy an adapter
     * @param wsdlUrl WSDL URL parameter to be passed to the adapter
     */
	protected String prepareConfigFile(String deployPath, String wsdlUrl){

		String output = "";

		try {
			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = dBuilder.newDocument();

			Element rootElement = doc.createElement("Context");
			rootElement.setAttribute("path", deployPath);
			doc.appendChild(rootElement);
			
			// Create an Environment element to store WSDL URL
			Element wsdlElement = doc.createElement("Environment");
			wsdlElement.setAttribute("name", "wsdlUrl");
			wsdlElement.setAttribute("type", "java.lang.String");
			wsdlElement.setAttribute("value", wsdlUrl);
			rootElement.appendChild(wsdlElement);
			
			// Create an Environment element to store the absolute path of the adapter.
			// This is only used as the path to save the result of WSDL--(XSLT)-->HTML transformation.
			Element dirElement = doc.createElement("Environment");
			dirElement.setAttribute("name", "dirPath");
			dirElement.setAttribute("type", "java.lang.String");
			dirElement.setAttribute("value", getAppBase().getAbsolutePath() + deployPath);		
			rootElement.appendChild(dirElement);

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"no");
			
			// Config file will be saved in a temporary directory with a random name 
			int randomPosNumber = Math.abs(new Random().nextInt());
			String randomString = String.valueOf(randomPosNumber);
			File tempdir = new File(System.getProperty("java.io.tmpdir"));
			File configFile = new File(tempdir, randomString + ".xml");

			// Save the document 
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(configFile);
			transformer.transform(source, result);
			
			// Get the path to pass to Tomcat while deploying
			output = configFile.getAbsolutePath();

		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		return output;
	}
	
	
    /**
     * Print an HTML page to display
     * 
     * @param response The servlet response we are creating
     * @param message The response message after a command is executed
     */
	protected void printPage(HttpServletResponse response, String message) 
			throws IOException{

		response.setContentType("text/html");
		PrintWriter writer = response.getWriter();

		// Start writing
		writer.println("<!DOCTYPE html>");
		writer.println("<html>");
		writer.println("<head>");
		writer.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"/adapter-manager/index.css\">");
		writer.println("<title>/adapter-manager</title>");
		writer.println("</head>");
		writer.println("<body>");

		// Write the banner table
		writer.println("<table class=\"head\"><tr><td>");
		writer.println("<a href=\"/adapter-manager/\"><h1>Manager for SOAP/JSON-RPC Adapter</h1></a>");
		writer.println("</td></tr></table>");
		writer.println("<br>");

		// Write the message table
		writer.println("<table><tr>");
		writer.println("<td class=\"messageTitle\">Message</td>");
		writer.println("<td class=\"message\">" + message + "</td>");
		writer.println("</tr></table>");
		writer.println("<br>");

		// Write the apps table		
		writer.println("<table>");
		writer.println("<tr><th>Adapter applications</th></tr>");
		printAppsTable(writer);
		writer.println("</table>");
		writer.println("<br>");

		// Write the container table		
		writer.println("<table>");
		writer.println("<tr><th>Deploy a new adapter</th></tr>");
		writer.println("<tr><td>");	

		// Write the deploy table
		writer.println("<form method=\"POST\" action=\"/adapter-manager/main/deploy\">");
		writer.println("<table class=\"deploy\"><tr>");		
		writer.println("<td class=\"deployRows\">Context path for the adapter :</td>");
		writer.println("<td class=\"fixedWidth\">/adapter-</td>");
		writer.println("<td><input type=\"text\" name=\"pathTail\"></td>");	
		writer.println("</tr><tr>");		

		writer.println("<td class=\"deployRows\">WAR file URL of the adapter :</td>");
		writer.println("<td colspan=\"2\"><input type=\"text\" name=\"deployWar\" value=\"/home/sg/git/webservice-to-jsonrpc/adapter/target/adapter.war\"></td>");				
		writer.println("</tr><tr>");		

		writer.println("<td class=\"deployRows\">WSDL URL of the SOAP Web Service :</td>");
		writer.println("<td colspan=\"2\"><input type=\"text\" name=\"wsdlUrl\"></td>");
		writer.println("</tr><tr>");

		writer.println("<td>&nbsp;</td>");
		writer.println("<td colspan=\"2\"><input type=\"submit\" value=\"Deploy\"></td>");
		writer.println("</tr></table></form>");

		// Close container table
		writer.println("</td></tr></table>");
		// Finish writing
		writer.println("</body></html>");

		// Finish up the response
		writer.close();
	}
	
	
    /**
     * Print the table for adapter instances
     * 
     * @param writer The writer that generates the HTML page
     */
	protected void printAppsTable(PrintWriter writer){

		// Search for only adapter contexts and save them in a list
		Container contexts[] = host.findChildren();
		ArrayList<Context> adapterContexts = new ArrayList<Context>();
		for (int i = 0; i < contexts.length; i++){
			Context context = (Context) contexts[i];			
			String path = context.getPath();
			if( path.startsWith("/adapter-") && !path.equals("/adapter-manager") ){
				adapterContexts.add(context);
			}
		} 

		// If there is no adapter
		if( adapterContexts.isEmpty() ){
			writer.println("<tr><td class=\"noApps\">");
			writer.println("No adapter application is running!");
			writer.println("</td></tr>");
			return;
		}

		///// Start printing the apps table /////
		
		writer.println("<tr><td>");
		writer.println("<table class=\"apps\">");
		writer.println("<tr><th>Path</th><th>WSDL URL</th><th>Running</th><th>Commands</th></tr>");

		for (Context context : adapterContexts){		
			String path = context.getPath();
			String wsdlUrl = context.getNamingResources().findEnvironment("wsdlUrl").getValue();
			boolean running = context.getState().isAvailable();

			writer.println("<tr>");
			writer.println("<td class=\"alignLeft\"><a href=\"" + path + "\">" + path + "</a></td>");
			writer.println("<td><a href=\"" + wsdlUrl + "\">" + wsdlUrl + "</a></td>");
			writer.println("<td>" + running + "</td>");
			writer.println("<td class=\"fixedWidth\">");

			// Write command buttons depending on running state)
			if (running){				
				writer.println("<form class=\"inline\">");
				writer.println("<input type=\"submit\" value=\"Start\" disabled></form>");

				writer.println("<form class=\"inline\" method=\"POST\" action=\"/adapter-manager/main/stop?path=" + path + "\">");
				writer.println("<input type=\"submit\" value=\"Stop\"></form>");

				writer.println("<form class=\"inline\" method=\"POST\" action=\"/adapter-manager/main/reload?path=" + path + "\">");
				writer.println("<input type=\"submit\" value=\"Reload\"></form>");
			}else{	
				writer.println("<form class=\"inline\" method=\"POST\" action=\"/adapter-manager/main/start?path=" + path + "\">");
				writer.println("<input type=\"submit\" value=\"Start\"></form>");

				writer.println("<form class=\"inline\">");
				writer.println("<input class=\"inline\" type=\"submit\" value=\"Stop\" disabled></form>");

				writer.println("<form class=\"inline\">");
				writer.println("<input class=\"inline\" type=\"submit\" value=\"Reload\" disabled></form>");
			}

			// Write undeploy button always (independent of running state)
			writer.println("<form class=\"inline\" method=\"POST\" action=\"/adapter-manager/main/undeploy?path=" + path + "\">");
			writer.println("<input type=\"submit\" value=\"Undeploy\"></form>");

			writer.println("</td></tr>");
		}

		writer.println("</table>");
		writer.println("</td></tr>");
				
		// CLEAR VARIABLES
		contexts = null;
		adapterContexts = null;
	}

    /**
     * Note: Taken from org.apache.catalina.manager.HTMLManagerServlet.java
     * 
     * Start the web application at the specified context path.
     *
     * @see ManagerServlet#start(PrintWriter, ContextName, StringManager)
     *
     * @param cn Name of the application to be started
     * @param smClient  StringManager for the client's locale
     * @return message String
     */
	protected String start(ContextName cn, StringManager smClient) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		super.start(printWriter, cn, smClient);
		return stringWriter.toString();
	}

    /**
     * Note: Taken from org.apache.catalina.manager.HTMLManagerServlet.java
     * 
     * Stop the web application at the specified context path.
     *
     * @see ManagerServlet#stop(PrintWriter, ContextName, StringManager)
     *
     * @param cn Name of the application to be stopped
     * @param smClient  StringManager for the client's locale
     * @return message String
     */
	protected String stop(ContextName cn, StringManager smClient) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		super.stop(printWriter, cn, smClient);
		return stringWriter.toString();
	}
	
    /**
     * Note: Taken from org.apache.catalina.manager.HTMLManagerServlet.java
     * 
     * Reload the web application at the specified context path.
     *
     * @see ManagerServlet#reload(PrintWriter, ContextName, StringManager)
     *
     * @param cn Name of the application to be restarted
     * @param smClient  StringManager for the client's locale
     * @return message String
     */
	protected String reload(ContextName cn, StringManager smClient) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		super.reload(printWriter, cn, smClient);
		return stringWriter.toString();
	}
	
    /**
     * Note: Taken from org.apache.catalina.manager.HTMLManagerServlet.java
     * 
     * Undeploy the web application at the specified context path.
     *
     * @see ManagerServlet#undeploy(PrintWriter, ContextName, StringManager)
     *
     * @param cn Name of the application to be undeployed
     * @param smClient  StringManager for the client's locale
     * @return message String
     */
	protected String undeploy(ContextName cn, StringManager smClient) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		super.undeploy(printWriter, cn, smClient);
		return stringWriter.toString();
	}

    /**
     * Note: Taken from org.apache.catalina.manager.HTMLManagerServlet.java
     * 
     * Deploy an application for the specified path from the specified
     * web application archive.
     *
     * @param config URL of the context configuration file to be deployed
     * @param cn Name of the application to be deployed
     * @param war URL of the web application archive to be deployed
     * @return message String
     */
	protected String deployInternal(String config, ContextName cn, String war, StringManager smClient) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		super.deploy(printWriter, config, cn, war, false, smClient);
		return stringWriter.toString();
	}
}

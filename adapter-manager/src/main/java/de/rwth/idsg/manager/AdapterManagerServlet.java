package de.rwth.idsg.manager;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.manager.ManagerServlet;
import org.apache.catalina.util.ContextName;
import org.apache.tomcat.util.res.StringManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Random;


/**
 * This class provides a Web interface to manage adapter instances.
 *
 */
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
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
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
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        StringManager smClient = getStringManager(request);

        // Get the request details
        String command = request.getPathInfo();
        String path = request.getParameter("path");

        ContextName cn = null;
        if (path != null) {
            cn = new ContextName(path, request.getParameter("version"));
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
            // Get deploy parameters
            String pathTail = request.getParameter("pathTail");
            String deployWar = request.getParameter("deployWar");
            String wsdlUrl = request.getParameter("wsdlUrl");

            if (pathTail.isEmpty() || deployWar.isEmpty() || wsdlUrl.isEmpty()) {
                message = "FAIL - One or more input fields were empty";
            } else {
                String deployPath	= "/adapter-" + pathTail;
                ContextName deployCn = new ContextName(deployPath, request.getParameter("deployVersion"));
                String deployConfig = prepareConfigFile(deployPath, wsdlUrl);
                message = deployInternal(deployConfig, deployCn, deployWar, smClient);
            }
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
    protected String prepareConfigFile(String deployPath, String wsdlUrl) {
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
     * Print an HTML page to display.
     * 
     * @param response The servlet response we are creating
     * @param message The response message after a command is executed
     */
    protected void printPage(HttpServletResponse response, String message) throws IOException {

        String page = "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>"
                + "<link rel=\"stylesheet\" type=\"text/css\" href=\"/adapter-manager/index.css\">"
                + "<title>/adapter-manager</title>"
                + "</head>\n"
                + "<body>\n"

                // The banner table
                + "<table class=\"head\"><tr><td>"
                + "<a href=\"/adapter-manager/\"><h1>Manager for SOAP/JSON-RPC Adapter</h1></a>"
                + "</td></tr></table>"
                + "<br>\n"

                // The message table
                + "<table><tr><td class=\"messageTitle\">Message</td><td class=\"message\">" + message + "</td></tr></table>"
                + "<br>\n"

                // The apps table
                + "<table>"
                + "<tr><th>Adapter applications</th></tr>\n"
                + printAppsTable()
                + "</table>\n"
                + "<br>\n"

                // The container table
                + "<!-- Deployment table -->\n"
                + "<table><tr><th>Deploy a new adapter</th></tr><tr><td>\n"

                // The deploy table
                + "<form method=\"POST\" action=\"/adapter-manager/main/deploy\">\n"
                + "<table class=\"deploy\">\n"
                + "<tr>"
                + "<td class=\"deployRows\">Context path for the adapter :</td>"
                + "<td class=\"fixedWidth\">/adapter-</td>"
                + "<td><input type=\"text\" name=\"pathTail\"></td></tr>\n"
                + "<tr>"
                + "<td class=\"deployRows\">WAR file URL of the adapter :</td>"
                + "<td colspan=\"2\"><input type=\"text\" name=\"deployWar\" placeholder=\"Path to the template adapter.war\"></td></tr>\n"
                + "<tr>"
                + "<td class=\"deployRows\">WSDL URL of the SOAP Web Service :</td>"
                + "<td colspan=\"2\"><input type=\"text\" name=\"wsdlUrl\"></td></tr>\n"
                + "<tr>"
                + "<td>&nbsp;</td><td colspan=\"2\"><input type=\"submit\" value=\"Deploy\"></td></tr>\n"
                + "</table></form>\n"

                // Close container table
                + "</td></tr></table>\n"
                // Close HTML
                + "</body>\n</html>";

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.write(page);
        writer.close();  
    }

    /**
     * Print the table for adapter instances.
     */
    protected String printAppsTable() {

        // Search for only adapter contexts and save them in a list
        Container contexts[] = host.findChildren();
        ArrayList<Context> adapterContexts = new ArrayList<Context>();
        for (Container context1 : contexts) {
            Context context = (Context) context1;
            String path = context.getPath();
            if (path.startsWith("/adapter-") && !path.equals("/adapter-manager")) {
                adapterContexts.add(context);
            }
        }

        // If there is no adapter
        if (adapterContexts.isEmpty()) {
            return "<tr><td class=\"noApps\">No adapter application is running!</td></tr>\n";
        }

        ///// Start printing the adapter instances /////

        StringBuilder builder = new StringBuilder("<tr><td>\n<table class=\"apps\">"
                + "<tr><th>Path</th><th>WSDL URL</th><th>Running</th><th>Commands</th></tr>\n");

        for (Context context : adapterContexts) {
            String path = context.getPath();
            String wsdlUrl = context.getNamingResources().findEnvironment("wsdlUrl").getValue();
            boolean running = context.getState().isAvailable();

            builder.append("<!-- Adapter instance -->\n"
                    + "<tr>\n"
                    + "<td class=\"alignLeft\"><a href=\"" + path + "\">" + path + "</a></td>"
                    + "<td><a href=\"" + wsdlUrl + "\">" + wsdlUrl + "</a></td>"
                    + "<td>" + running + "</td>\n"
                    + "<td class=\"fixedWidth\">\n");

            // Write command buttons depending on running state)
            if (running) {
                builder.append("<form class=\"inline\">"
                        + "<input type=\"submit\" value=\"Start\" disabled></form>\n"
                        + "<form class=\"inline\" method=\"POST\" action=\"/adapter-manager/main/stop?path=" + path + "\">"
                        + "<input type=\"submit\" value=\"Stop\"></form>\n"
                        + "<form class=\"inline\" method=\"POST\" action=\"/adapter-manager/main/reload?path=" + path + "\">"
                        + "<input type=\"submit\" value=\"Reload\"></form>\n");
            } else {
                builder.append("<form class=\"inline\" method=\"POST\" action=\"/adapter-manager/main/start?path=" + path + "\">"
                        + "<input type=\"submit\" value=\"Start\"></form>\n"
                        + "<form class=\"inline\">"
                        + "<input class=\"inline\" type=\"submit\" value=\"Stop\" disabled></form>\n"
                        + "<form class=\"inline\">"
                        + "<input class=\"inline\" type=\"submit\" value=\"Reload\" disabled></form>\n");
            }

            // Write undeploy button always (independent of running state)
            builder.append("<form class=\"inline\" method=\"POST\" action=\"/adapter-manager/main/undeploy?path=" + path + "\">"
                    + "<input type=\"submit\" value=\"Undeploy\"></form>\n"
                    + "</td>\n</tr>\n");
        }

        builder.append("</table>\n</td></tr>");
        return builder.toString();
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

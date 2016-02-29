package de.rwth.idsg.adapter.soap2json;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.cxf.CxfPayload;
import org.apache.cxf.binding.soap.SoapHeader;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.List;

/**
 * The main class that handles Camel exchanges to initiate and manage the mapping process of responses.
 */
public class ResponseProcessor implements Processor {

    @Override
    public void process(Exchange exchange)
            throws ParserConfigurationException, XMLStreamException, IOException {

        // Read the exchange into a SOAP payload.
        @SuppressWarnings("unchecked")
        CxfPayload<SoapHeader> inputPayload = exchange.getIn().getBody(CxfPayload.class);

        // Assumption: Since JSON-RPC can send only one method request at a time,
        // the soap body should contain only one response element.
        // Get the body element of payload.
        Element inputBodyElement = inputPayload.getBody().get(0);

        ResponseObjectCreator roc = new ResponseObjectCreator();

        // Soap headers may exist. Proceed accordingly.
        List<SoapHeader> inputHeadersList = inputPayload.getHeaders();
        byte[] response;
        if (!inputHeadersList.isEmpty()) {

            Element inputHeadersElement = DocumentBuilderFactory.newInstance()
                                                                .newDocumentBuilder()
                                                                .newDocument()
                                                                .createElement("SOAP-HEADER");

            // Headers might have multiple elements.
            for (SoapHeader anInputHeadersList : inputHeadersList) {
                Element item = (Element) anInputHeadersList.getObject();
                inputHeadersElement.appendChild(item);
            }

            // Create a JSON-RPC response WITH HEADER.
            response = roc.createNormalResponse(
                    inputHeadersElement,
                    inputBodyElement,
                    exchange.getProperty("jsonrpc-id"));

        } else {
            // Create a JSON-RPC response WITHOUT HEADER.
            response = roc.createNormalResponse(
                    inputBodyElement,
                    exchange.getProperty("jsonrpc-id"));

        }

        // Set the output body to response.
        exchange.getOut().setBody(response);

        // Set the content type (although it is not essential).
        exchange.getOut().setHeader(Exchange.CONTENT_TYPE, "application/json");
    }
}

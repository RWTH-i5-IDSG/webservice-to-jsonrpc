package de.rwth.idsg.adapter.json2soap;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.rwth.idsg.adapter.soap2json.ResponseObjectCreator;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * The processor to create a JSON-RPC error message when method does not exist at WS.
 */
public class BOIExceptionProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws JsonProcessingException {
        ResponseObjectCreator roc = new ResponseObjectCreator();

        byte[] error = roc.createErrorResponse(
                -32601,
                "Method not found",
                null,
                exchange.getProperty("jsonrpc-id"));

        exchange.getOut().setBody(error);
    }
}

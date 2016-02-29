package de.rwth.idsg.adapter.common;


/**
 * This class describes an exception for the adapter to throw when JSON-RPC requests do not conform to the
 * specification.
 */
public class JsonRpcSyntaxException extends Exception {

    private static final long serialVersionUID = 3722064220984493914L;

    public JsonRpcSyntaxException() { }

    public JsonRpcSyntaxException(String msg) {
        super(msg);
    }

}

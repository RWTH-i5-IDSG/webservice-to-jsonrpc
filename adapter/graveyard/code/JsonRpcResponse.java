package de.rwthaachen.idsg.adapter.soap2json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;


/**
 * This is the POJO for a jsonrpc response object.
 * NOT USED ANYMORE
 */

public class JsonRpcResponse {

	private static final String JSON_RPC_VERSION = "2.0";
	private JsonNode id;
	private JsonNode result;
	private String error;
	
	@JsonProperty("result")
	@JsonSerialize(include = Inclusion.NON_NULL)
	public void setResult(JsonNode result){
		this.result = result;
	}	
		
	@JsonProperty("id")
    public void setID(JsonNode id){
        this.id = id;
    }
	
	@JsonProperty("error")
	@JsonSerialize(include = Inclusion.NON_NULL)
	public void setError(String error){
		this.error = error;
	}
	
	@JsonProperty("jsonrpc")
	public String getJsonRpcVersion() { return JSON_RPC_VERSION; }
	public JsonNode getId() { return id; }
	public JsonNode getResult() { return result; }	
	public String getError() { return error; }
}

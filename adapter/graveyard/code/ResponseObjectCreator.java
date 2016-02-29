//package de.rwth.idsg.adapter.soap2json;
//
//import java.util.HashMap;
//
//import org.w3c.dom.Element;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//
//import de.rwth.idsg.adapter.common.StaxonConverter;
//
//public class ResponseObjectCreator {
//
//	ObjectMapper mapper = new ObjectMapper();
//
//	/**
//	 * Writes a JSON-RPC 2.0 response object WITHOUT HEADER when everything goes okay
//	 * 
//	 * @param body			The soap body element
//	 * @param idHeader		JSON-RPC id
//	 */
//	public byte[] createNormalResponse(Element body, Object idHeader) throws Exception {		
//
//		// Convert the soap body element to json.
//		JsonNode resultNode = mapper.readTree(StaxonConverter.convertToJson(body))
//									.get(body.getNodeName());
//		
//		// If the result tree has only one child node, get its value
//		if ( resultNode.size() == 1 ) resultNode = resultNode.elements().next();
//
//		ObjectNode outputJson = mapper.createObjectNode();
//		outputJson.put("jsonrpc", "2.0");
//		outputJson.put("result", resultNode);
//		outputJson.put("id", (JsonNode) idHeader);
//
//		return mapper.writeValueAsBytes(outputJson);
//	}
//
//	/**
//	 * Writes a JSON-RPC 2.0 response object WITH HEADER when everything goes okay
//	 * 
//	 * @param header		The soap header element
//	 * @param body			The soap body element
//	 * @param idHeader		JSON-RPC id
//	 */
//	public byte[] createNormalResponse(Element header, Element body, Object idHeader) throws Exception {
//
//		// Convert the soap header element to json
//		ObjectNode headerNode = mapper.readValue(StaxonConverter.convertToJson(header), ObjectNode.class);
//
//		// Convert the soap body element to json.
//		ObjectNode resultNode = mapper.readValue(StaxonConverter.convertToJson(body), ObjectNode.class);
//
//		ObjectNode resultObject = mapper.createObjectNode();
//		resultObject.putAll(headerNode);
//		resultObject.putAll(resultNode);
//
//		ObjectNode outputJson = mapper.createObjectNode();
//		outputJson.put("jsonrpc", "2.0");
//		outputJson.put("result", resultObject);
//		outputJson.put("id", (JsonNode) idHeader);
//
//		return mapper.writeValueAsBytes(outputJson);
//	}
//
//	/**
//	 * Writes a JSON-RPC 2.0 response object when a RPC call encounters an error
//	 * 
//	 * @param code		Error code
//	 * @param message	Error message
//	 * @param data		Details about error (if any) 
//	 * @param idHeader	JSON-RPC id
//	 */
//	public byte[] createErrorResponse(int code, String message, HashMap<String, String> data, Object idHeader) {
//
//		ObjectNode errorObject = mapper.createObjectNode();
//		errorObject.put("code", code);
//		errorObject.put("message", message);
//		if ( data != null ) errorObject.put("data", mapper.valueToTree(data));
//
//		ObjectNode outputJson = mapper.createObjectNode();
//		outputJson.put("jsonrpc", "2.0");
//		outputJson.put("error", errorObject);
//		outputJson.put("id", (JsonNode) idHeader);
//		
//		byte[] output = null;
//		try {
//			output = mapper.writeValueAsBytes(outputJson);
//		} catch (JsonProcessingException e) {
//			e.printStackTrace();
//		}
//		return output;
//	}
//}

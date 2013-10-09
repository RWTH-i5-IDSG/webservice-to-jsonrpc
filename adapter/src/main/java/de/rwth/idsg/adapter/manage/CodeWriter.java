package de.rwth.idsg.adapter.manage;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.wsdl.Operation;

/**
 * This class provides functionality to generates an Objective-C client 
 * stub and corresponding XCode sample project for the web service adapter. 
 * It runs during initialization of the adapter and depends on data the 
 * WSDLParser class extracts from the web services's WSDL descriptor. 
 * 
 * @author  Lars C. Gleim <lars.gleim@rwth-aachen.de>
 * @since   2013-09-09
 * @version 1.0
 * 
 */

class CodeWriter {
	/**
	 * Stores the code that gives a usage stub for each supported method
	 */
	String codeUsage;
	
	/**
	 * The header file code of the actual web service adapter
	 */
	String codeHead; 
	
	/**
	 * The implementation of the actual web service adapter
	 */
	String codeImpl;
	
	/**
	 * The resource URL of the web service's WSDL description file
	 */
	String wsdlUrl;

	
	/**
	 * Initializes a new CodeWriter
	 * 
	 * Adds initial static stub code to the respective variables
	 * 
	 * @param url The resource URL of the web service's WSDL description file
	 */
	CodeWriter(String url) {
		wsdlUrl = url;
		
		// Initialize the usage example code ViewController.m
		codeUsage = 
				"// Instantiate the Adapter\n" +
				"self._soapAdapter = [SoapAdapter new];\n\n" + 
				"// We're going to use a standard completion handler for our json-rpc calls - Please feel free to define your own\n" + 
				"DSJSONRPCCompletionHandler completionHandler = ^(NSString *methodName, NSInteger callId, id methodResult, DSJSONRPCError *methodError, NSError *internalError) {\r\n" + 
				"    if      (methodError)  {NSLog(@\"\\nMethod %@(%i) returned an error: %@\\n\\n\", methodName, callId, methodError);}\r\n" + 
				"    else if (internalError){NSLog(@\"\\nMethod %@(%i) couldn't be sent with error: %@\\n\\n\", methodName, callId, internalError);}\r\n" + 
				"    else                   {NSLog(@\"\\nMethod %@(%i) completed with result: %@\\n\\n\", methodName, callId, methodResult);}\r\n" + 
				"};\n\n" +
				"// You can store the generated call id to match up responses\n" + 
				"NSInteger callId;\n\n";
		
		// Initialize the header file code SoapAdapter.h
		codeHead = 
				"#import <Foundation/Foundation.h>\n" +
				"#import \"DSJSONRPC.h\"\n\n" +
				"@class SoapAdapter;\n\n" + 
				"@interface SoapAdapter : NSObject\n\n";
		// Initialize the implementation file code SoapAdapter.m
		codeImpl = 
				"#import \"SoapAdapter.h\"\r\n" + 
				"#import \"DSJSONRPC.h\"\r\n\r\n" + 
				"@interface SoapAdapter ()\r\n" + 
				"@property (strong, nonatomic) DSJSONRPC *_jsonRPC;\r\n" + 
				"@end\r\n\r\n\r\n" + 
				"@implementation SoapAdapter\n" +
				"-(id) init {\r\n" + 
				"    if (!(self = [super init])) return self;\r\n" + 
				"    self._jsonRPC = [[DSJSONRPC alloc] initWithServiceEndpoint:[NSURL URLWithString:@\"" +
				getAdapterAddress() +
				"\"]];\r\n" + 
				"    return self;\r\n" + 
				"}\r\n" + 
				"- (void)dealloc {\r\n" + 
				"    DS_RELEASE(_jsonRPC)\r\n" + 
				"    DS_SUPERDEALLOC()\r\n" + 
				"}\n\n;";
	}
	
	
	
	/**
	 * Creates a new adapter method for the given web service operation
	 * 
	 * @param op     The javax.wsdl.Operation object representing the web service's operation that should be added
	 * @param params A string representation of the parameter structure that may be passed to the web service's operation as provided by WSDLParser.getParameterDetails(String, QName, QName)
	 * @param output A string representation of the object structure that may be returned by the web service's operation as provided by WSDLParser.getParameterDetails(String, QName, QName)
	 */
	void addOperation(Operation op, String params, String output) {

		// Documentation
		addDocumentation(op, output);

		if (params.equals(""))  {
			if(output.equals("")) {
				noInputNoOutputWriter(op);
			} else {
				noInputHasOutputWriter(op, output);
			}
		} else {
			if(output.equals("")) {
				hasInputNoOutputWriter(op, params);
			} else {
				hasInputHasOutputWriter(op, params, output);
			}
		}
		codeUsage += "\n\n\n";
	}
	
	
	
	/**
	 * Writes the generated code stubs to the servlet's resource path
	 * 
	 * @param dirPath The file system path that should be written to
	 */
	void write(String dirPath) {
		codeHead += "@end\n";
		codeImpl += "@end\n";

		toFile(dirPath, "ObjC.html", htmlDocumentation());
		
		// Include Imports etc. for Sample Project
		finalizeUsageCode(); 
		
		toFile(dirPath, "SoapAdapter.m", codeImpl);
		toFile(dirPath, "SoapAdapter.h", codeHead);
		toFile(dirPath, "ViewController.m", codeUsage);
		
		File[] files = {new File(dirPath + "/SoapAdapter.m"),
						new File(dirPath + "/SoapAdapter.h"),
						new File(dirPath + "/ViewController.m")};
		addFilesToZip(new File(dirPath+"/objc.zip"), files, "objc/json-rpc-demo/");
	}
	
	
	
	/**
	 * Code stub generation for a one-way message without parameters
	 * @param op The javax.wsdl.Operation object representing the web service's operation that should be added
	 */
	private void noInputNoOutputWriter(Operation op){
		// Operation without Parameters and without Output
		codeImpl += 
				"- (NSInteger) "+op.getName()+" {\n" + 
				"    return [self._jsonRPC callMethod:@\""+op.getName()+"\"];\n" + 
				"}\n";
		
		codeHead += 
				"- (NSInteger) "+op.getName()+";\n";
		
		codeUsage += 
				"callId = [self._soapAdapter "+op.getName()+"];";
	}
	
	
	
	/**
	 * Code stub generation for a request-response message without parameters
	 * @param op     The javax.wsdl.Operation object representing the web service's operation that should be added
	 * @param output A string representation of the object structure that may be returned by the web service's operation as provided by WSDLParser.getParameterDetails(String, QName, QName)
	 */
	private void noInputHasOutputWriter(Operation op, String output){
		// No Input, Has Output
		codeImpl += 
				"- (NSInteger) "+op.getName()+":(DSJSONRPCCompletionHandler)completionHandler {\n" + 
				"    return [self._jsonRPC callMethod:@\""+op.getName()+"\" onCompletion:completionHandler];\n" + 
				"}\n";
		
		codeHead += 
				"- (NSInteger) "+op.getName()+":(DSJSONRPCCompletionHandler)completionHandler;\n";
		
		//codeUsage += "/* Can Also be Called Neglecting All Responses: \n";
		//noInputNoOutputWriter(op);
		codeUsage += " */\n" +
				"callId = [self._soapAdapter "+op.getName()+":completionHandler];\n";
	}
	
	
	
	/**
	 * Code stub generation for a one-way message with parameters
	 * @param op     The javax.wsdl.Operation object representing the web service's operation that should be added
	 * @param params A string representation of the parameter structure that may be passed to the web service's operation as provided by WSDLParser.getParameterDetails(String, QName, QName)
	 */
	private void hasInputNoOutputWriter(Operation op, String params){
		// Has Input, No Output
		codeImpl += 
				"- (NSInteger) "+op.getName()+":(id)methodParams {\n" + 
				"    return [self._jsonRPC callMethod:@\""+op.getName()+"\" withParameters:methodParams];\n" + 
				"}\n";
		
		codeHead += 
				"- (NSInteger) "+op.getName()+":(id)methodParams;\n";
		
		codeUsage += parameterDictionary(op,params) +
				"callId = [self._soapAdapter "+op.getName()+":"+op.getName()+"Params];";
	}
	
	
	
	/**
	 * Code stub generation for a request-response message with parameters
	 * @param op     The javax.wsdl.Operation object representing the web service's operation that should be added
	 * @param params A string representation of the parameter structure that may be passed to the web service's operation as provided by WSDLParser.getParameterDetails(String, QName, QName)
	 * @param output A string representation of the object structure that may be returned by the web service's operation as provided by WSDLParser.getParameterDetails(String, QName, QName)
	 */
	private void hasInputHasOutputWriter(Operation op, String params, String output){
		// Has Input, Has Output
		codeImpl += 
				"- (NSInteger) "+op.getName()+":(id)methodParams withHandler:(DSJSONRPCCompletionHandler)completionHandler {\n" + 
				"    return [self._jsonRPC callMethod:@\""+op.getName()+"\" withParameters:methodParams onCompletion:completionHandler];\n" + 
				"}\n";
		
		codeHead += 
				"- (NSInteger) "+op.getName()+":(id)methodParams withHandler:(DSJSONRPCCompletionHandler)completionHandler;\n";
		
		//codeUsage += "/* Can Also be Called Neglecting All Responses: \n";
		//hasInputNoOutputWriter(op,"");
		codeUsage += " */\n";
		
		codeUsage += parameterDictionary(op,params) +
				"callId = [self._soapAdapter "+op.getName()+":"+op.getName()+"Params withHandler:completionHandler];\n";

	}
	
	
	
	/**
	 * Adds further static code to the Objective-C usage stub that is needed to generate a valid XCode project.
	 */
	private void finalizeUsageCode(){
		codeUsage =
				"/**\n" + 
				" * Code Stub Generated by WebService to JSON-RPC Adapter from WSDL:\n" + 
				" * " + wsdlUrl + "\n" +
				" */ \n" + 
				"\n" +
				"#import \"ViewController.h\"\r\n" + 
				"#import \"SoapAdapter.h\"\r\n" + 
				"\r\n" + 
				"@interface ViewController ()\r\n" + 
				"\r\n" + 
				"@property (strong, nonatomic) SoapAdapter *_soapAdapter;" + 
				"\n" + 
				"@end\n" + 
				"\n" + 
				"@implementation ViewController\n" + 
				"\n" + 
				"- (void)viewDidLoad\n" + 
				"{\n" + 
				"    [super viewDidLoad];\n" + 
				
				codeUsage +
				
				"}\n\n" +
				"- (void)viewDidUnload\n" + 
				"{\n" + 
				"    [super viewDidUnload];\n" + 
				"    // Release any retained subviews of the main view.\n" + 
				"}\n" + 
				"\n" + 
				"- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation\n" + 
				"{\n" + 
				"    return (interfaceOrientation != UIInterfaceOrientationPortraitUpsideDown);\n" + 
				"}\n" + 
				"\n" + 
				"@end\n";
	}
	
	

	/**
	 * Creates a Description Text/Documentation for an Operation
	 * @param op     The javax.wsdl.Operation object representing the web service's operation that should be added 
	 * @param output A string representation of the object structure that may be returned by the web service's operation as provided by WSDLParser.getParameterDetails(String, QName, QName)
	 */
	private void addDocumentation(Operation op, String output){
		codeUsage +=
				"/**\n * Operation: \"" + op.getName() + "\"\n" +
				((op.getDocumentationElement()== null 
					|| op.getDocumentationElement().getNodeValue()==null) 
						? "" // " * No Documentation or Parsing Failed\n" 
						: (" * Description: " + op.getDocumentationElement().getNodeValue()+"\n")) +	
				(output.equals("") ? "" : (" * @returns: @{\n" + output + "   }\n")) +
				" */\n";
	}	


	
	/**
	 * Generates an html document that displays nicely formatted stub code for developers
	 * 
	 * Depends on codeUsage, codeHead and codeImplementation to already be correctly generated
	 * 
	 * @return A String containing the html document
	 */
	private String htmlDocumentation(){
		return 	
		"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\r\n" + 
		"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n" +
		"<head>\r\n" +
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\r\n" +
			"<script type=\"text/javascript\" src=\"res/shCore.js\"></script>\r\n" + 
			"<script type=\"text/javascript\" src=\"res/shBrushObjC.js\"></script>\r\n" + 
			"<link type=\"text/css\" rel=\"stylesheet\" href=\"res/shCoreDefault.css\"/>\r\n" +
			"<link type=\"text/css\" rel=\"stylesheet\" href=\"res/style.css\"/>\r\n" +
			"<link type=\"text/css\" rel=\"stylesheet\" href=\"res/shThemeDefault.css\"/>\r\n" +
			"<script type=\"text/javascript\">SyntaxHighlighter.all();</script>\r\n" +
		"</head>" +
		"<body>\r\n" +
			"<h1 align=\"center\">Objective-C Stubs</h1>\r\n" +
			"<h3>Please note, that the generated stub code depends on <a href=\"https://github.com/dbowen/Demiurgic-JSON-RPC\">Demiurgic JSON-RPC</a> by Derek Bowen</h3>" +
			"<h3>Download xCode stub project for this webservice: <a href=\"objc.zip\">objc.zip</a> - You may have to adapt the adapter resource URL / IP manually in SoapAdapter.m!</h3>\r\n" + 
			"<h2>Code Samples</h2>\r\n" + 
			"<h3>Alternatively you may copy the following code to your Project. Double click to select all code for Copy&amp;Paste</h3>" +
			"<h1>Usage</h1>" + 
			"<script type=\"syntaxhighlighter\" class=\"brush: objc;\" height=\"600px\"><![CDATA[\r\n" + codeUsage + "]]></script>\r\n" + 
			"<h1>SoapAdapter.h</h1>" + 
			"<script type=\"syntaxhighlighter\" class=\"brush: objc;\" height=\"600px\"><![CDATA[\r\n" + codeHead + "]]></script>\r\n" + 
			"<h1>SoapAdapter.m</h1>" + 
			"<script type=\"syntaxhighlighter\" class=\"brush: objc;\" height=\"600px\"><![CDATA[\r\n" + codeImpl + "]]></script>\r\n" + 
		"</body>" +
		"</html>";
	}
	
	/**
	 * Generates a NSMutableDictionary Object (part of the Cocoa Foundation Framework) from the parameters object
	 * @param op     The javax.wsdl.Operation object representing a web service operation 
	 * @param params A string representation of the parameter structure that may be passed to the web service's operation as provided by WSDLParser.getParameterDetails(String, QName, QName)
	 * @return       A NSMutuableDictionary representation of the parameters
	 */
	private String parameterDictionary(Operation op, String params) {
		return (params.equals(""))
					? ""
					: "NSMutableDictionary *"+op.getName()+"Params = [@{ \n"+params+"} mutableCopy];\n";
	}
	
	/**
	 * Creates a currently Objective-C specific NSDictionary element with key, value and optional comment.
	 * @param key     The entry key
	 * @param value   The entry value
	 * @param comment An optional comment for the entry
	 * @return        The generated NSDictionary
	 */
	static String keyVal(String key, String value, String comment) {
		String out = comment.equals("") ? "" : "    // " + comment;
		//System.out.println(key + ": " + value + out);
		return "    @\""+ key + "\" : @\"" + value + "\"," + out + "\n";
	}
	
/**
 * GENERAL FUNCTIONALITY BELOW	
 */
	
	/**
	 * Writes a String to a file
	 * @param dirPath  The file system path to write to
	 * @param filename The filename that should be used
	 * @param content  The content to write to the file
	 */
	static void toFile(String dirPath, String filename, String content){
		try{
			BufferedWriter out = new BufferedWriter(new FileWriter(dirPath + "/" + filename));
			out.write(content);
			out.close();
			
		} catch (Exception e){e.printStackTrace();}
	}
	
	/**
	 * Copies files into a zip archive
	 * @param archive The zip archive that should be written to
	 * @param files  An array of file that should be copied to the zip archive
	 * @param path   The path inside the zip archive that the files should be written to
	 */
	static void addFilesToZip(File archive, File[] files, String path){
	    try{
	        File tmpZip = File.createTempFile(archive.getName(), null);
	        tmpZip.delete();
	        if(!archive.renameTo(tmpZip)){
	            throw new Exception("Could not make temp file (" + archive.getName() + ")");
	        }
	        byte[] buffer = new byte[4096];
	        ZipInputStream zin = new ZipInputStream(new FileInputStream(tmpZip));
	        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(archive));
	        for(int i = 0; i < files.length; i++){
	            InputStream in = new FileInputStream(files[i]);
	            out.putNextEntry(new ZipEntry(path + files[i].getName()));
	            for(int read = in.read(buffer); read > -1; read = in.read(buffer)){
	                out.write(buffer, 0, read);
	            }
	            out.closeEntry();
	            in.close();
	        }
	        for(ZipEntry ze = zin.getNextEntry(); ze != null; ze = zin.getNextEntry()){
	            if(!zipEntryMatch(ze.getName(), files, path)){
	                out.putNextEntry(ze);
	                for(int read = zin.read(buffer); read > -1; read = zin.read(buffer)){
	                    out.write(buffer, 0, read);
	                }
	                out.closeEntry();
	            }
	        }
	        out.close();
	        zin.close();
	        tmpZip.delete();
	    }catch(Exception e){
	        e.printStackTrace();
	    }
	}

	
	/**
	 * Helper function to check if a certain file already exists
	 * @param zeName The filename (inside the zip archive) to check
	 * @param files  The file array that should be checked for equality
	 * @param path   The zip archive path to search in
	 * @return       Returns true if a match was found
	 */
	private static boolean zipEntryMatch(String zeName, File[] files, String path){
	    for(int i = 0; i < files.length; i++){
	        if((path + files[i].getName()).equals(zeName)){
	            return true;
	        }
	    }
	    return false;
	}
	
	
	/**
	 * Tries to at least half way determine the host address
	 * @return The adapter URL
	 */
	static String getAdapterAddress(){
		try {
			return "http://" + InetAddress.getLocalHost().getHostAddress() + ":8080/adapter-test/request/";
		} catch (UnknownHostException e) {
			return "http://localhost:8080/adapter-test/request/";
		}
	}
}

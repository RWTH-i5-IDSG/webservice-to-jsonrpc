package de.rwth.idsg.adapter.manage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletContext;
import javax.wsdl.Operation;

public class CodeWriter {
	String codeUsage, codeHead, codeImpl, wsdlUrl;

	CodeWriter(String url) {
		wsdlUrl = url;
		
		// Initialize the usage example code
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
		
		// Initialize the header file code
		codeHead = 
				"#import <Foundation/Foundation.h>\n" +
				"#import \"DSJSONRPC.h\"\n\n" +
				"@class SoapAdapter;\n\n" + 
				"@interface SoapAdapter : NSObject\n\n";
		// Initialize the implementation file code
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
	
	void write() {
		codeHead += "@end\n";
		codeImpl += "@end\n";

		try{
			BufferedWriter html = new BufferedWriter(new FileWriter("C:\\Users\\New\\Desktop\\code.htm"));
			html.write(htmlDocumentation());
			html.close();
			
			finalizeUsageCode();
			BufferedWriter usage = new BufferedWriter(new FileWriter("C:\\Users\\New\\Desktop\\ViewController.m"));
			usage.write(codeUsage);
			usage.close();
			BufferedWriter impl = new BufferedWriter(new FileWriter("C:\\Users\\New\\Desktop\\SoapAdapter.m"));
			impl.write(codeImpl);
			impl.close();
			BufferedWriter head = new BufferedWriter(new FileWriter("C:\\Users\\New\\Desktop\\SoapAdapter.h"));
			head.write(codeHead);
			head.close();

		}catch (Exception e){ //Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
	
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
	private void noInputHasOutputWriter(Operation op, String output){
		// No Input, Has Output
		codeImpl += 
				"- (NSInteger) "+op.getName()+":(DSJSONRPCCompletionHandler)completionHandler {\n" + 
				"    return [self._jsonRPC callMethod:@\""+op.getName()+"\" onCompletion:completionHandler];\n" + 
				"}\n";
		
		codeHead += 
				"- (NSInteger) "+op.getName()+":(DSJSONRPCCompletionHandler)completionHandler;\n";
		
		codeUsage += "/* Can Also be Called Neglecting All Responses: \n";
		noInputNoOutputWriter(op);
		codeUsage += " */\n" +
				"callId = [self._soapAdapter "+op.getName()+":completionHandler];\n";

				
				
	}
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
	private void hasInputHasOutputWriter(Operation op, String params, String output){
		// Has Input, Has Output
		codeImpl += 
				"- (NSInteger) "+op.getName()+":(id)methodParams withHandler:(DSJSONRPCCompletionHandler)completionHandler {\n" + 
				"    return [self._jsonRPC callMethod:@\""+op.getName()+"\" withParameters:methodParams onCompletion:completionHandler];\n" + 
				"}\n";
		
		codeHead += 
				"- (NSInteger) "+op.getName()+":(id)methodParams withHandler:(DSJSONRPCCompletionHandler)completionHandler;\n";
		
		codeUsage += "/* Can Also be Called Neglecting All Responses: \n";
		hasInputNoOutputWriter(op,"");
		codeUsage += " */\n";
		parameterDictionary(op,params);
		codeUsage += "callId = [self._soapAdapter "+op.getName()+":"+op.getName()+"Params withHandler:completionHandler];\n";

	}
	private void finalizeUsageCode(){
		codeUsage = copyright() +
				"#import \"ViewController.h\"\r\n" + 
				"#import \"SoapAdapter.h\"\r\n" + 
				"#import \"DSJSONRPC.h\"\r\n" + 
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
	 * Creates Description Text/Documentation for an Operation
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
	private String htmlDocumentation(){
		return 	
		"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\r\n" + 
		"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n" +
		"<head>\r\n" +
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\r\n" +
			"<script type=\"text/javascript\" src=\"res/shCore.js\"></script>\r\n" + 
			"<script type=\"text/javascript\" src=\"res/shBrushObjC.js\"></script>\r\n" + 
			"<link type=\"text/css\" rel=\"stylesheet\" href=\"res/shCoreDefault.css\"/>\r\n" + 
			"<link type=\"text/css\" rel=\"stylesheet\" href=\"res/shThemeDefault.css\"/>\r\n" +
			"<script type=\"text/javascript\">SyntaxHighlighter.all();</script>\r\n" +
		"</head>" +
		"<body>\r\n" +
			"<p>Double click anywhere within the code windows to select all code for Copy&amp;Paste</p>\r\n" +
			"<h1>Usage</h1>" + 
			"<pre class=\"brush: objc;\" height=\"600px\">\r\n" + codeUsage + "</pre>\r\n" + 
			"<h1>SoapAdapter.h</h1>" + 
			"<pre class=\"brush: objc;\" height=\"600px\">\r\n" + codeHead + "</pre>\r\n" + 
			"<h1>SoapAdapter.m</h1>" + 
			"<pre class=\"brush: objc;\" height=\"600px\">\r\n" + codeImpl + "</pre>\r\n" + 
		"</body>" +
		"</html>";
	}
	private String parameterDictionary(Operation op, String params) {
		return (params.equals(""))
					? ""
					: "NSMutableDictionary *"+op.getName()+"Params = [@{ \n"+params+"} mutableCopy];\n";
	}
	private String copyright() {
		return "/**\n" + 
				" * Code Generated By WebService to JSONRpc Adapter\n" + 
				" * Source WSDL: " + wsdlUrl + "\n" +
				" */ \n" + 
				"\n";
	}
	private String getAdapterAddress(){
		// Trying to at least half way determine the host address
		
		try {
			return "http://" + InetAddress.getLocalHost().getHostAddress() + ":8080/adapter-test/request/";
		} catch (UnknownHostException e) {
			return "http://localhost:8080/adapter-test/request/";
		}
	}
}

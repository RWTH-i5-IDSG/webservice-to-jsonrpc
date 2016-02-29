SOAP/JSON-RPC Adapter
=====================

This tool provides a solution for JSON-RPC clients to use SOAP Web services. The solution is designed as a group of Java servlets to run under Apache Tomcat. It consists of the *manager* servlet and one or more, independent *adapter* servlets.

In addition the Adapter generates easily usable Objective-C code stubs and an extensible X-Code sample project that enable easy integration of SOAP Web Services into iOS and OSX Applications, as well as rapid prototyping.


### The manager

The manager is a Web interface similar to the Apache Tomcat’s HTML Manager. The developer can access the manager using username and password and then deploy, start, stop, reload or undeploy adapters. To deploy an adapter the developer selects the template WAR file of the adapter, enters a name for the adapter instance and the URL of the respective WSDL. Respective sample Objective-C code is generated automatically and can be downloaded from each Adapters' status page. 


### The adapter

The adapter converts incoming JSON-RPC requests to SOAP requests and invokes the Web service. The SOAP responses from the Web service will be converted to JSON-RPC responses by the adapter and sent back to the client. The adapter generates JSON-RPC responses with the `error` object if an error occurs. It maps SOAP Faults to JSON-RPC responses with `error` objects. 

Furthermore, the adapter provides a workaround for SOAP Headers since many enterprise Web services often require a Header element in SOAP messages for authentication but JSON-RPC does not specify a similar element. For this purpose, the adapter expects a designated key `“SOAP-HEADER”` under `params` of the JSON-RPC request. The content of a SOAP Header should be stored there.  The remaining contents of `params` are the method parameters as usual. So, the clients should comply with this practice of the adapter and convey JSON-RPC requests of such structure. In case the Web service replies with a SOAP Header in the response, the adapter stores its contents under the `“SOAP-HEADER”` of the `result` in the JSON-RPC response. 

The adapter in its current state functions with SOAP 1.1, WSDL 1.1 and JSON-RPC 2.0 protocol versions. An adapter is specific per WSDL and therefore per Web service. After deployment no additional run-time configuration is needed. 


## Requirements

To successfully deploy the adapter servlet:
* SOAP <= 1.1
* WSDL <= 1.1 - Currently only XML Schema namespaces are well supported
* JSON-RPC 2.0
* Apache Tomcat 7.x or comparable Java Servlet Container
* Apache Maven

To use the generated Objective-C code stubs and/or stub XCode project:
* iOS 5.0 or greater
* XCode 4.4 or greater
* [Demiurgic JSON-RPC](https://github.com/dbowen/Demiurgic-JSON-RPC) as JSON-RPC Client - bundled with the generated stub XCode project


## Building

Build it by running 

	$mvn package
	
or simply download the [precompiled binaries](https://github.com/RWTH-i5-IDSG/webservice-to-jsonrpc/releases/download/v0.1/WS2JSON-v0.1.zip).


## Installation

1. Deploy the Adapter Manager *adapter-manager.war* using Tomcat's Manager App.
2. Deploy a new Adapter by entering the necessary parameters at */adapter-manager*, e.g.:
  ![Deployment Parameters](https://raw.github.com/RWTH-i5-IDSG/webservice-to-jsonrpc/master/website/deploy.png)
 
        NOTE: A Tomcat user with role "admin" is required to access the Adapter Manager!
 
3. A new entry should appear in the Adapter list:
	![Deployed](https://raw.github.com/RWTH-i5-IDSG/webservice-to-jsonrpc/master/website/deployed.png)
  
  Refer to the Adapter's documentation page at it's context path for further information and the generated code stubs.
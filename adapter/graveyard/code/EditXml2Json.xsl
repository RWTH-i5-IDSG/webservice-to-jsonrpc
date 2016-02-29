<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" omit-xml-declaration="yes" />

	<!--Set dummy variables. Later to be configured from Java -->
	<xsl:variable name="JsonRpcVersion" select="'2.0'" />
	<xsl:variable name="ResultNode" select="ConvertSpeedResult" />
	<xsl:variable name="JsonRpcId" select="40" />

	<!--Template for error-free JSON-RPC response. 
	Write the jsonrpc version, result and id -->
	<xsl:template match="/">
		<xsl:element name="root">
			<xsl:element name="jsonrpc">
				<xsl:value-of select="$JsonRpcVersion" />
			</xsl:element>
			<xsl:element name="result">
				<xsl:copy-of select="$ResultNode" />
				<xsl:apply-templates />
			</xsl:element>
			<xsl:element name="id">
				<xsl:value-of select="$JsonRpcId" />
			</xsl:element>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>
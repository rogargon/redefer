<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fn="http://www.w3.org/2004/10/xpath-functions" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:owl="http://www.w3.org/2002/07/owl#">
	<xsl:output media-type="text/xml" version="1.0" encoding="UTF-8" indent="yes" use-character-maps="owl"/>
	
	<xsl:include href="cs2owl.xsl"/>
	
	<!-- Do not define term ids as the concatenation of parent ids plus current term id -->
	<xsl:param name="concat" select="false()"/>
	
</xsl:stylesheet>
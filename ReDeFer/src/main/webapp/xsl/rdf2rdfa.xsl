<?xml version="1.0"?>
<!--
   RDF2RDFa is a joint project of Roberto García, Martin Hepp, and Andreas Radinger.
   It is based on the ReDeFer RDF2HTML tool, http://rhizomik.net/redefer/

   For a front-end to this tool, see
   http://www.ebusiness-unibw.org/tools/rdf2rdfa/

   For background information, please see our Technical Report at
   http://www.heppnetz.de/files/RDF2RDFa-TR.pdf.

   If you use the tool or script for academic purpose, please cite the technical report 
   or contact one of the authors for the most recent paper.

-->
<!--
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Lesser General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.

   For the full text of the GNU Lesser General Public License, see <http://www.gnu.org/licenses/>.
-->
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">

	<xsl:output media-type="text/xml" method="xml" encoding="UTF-8" indent="yes" omit-xml-declaration="yes"/> 
	<!--  doctype-public="-//W3C//DTD XHTML+RDFa 1.0//EN" doctype-system="http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd"/-->
	<xsl:strip-space elements="*"/>
	
	<xsl:template match="/">
		<div class="rdf2rdfa" xmlns="http://www.w3.org/1999/xhtml">
			<!-- Generate the xmlns for RDFa from those in the RDF/XML and attach to div -->
			<xsl:variable name="namespaces">
				<xsl:for-each select="/rdf:RDF/namespace::*[name()!='xml' and name()!='xsd']">
					<xsl:choose>
						<!-- The base NS in the output is XHTML so keep base NS in input RDF file with alias "base" -->
						<xsl:when test="name()=''">
							<xsl:element name="base:dummy-for-xmlns" namespace="{.}"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:element name="{concat(name(),':dummy-for-xmlns')}" namespace="{.}"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:for-each>
				<xsl:element name="xsd:dummy-for-xmlns" namespace="http://www.w3.org/2001/XMLSchema#"/>
			</xsl:variable>
			<xsl:copy-of select="$namespaces/*/namespace::*"/> <!-- [name()!='']"/ -->		
			
			<xsl:apply-templates select="rdf:RDF"/>
			<!-- Show error message if we have a parsererror -->
			<xsl:value-of select="//*[local-name()='sourcetext']"/>
		</div>
	</xsl:template>
	
	<xsl:template match="rdf:RDF">
			<!-- If no RDF descriptions... -->
			<xsl:if test="count(child::*)=0">
				<p xmlns="http://www.w3.org/1999/xhtml">No data available.</p>
			</xsl:if>
			<!-- If rdf:RDF has child elements, they are descriptions... -->
			<xsl:for-each select="child::*">
				<xsl:sort select="@rdf:about|@rdf:ID" order="ascending"/>
				<xsl:call-template name="rdfDescription"/>
			</xsl:for-each>
	</xsl:template>
  	
	<xsl:template name="rdfDescription">
		<div class="description" xmlns="http://www.w3.org/1999/xhtml">
			<xsl:if test="@rdf:ID">
				<xsl:attribute name="about" >
					<xsl:value-of select="concat('#',@rdf:ID)"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:if test="@rdf:about">
				<xsl:attribute name="about" >
					<xsl:value-of select="@rdf:about"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:call-template name="header"/>
			<xsl:call-template name="attributes"/>
			<xsl:call-template name="properties"/>
		</div>
	</xsl:template>
	
	<xsl:template name="embeddedRdfDescription">
		<div class="description" xmlns="http://www.w3.org/1999/xhtml">
			<xsl:if test="not(@rdf:parseType='Resource')">
				<xsl:call-template name="header"/>
			</xsl:if>
			<xsl:if test="@rdf:parseType='Resource'">
				<xsl:attribute name="typeof">
					<xsl:value-of select="'rdfs:Resource'"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:call-template name="attributes"/>
			<xsl:call-template name="properties"/>
		</div>
	</xsl:template>
	
	<!-- Process resource identifier and types, if available -->
	<xsl:template name="header">
		<xsl:choose>
			<xsl:when test="@rdf:ID|@rdf:about or not(local-name()='Description') or 
								count(*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type'])>0">
				<xsl:if test="@rdf:ID">
					<xsl:attribute name="about" >
						<xsl:value-of select="concat('#',@rdf:ID)"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:if test="@rdf:about">
					<xsl:attribute name="about" >
						<xsl:value-of select="@rdf:about"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:if test="not(local-name()='Description')">
					<xsl:attribute name="typeof">
						<xsl:call-template name="curie">
							<xsl:with-param name="uri" select="concat(namespace-uri(),local-name())"/>
						</xsl:call-template>
					</xsl:attribute>
				</xsl:if>
				<xsl:call-template name="types"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="typeof">
					<xsl:value-of select="'rdfs:Resource'"/>
				</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="types">
		<!-- rdf:type properties -->
		<xsl:for-each select="*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
			<xsl:choose>
				<xsl:when test="@rdf:resource">
					<div rel="rdf:type" resource="{@rdf:resource}" xmlns="http://www.w3.org/1999/xhtml"></div>
				</xsl:when>
				<xsl:when test="@rdf:parseType='Resource'">
					<div rel="rdf:type" class="connector" xmlns="http://www.w3.org/1999/xhtml">
						<xsl:call-template name="embeddedRdfDescription"/>
					</div>
				</xsl:when>
				<xsl:otherwise>
					<xsl:for-each select="child::*">
						<div rel="rdf:type" class="connector" xmlns="http://www.w3.org/1999/xhtml">
							<xsl:call-template name="embeddedRdfDescription"/>
						</div>
					</xsl:for-each>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="attributes">
		<xsl:for-each select="@*[not(namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#')]">
								<!-- and not(local-name='about' or local-name='ID' or local-name='type')]" -->
			<xsl:sort select="local-name()" order="ascending"/>
			<div property="{name()}" content="{.}" xmlns="http://www.w3.org/1999/xhtml"></div>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="property-objects">
		<xsl:param name="propertyCurie"/>
		<xsl:choose>
			<xsl:when test="@rdf:resource">
				<div rel="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
					<xsl:attribute name="resource">
						<xsl:value-of select="@rdf:resource"/>
					</xsl:attribute>
				</div>
			</xsl:when>
			<xsl:when test="@rdf:parseType='Resource'">
				<div rel="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
					<xsl:call-template name="embeddedRdfDescription"/>
				</div>
			</xsl:when>
			<xsl:when test="@rdf:parseType='Collection'">
				<div rel="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
					<xsl:for-each select="child::*[1]">
						<xsl:call-template name="buildList"/>
					</xsl:for-each>
				</div>
			</xsl:when>
			<xsl:when test="@rdf:parseType='Literal'">
				<div property="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
					<xsl:attribute name="datatype">
						<xsl:call-template name="curie">
							<xsl:with-param name="uri" select="'http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral'"/>
						</xsl:call-template>
					</xsl:attribute>
					<xsl:if test="@xml:lang">
						<xsl:attribute name="xml:lang">
							<xsl:value-of select="@xml:lang"/>
						</xsl:attribute>
					</xsl:if>
					<xsl:attribute name="style">display:none</xsl:attribute>
					<xsl:apply-templates mode="copy-subtree"/>
				</div>
			</xsl:when>
			<xsl:when test="child::*">
				<div rel="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
					<xsl:for-each select="child::*">
						<xsl:call-template name="embeddedRdfDescription"/>
					</xsl:for-each>
				</div>
			</xsl:when>
			<xsl:otherwise>
				<div property="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="buildList">
		<div typeof="rdf:List">
			<div rel="rdf:first">
				<xsl:call-template name="embeddedRdfDescription"/>
			</div>
        	<div rel="rdf:rest">
        		<xsl:choose>
        			<xsl:when test="count(following-sibling::*)>0">
        				<xsl:for-each select="following-sibling::*[1]">
        					<xsl:call-template name="buildList"/>
        				</xsl:for-each>
        			</xsl:when>
        			<xsl:otherwise>
        				<xsl:attribute name="resource"><xsl:value-of select="'[rdf:nil]'"/></xsl:attribute>
        			</xsl:otherwise>
        		</xsl:choose>
        	</div>
        </div>
	</xsl:template>
	
	<xsl:template name="properties">
		<xsl:for-each select="*[not(namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type')]">
			<xsl:sort select="local-name()" order="ascending"/>
			<xsl:variable name="propertyCurie">
				<xsl:call-template name="curie">
					<xsl:with-param name="uri" select="concat(namespace-uri(),local-name())"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="text() and count(descendant::*)=0 and not(@rdf:parseType)">
					<div property="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
						<xsl:attribute name="content">
							<xsl:value-of select="." disable-output-escaping="no"/>
						</xsl:attribute>
						<xsl:if test="@xml:lang">
							<xsl:attribute name="xml:lang">
								<xsl:value-of select="@xml:lang"/>
							</xsl:attribute>
						</xsl:if>
						<xsl:if test="@rdf:datatype">
							<xsl:attribute name="datatype">
								<xsl:call-template name="curie">
									<xsl:with-param name="uri" select="@rdf:datatype"/>
								</xsl:call-template>
							</xsl:attribute>
							<xsl:variable name="datatype-xmlns">
								<xsl:call-template name="xmlns">
									<xsl:with-param name="uri" select="@rdf:datatype"/>
								</xsl:call-template>
							</xsl:variable>
							<xsl:copy-of select="$datatype-xmlns/*/namespace::*"/>
						</xsl:if>
					</div>
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="property-objects">
						<xsl:with-param name="propertyCurie" select="$propertyCurie"/>
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>
	
	<!-- UTILITIES -->
	
	<xsl:template name="substring-after-last">
		<xsl:param name="text"/>
		<xsl:param name="chars"/>
		<xsl:choose>
		  <xsl:when test="contains($text, $chars)">
			<xsl:variable name="last" select="substring-after($text, $chars)"/>
			<xsl:choose>
			  <xsl:when test="contains($last, $chars)">
				<xsl:call-template name="substring-after-last">
				  <xsl:with-param name="text" select="$last"/>
				  <xsl:with-param name="chars" select="$chars"/>
				</xsl:call-template>
			  </xsl:when>
			  <xsl:otherwise>
				<xsl:value-of select="$last"/>
			  </xsl:otherwise>
			</xsl:choose>
		  </xsl:when>
		  <xsl:otherwise>
			<xsl:value-of select="$text"/>
		  </xsl:otherwise>
		</xsl:choose>
  	</xsl:template>
  
  	<xsl:template name="substring-before-last">
		<xsl:param name="text"/>
		<xsl:param name="chars"/>
		<xsl:choose>
		  <xsl:when test="contains($text, $chars)">
			<xsl:variable name="before" select="substring-before($text, $chars)"/>
			<xsl:variable name="after" select="substring-after($text, $chars)"/>
			<xsl:choose>
			  <xsl:when test="contains($after, $chars)">
			    <xsl:variable name="before-last">
					<xsl:call-template name="substring-before-last">
				  		<xsl:with-param name="text" select="$after"/>
				  		<xsl:with-param name="chars" select="$chars"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="concat($before,concat($chars,$before-last))"/>
			  </xsl:when>
			  <xsl:otherwise>
				<xsl:value-of select="$before"/>
			  </xsl:otherwise>
			</xsl:choose>
		  </xsl:when>
		  <xsl:otherwise>
			<xsl:value-of select="$text"/>
		  </xsl:otherwise>
		</xsl:choose>
  	</xsl:template>
  	
  	<xsl:template name="replace-string">
		<xsl:param name="text"/>
		<xsl:param name="replace"/>
		<xsl:param name="with"/>
		<xsl:choose>
			<xsl:when test="contains($text,$replace)">
				<xsl:value-of select="substring-before($text,$replace)"/>
				<xsl:value-of select="$with"/>
				<xsl:call-template name="replace-string">
					<xsl:with-param name="text" select="substring-after($text,$replace)"/>
					<xsl:with-param name="replace" select="$replace"/>
					<xsl:with-param name="with" select="$with"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$text"/>
			</xsl:otherwise>
		</xsl:choose>
  	</xsl:template>
   
    <xsl:template name="get-ns">
		<xsl:param name="uri"/>
		<xsl:choose>
	  		<xsl:when test="contains($uri,'#')">
				<xsl:value-of select="concat(substring-before($uri,'#'),'#')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="ns-without-slash">
					<xsl:call-template name="substring-before-last">
						<xsl:with-param name="text" select="$uri"/>
						<xsl:with-param name="chars" select="'/'"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="concat($ns-without-slash, '/')"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
    <xsl:template name="get-name">
		<xsl:param name="uri"/>
		<xsl:choose>
	  		<xsl:when test="contains($uri,'#')">
				<xsl:value-of select="substring-after($uri,'#')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="substring-after-last">
					<xsl:with-param name="text" select="$uri"/>
					<xsl:with-param name="chars" select="'/'"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="xmlns">
		<xsl:param name="uri"/>
		<xsl:variable name="namespace">
			<xsl:call-template name="get-ns">
				<xsl:with-param name="uri" select="$uri"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="namespace::*[.=$namespace]">
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="{concat(generate-id(),':dummy-for-xmlns')}" namespace="{$namespace}"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="curie">
		<xsl:param name="uri"/>
		<xsl:variable name="namespace">
			<xsl:call-template name="get-ns">
				<xsl:with-param name="uri" select="$uri"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="localname">
			<xsl:call-template name="get-name">
				<xsl:with-param name="uri" select="$uri"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="namespace::*[.=$namespace and name()!='']">
				<xsl:variable name="namespaceAlias">
					<xsl:value-of select="name(namespace::*[.=$namespace and name()!=''])"/>
				</xsl:variable>
				<xsl:value-of select="concat(concat($namespaceAlias,':'),$localname)"/>
			</xsl:when>
			<xsl:when test="$namespace='http://www.w3.org/2001/XMLSchema#'">
				<xsl:value-of select="concat('xsd:',$localname)"/>
			</xsl:when>
			<xsl:when test="namespace::*[name()='']">
				<xsl:value-of select="concat('base:',$localname)"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="concat(concat(generate-id(),':'),$localname)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="*|@*|text()" mode="copy-subtree">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|text()" mode="copy-subtree"/>
		</xsl:copy>
	</xsl:template>
	
</xsl:stylesheet>

<?xml version="1.0" encoding="UTF-8"?>
<!-- 
This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike License. 
To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/1.0/ 
or send a letter to Creative Commons, 559 Nathan Abbott Way, Stanford, California 94305, USA.
License: http://rhizomik.upf.edu/redefer/xsd2owl.xsl.rdf
-->
<xsl:stylesheet version="2.0" xmlns:xo="http://rhizomik.net/redefer/xsl/xsd2owl-functions.xsl" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">

	<!-- Generates URI reference from element namespace and local name using entity -->		
	<xsl:function name="xo:rdfUri" as="xsd:string">
		<xsl:param name="uriRef" as="xsd:string"/>
		<xsl:param name="namespaces"/>
		<xsl:choose>
			<xsl:when test="contains($uriRef,':')">
				<xsl:sequence select="
					concat('&amp;',substring-before($uriRef,':'),';',substring-after($uriRef,':'))"/>
			</xsl:when>
			<xsl:otherwise>
			<!-- When there isn't namespace declaration use dafault namespace if it exists or leave empty -->
				<xsl:choose>
					<xsl:when test="$namespaces[name()='']=''">
						<xsl:sequence select="
							concat('#',$uriRef)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:sequence select="
							if (contains($namespaces[name()=''],'#')) then
								concat($namespaces[name()=''],$uriRef)
							else
								concat($namespaces[name()=''],'#',$uriRef)"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>
	
	<!-- Generate absolute URI, i.e. without namespace alias -->			
	<xsl:function name="xo:absoluteUri" as="xsd:string">
		<xsl:param name="uriRef" as="xsd:string"/>
		<xsl:param name="namespaces"/>
		<xsl:choose>
			<xsl:when test="contains($uriRef,':')">
				<xsl:sequence select="
					if (contains($namespaces[name()=substring-before($uriRef,':')],'#')) then
						concat($namespaces[name()=substring-before($uriRef,':')],substring-after($uriRef,':'))
					else
						concat($namespaces[name()=substring-before($uriRef,':')],'#',substring-after($uriRef,':'))"/>
			</xsl:when>
			<xsl:otherwise>
			<!-- When there isn't namespace declaration use dafault namespace if it exists or leave empty -->
				<xsl:choose>
					<xsl:when test="$namespaces[name()='']=''">
						<xsl:sequence select="
							concat('#',$uriRef)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:sequence select="
							if (contains($namespaces[name()=''],'#')) then
								concat($namespaces[name()=''],$uriRef)
							else
								concat($namespaces[name()=''],'#',$uriRef)"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>
	
	<!-- Generate absolute URI for locally declared entities using targetNamespace -->			
	<xsl:function name="xo:localAbsoluteUri" as="xsd:string">
		<xsl:param name="uriRef" as="xsd:string"/>
		<xsl:param name="namespaces"/>
		<xsl:sequence select="
			if (contains($targetNamespace,'#')) then
				concat($targetNamespace ,$uriRef)
			else
				concat($targetNamespace ,'#',$uriRef)"/>
	</xsl:function>
	
	<!-- Test if the provided URI makes reference to the XMLSchema namespace -->
	<xsl:function name="xo:isXsdUri" as="xsd:boolean">
		<xsl:param name="uriRef" as="xsd:string"/>
		<xsl:param name="namespaces"/>
		<xsl:sequence select=" 
			contains(xo:absoluteUri($uriRef, $namespaces),'http://www.w3.org/2001/XMLSchema#')"/>
	</xsl:function>
	
	<!-- Determine if XSD element or attribute corresponds to a data type property:
		1.- its URI contains the XSD namespace or
		2.- there is a local simpleType named like the defined type or
		3.- the element defines a implicit simpleType -->
	<!-- TODO: what about simpleTypes defined in external XSD files -->
	<xsl:function name="xo:isDatatype" as="xsd:boolean">
		<xsl:param name="element"/>
		<xsl:param name="localSimpleTypes"/>
		<xsl:param name="namespaces"/>
		<!--xsl:message>
			<xsl:value-of select="$element/@name"/><xsl:value-of select="' -data- '"/>
			<xsl:value-of select="($element/@type and xo:isXsdUri($element/@type, $namespaces)) or 
			($element/@type and count($localSimpleTypes[xo:absoluteUri($element/@type, $namespaces)=xo:localAbsoluteUri(@name, $namespaces)])>0) or
			count($element/xsd:simpleType)>0"/>
		</xsl:message-->
		<xsl:sequence select="
			($element/@type and xo:isXsdUri($element/@type, $namespaces)) or 
			($element/@type and count($localSimpleTypes[xo:absoluteUri($element/@type, $namespaces)=xo:localAbsoluteUri(@name, $namespaces)])>0) or
			count($element/xsd:simpleType)>0"/>
	</xsl:function>
	<xsl:function name="xo:allDatatype" as="xsd:boolean">
		<xsl:param name="elements"/>
		<xsl:param name="localComplexTypes"/>
		<xsl:param name="namespaces"/>
		<!--xsl:message>
			<xsl:value-of select="$elements[1]/@name"/>	<xsl:value-of select="' -all datatype- '"/>
			<xsl:sequence select="sum(for $e in $elements return (xo:isObjectype($e, $localComplexTypes, $namespaces)) cast as xsd:integer) = 0"/>
		</xsl:message-->
		<xsl:sequence select="
			sum(for $e in $elements return (xo:isObjectype($e, $localComplexTypes, $namespaces)) cast as xsd:integer) = 0"/>
	</xsl:function>
	
	<!-- Determine if XSD element or attribute corresponds to a object type property:
		1.- there is a local complexType named like the defined type
		2.- the element defines an implicit complexType -->
	<xsl:function name="xo:isObjectype" as="xsd:boolean">
		<xsl:param name="element"/>
		<xsl:param name="localComplexTypes"/>
		<xsl:param name="namespaces"/>
		<!--xsl:message>
			<xsl:value-of select="$element/@name"/><xsl:value-of select="' -object- '"/>
			<xsl:value-of select="($element/@type and count($localComplexTypes[xo:absoluteUri($element/@type, $namespaces)=xo:localAbsoluteUri(@name, $namespaces)])>0) or
				count($element/xsd:complexType)>0"/>
		</xsl:message-->
		<xsl:sequence select="
			($element/@type and count($localComplexTypes[xo:absoluteUri($element/@type, $namespaces)=xo:localAbsoluteUri(@name, $namespaces)])>0) or
			count($element/xsd:complexType)>0"/>
	</xsl:function>
	<xsl:function name="xo:allObjectype" as="xsd:boolean">
		<xsl:param name="elements"/>
		<xsl:param name="localSimpleTypes"/>
		<xsl:param name="namespaces"/>
		<!--xsl:message>
			<xsl:value-of select="$elements[1]/@name"/>	<xsl:value-of select="' -all objectype- '"/>
			<xsl:sequence select="sum(for $e in $elements return (xo:isDatatype($e, $localSimpleTypes, $namespaces)) cast as xsd:integer) = 0"/>
		</xsl:message-->
		<xsl:sequence select="
			sum(for $e in $elements return (xo:isDatatype($e, $localSimpleTypes, $namespaces)) cast as xsd:integer) = 0"/>
	</xsl:function>
	
	<!-- For element and attributes with values defined using a type reference -->
	<!-- If value simpleType, map it to a OWL supported datatype -->
	<!-- If value complexType, generate range uri from type reference -->
	<!-- TODO: manage simpleTypes in a separate file an generate corresponding references here -->
	<xsl:function name="xo:rangeUri" as="xsd:string">
		<xsl:param name="element"/>
		<xsl:param name="localSimpleTypes"/>
		<xsl:param name="namespaces"/>
		<xsl:choose>
			<xsl:when test="xo:isDatatype($element, $localSimpleTypes, $namespaces)">
				<xsl:sequence select="xo:supportedDatatype($element/@type, $namespaces)"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:sequence select="xo:rdfUri($element/@type, $namespaces)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>

	<!-- For element and attributes with values defined completely inside -->
	<!-- If value simpleType, map it to an xsd:string because it is new and thus unsupported by OWL -->
	<!-- If value complexType, do nothing because the corresponding anonymous class is generated by 
		the complexType template -->
	<!-- TODO: manage simpleTypes in a separate file an generate corresponding references here -->
	<xsl:function name="xo:newRangeUri">
		<xsl:param name="element"/>
		<xsl:param name="baseEntity" as="xsd:string"/>
		<xsl:if test="count($element/xsd:simpleType)>0">
			<xsl:sequence select="'&amp;xsd;string'"/>
		</xsl:if>
	</xsl:function>
	
	<!-- If datatype in the XSD namespace use it directly or map it if not OWL supported -->
	<!-- For datatypes outside XSD, map them to xsd:string -->
	<xsl:function name="xo:supportedDatatype" as="xsd:string">
		<xsl:param name="datatype"/>
		<xsl:param name="namespaces"/>
		<xsl:choose>
			<xsl:when test="xo:isXsdUri($datatype, $namespaces)">
				<xsl:choose>
					<xsl:when test="contains($datatype,'ID')">
						<xsl:sequence select="'&amp;xsd;string'"/>
					</xsl:when>
					<xsl:when test="contains($datatype,'base64Binary')">
						<xsl:sequence select="'&amp;xsd;string'"/>
					</xsl:when>
					<xsl:when test="contains($datatype,'QName')">
						<xsl:sequence select="'&amp;xsd;string'"/>
					</xsl:when>
					<xsl:when test="contains($datatype,'hexBinary')">
						<xsl:sequence select="'&amp;xsd;string'"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:sequence select="xo:rdfUri($datatype, $namespaces)"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:sequence select="'&amp;xsd;string'"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>

	<!-- anyURI duration dateTime float -->

	<xsl:function name="xo:existsElemOrAtt" as="xsd:boolean">
		<xsl:param name="elements"/>
		<xsl:param name="name" as="xsd:string"/>
		<xsl:sequence select="count($elements/xsd:element[@name=$name] | $elements/xsd:attribute[@name=$name])>0"/>
	</xsl:function>
	<xsl:function name="xo:existsElem" as="xsd:boolean">
		<xsl:param name="elements"/>
		<xsl:param name="name" as="xsd:string"/>
		<xsl:sequence select="count($elements[@name=$name])>0"/>
	</xsl:function>
	
</xsl:stylesheet>

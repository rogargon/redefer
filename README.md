Project Description
===================

The ReDeFer project is a compendium of RDF-aware utilities organised in a set of packages:

* RDF2HTML: render a piece of RDF/XML as HTML.
* RDF2HTML+RDFa: render a piece of RDF/XML as HTML+RDFa.
* XSD2OWL: transform an XML Schema into an OWL Ontology.
* CS2OWL: transform a MPEG-7 Classification Scheme into an OWL Ontology.
* XML2RDF: transform a piece of XML into RDF.
* RDF2SVG: render a piece of RDF/XML as a SVG showing the corresponding graph. 
This project is available from [redefer-rdf2svg](https://github.com/rhizomik/redefer-rdf2svg)

Versions History

ReDeFer Lite
============

ReDeFer-Lite is a version of ReDeFer that just includes the RDF2HTML and RDF2SVG services.

ReDeFer Lite 0.6 (Jul 20th, 2010)
---------------------------------
Added version of the RDF2HTML transformation without embedding RDFa plus some bug fixes and improvements in both RDF2HTML and RDF2SVG.

ReDeFer Lite 0.5 (Feb 26th, 2010)
---------------------------------
This version adds a bunch of common prefixes to RDF2SVG in order to generate more compact and informative curies.

ReDeFer Lite 0.4
----------------
This version fixes some bugs and adds new functionality. For RDF2SVG:

Long string literals without spaces that facilitate pagination are now cut at a maximum line length.
For RDF2HTML:

In addition to HTML, RDFa is also generated. It has been tested using the RDF and RDFa test suites.
More compact output is generated because the input RDF/XML is reserialised as RDF/XML-ABBREV before the XSL transformation is applied.

ReDeFer Lite 0.3
----------------
This version fixes some bugs and problems with encoding. It also adds new functionality:

Long string literals are "paginated" to a fixed width in order to get a more compact graph
rdfs:labels are used when available in order to make the graph more user-friendly
It is possible to filter just the literals and labels for a given language (en, es, it,...) as defined with xml:lang. This is done using the "language" parameter (e.g. ...&language=es). If no language is specified, all available literals and labels are shown
URL links that allow browsing from the SVG

ReDeFer Lite 0.2
----------------
First public version of ReDeFer Lite.

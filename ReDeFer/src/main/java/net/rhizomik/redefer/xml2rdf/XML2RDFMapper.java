package net.rhizomik.redefer.xml2rdf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author: http://rhizomik.net/~roberto
 */
public class XML2RDFMapper
{
	//protected String baseNS;
	protected String language;
	protected Document doc;
	protected XSD2OWLMapper map;
	protected Model rdf;
	protected String docID;
	protected String baseID = "http://rhizomik.net/semanticxbrl/";
	protected boolean linkeddata = false;
	protected char urlPartDelimiter = '#';
	
	private static Property xbrliItem = 
		new PropertyImpl("http://rhizomik.net/ontologies/2007/11/xbrl-instance-2003-12-31.owl#item");

	public XML2RDFMapper(URL xmlURL, XSD2OWLMapper map, String ID, boolean linkeddata) 
	throws ParserConfigurationException, SAXException, IOException
	{
		this(xmlURL, map, ID);
		this.linkeddata = linkeddata;
		if (linkeddata)
			urlPartDelimiter = '/';
	}
    public XML2RDFMapper(URL xmlURL, XSD2OWLMapper map, String ID) 
    	throws ParserConfigurationException, SAXException, IOException
    {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setCoalescing(true);
		factory.setExpandEntityReferences(true);
		factory.setIgnoringComments(true);
		factory.setIgnoringElementContentWhitespace(true);
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		this.doc = builder.parse(xmlURL.openStream());
		this.doc.normalize();
		this.map = map;
		this.rdf = ModelFactory.createDefaultModel();
		
		if (ID == null)
			this.docID = xmlURL.toExternalForm();
		else
			this.docID = ID;
		
		//this.baseNS ="";
    }
    
    public XML2RDFMapper(URL xmlURL, XSD2OWLMapper map) 
	throws ParserConfigurationException, SAXException, IOException
	{
    	this(xmlURL, map, null);
    }
    
    public void getXMLNamespaces()
    {
    }
    
    public void mapXML2RDF() throws UnsupportedEncodingException
    {
		Resource r = rdf.createResource(docID);
		Element e = doc.getDocumentElement();
		String baseNS = e.getAttribute("xmlns");
		if (baseNS == null || baseNS.equals(""))
			baseNS = docID+urlPartDelimiter;

		String rdfp = map.map(e.getNamespaceURI(), e.getLocalName(), baseNS);
		OntClass range = map.getPropertyRange(null, rdfp);
		if (range != null && !range.isAnon())
			r.addProperty(RDF.type, range);
		
		mapResourceProperties(r, doc.getDocumentElement(), range, baseNS);
		Collection<OntologyInfo> aliases = map.getOntologiesInfo();
		for(Iterator it = aliases.iterator(); it.hasNext();)
		{
			OntologyInfo alias = (OntologyInfo)it.next();
			rdf.setNsPrefix(alias.getAlias(), alias.getNS());
		}
    }
    
	//	Serialise using RDF/XML
    public void getRDF(OutputStream o)
    {
    	rdf.write(o);
    }
    // Serialise specifying "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE" or "N3"
	public void getRDF(OutputStream o, String lang)
	{
		rdf.write(o, lang);
	}
	// Directly get the Jena Model
	public Model getModel()
	{
		return this.rdf;
	}
    
	protected void mapResourceProperties(Resource r, Element e, OntClass domain, String baseNS) throws UnsupportedEncodingException
    {
    	e.normalize();
    	String localBaseNS = baseNS;
    	if (!(e.getAttribute("xmlns")==null) && !e.getAttribute("xmlns").equals(""))
    		localBaseNS = e.getAttribute("xmlns");

		// Generate triples for element attributes
		NamedNodeMap atts = e.getAttributes();
    	for(int i=0; i<atts.getLength(); i++)
    	{
    		Attr att = (Attr)atts.item(i);
    		// Use "*id" attributes as resource ids, rename previously created r
    		if (att.getName().endsWith("id"))
    		{
    			//Check if it is a URI, if not scape and add the document ID unless it is a XBRL unit
    			String id = att.getValue();
    			try{ new URL(id); } 
    			catch(MalformedURLException e1)
                {
    				if (e.getLocalName().equals("unit"))
    					id = baseID+URLEncoder.encode(id,"UTF-8")+"/";
    				else
    					id = docID+urlPartDelimiter+URLEncoder.encode(id,"UTF-8")+(linkeddata?"/":"");
                }
    			r = ResourceUtils.renameResource(r, id);
    		    continue;
    		}
    		// Map "xsi:type" and "href" to "rdf:type"
    		if (att.getName().equals("xsi:type") || 
    		    att.getName().indexOf("href")>=0)
    		{
    		    Resource range = map.getOntologyClass(att.getValue(), baseNS);
    		    if (range!=null) r.addProperty(RDF.type, range);
    		    continue;
    		}
    		// Ignore attributes for namespace declarations and schema location
    		if (att.getName().indexOf("xmlns:")>=0 || att.getName().equals("xsi:schemaLocation"))
    			continue;
    		// Default namespace declaration already processed
    		if (att.getName().equals("xmlns"))
    			continue;
    		// Ignore xml:lang until processing a text node
    		if (att.getName().equals("xml:lang"))
    			continue;
    		
    		String ns = att.getNamespaceURI();
    		// If there is no namespace use default 
    		if (ns == null)
    			ns = baseNS;
    		Property p = rdf.createProperty(map.map(ns, att.getLocalName(), localBaseNS));
    		
			// Special Case for XBRL reference attributes (ContextRef, UnitRef...)
    		if (att.getName().equals("unitRef"))
			{
    			Resource rChild = rdf.createResource(baseID+URLEncoder.encode(att.getValue(),"UTF-8")+"/");
    			r.addProperty(p, rChild);
			}
    		else if (att.getName().endsWith("Ref"))
			{
				Resource rChild = rdf.createResource(docID+urlPartDelimiter+
										URLEncoder.encode(att.getValue(),"UTF-8")+(linkeddata?"/":""));
			    r.addProperty(p, rChild);
			}
			else
				r.addProperty(p, att.getValue());
    	}
		// Generate triples for child elements
		NodeList childs = e.getChildNodes();
		for(int i=0; i<childs.getLength(); i++)
		{
			Node child = childs.item(i);
			// Child element that only contains text
			if (child.getNodeType()==Node.ELEMENT_NODE && 
					 child.getChildNodes().getLength()==1 &&
					 child.getFirstChild().getNodeType()==Node.TEXT_NODE)
			{
				String rdfp = map.map(child.getNamespaceURI(), child.getLocalName(), localBaseNS);
				OntClass range = map.getPropertyRange(domain, rdfp);
				Property p = rdf.createProperty(rdfp);
				String text = child.getFirstChild().getNodeValue();
				
				// Special Case for XBRL "identifier"
				if (child.getLocalName().equals("identifier"))
				{
					String scheme = ((Attr)child.getAttributes().getNamedItem("scheme")).getValue();
					String id = baseID;
					if (scheme.indexOf("CIK")>0)
					{
						id += "CIK";
						text = text.replaceAll("[^\\d]", "");
						id += String.format("%1$010d", Integer.parseInt(text))+"/";
						r.addProperty(rdf.createProperty("http://dbpedia.org/property/secCik"), text);
					}
					else
						id += URLEncoder.encode(text, "UTF-8")+(linkeddata?"/":"");
					try{ new URL(id); } 
	    			catch(MalformedURLException e1)
	                {
		                id = docID+urlPartDelimiter+URLEncoder.encode(id,"UTF-8");
	                }
	    			r = ResourceUtils.renameResource(r, id);
				}
				// Special case for XBRL facts, check if the element has a "contextRef" attribute
				if (((Element)child).hasAttribute("contextRef"))
				{
					String contextID = ((Element)child).getAttribute("contextRef");
					String factID = docID+"/"+contextID+"/"+child.getLocalName()+"/";
					Resource rChild = rdf.createResource(factID);
					rChild.addProperty(RDF.type, rdf.createResource(rdfp));
					range = map.getPropertyRange(range, rdfp);
					if (range != null && !range.isAnon())
						rChild.addProperty(RDF.type, range);
					rChild.addProperty(RDF.value, text);
					r.addProperty(xbrliItem, rChild);
					mapResourceProperties(rChild, (Element)child, range, baseNS);
				}
				
				
				// If text with general attrib. different from xml:lang
				// use rdf:value construct, reproduce xml:lang otherwise
				else if (child.getAttributes().getLength()>0)
				{
				    Attr langAttr = (Attr)child.getAttributes().getNamedItem("xml:lang");
				    if (langAttr==null)
				    {
				        Resource rChild = rdf.createResource();
					    r.addProperty(p, rChild);
					    
					    range = map.getPropertyRange(domain, rdfp);
						if (range != null && !range.isAnon())
							rChild.addProperty(RDF.type, range);
					    
					    rChild.addProperty(RDF.value, text);
					    mapResourceProperties(rChild, (Element)child, range, localBaseNS);
				    }
				    else
				        r.addProperty(p, text, langAttr.getValue());
				}
				else
				    r.addProperty(p, text);
			}
			// Child elements that might contain more elements and attributes
			else if (child.getNodeType()==Node.ELEMENT_NODE && child.getLocalName()!="segment")
			{
				Resource rChild = rdf.createResource();
				String rdfp = map.map(child.getNamespaceURI(), child.getLocalName(), localBaseNS);
				Property p = rdf.createProperty(rdfp);
				r.addProperty(p, rChild);

				OntClass range = map.getPropertyRange(domain, rdfp);
				if (range != null && !range.isAnon())
					rChild.addProperty(RDF.type, range);
					
				mapResourceProperties(rChild, (Element)child, range, localBaseNS);
			}

		}
    }
}

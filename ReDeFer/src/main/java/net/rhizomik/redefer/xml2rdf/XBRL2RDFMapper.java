package net.rhizomik.redefer.xml2rdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.vocabulary.RDF;

public class XBRL2RDFMapper extends XML2RDFMapper 
{
	private static Property xbrliItem = 
		new PropertyImpl("http://rhizomik.net/ontologies/2007/11/xbrl-instance-2003-12-31.owl#item");

	public XBRL2RDFMapper(URL xmlURL, XSD2OWLMapper map, String ID)
			throws ParserConfigurationException, SAXException, IOException {
		super(xmlURL, map, ID, true);
	}
	
    public void mapXBRL2RDF() throws UnsupportedEncodingException
    {
		Resource r = rdf.createResource(docID+"/");
		Element e = doc.getDocumentElement();
		String baseNS = e.getAttribute("xmlns");
		if (baseNS == null || baseNS.equals(""))
			baseNS = docID+"/";

		String rdfp = map.map(e.getNamespaceURI(), e.getLocalName(), baseNS);
		OntClass range = map.getPropertyRange(null, rdfp);
		if (range != null && !range.isAnon())
			r.addProperty(RDF.type, range);
		
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
				String text = child.getFirstChild().getNodeValue();
				String rdfr = map.map(child.getNamespaceURI(), child.getLocalName(), baseNS);
				
				// If the element has a "contextRef" attribute it is a fact
				if (((Element)child).hasAttribute("contextRef"))
				{
					String contextID = ((Element)child).getAttribute("contextRef");
					String factID = docID+"/"+contextID+"/"+child.getLocalName()+"/";
					Resource rChild = rdf.createResource(factID);
					rChild.addProperty(RDF.type, rdf.createResource(rdfr));
					range = map.getPropertyRange(range, rdfr);
					if (range != null && !range.isAnon())
						rChild.addProperty(RDF.type, range);
					rChild.addProperty(RDF.value, text);
					r.addProperty(xbrliItem, rChild);
					mapResourceProperties(rChild, (Element)child, range, baseNS);
				}		
				else		
				{	
					Property p = rdf.createProperty(rdfp);
					r.addProperty(p, text);
				}
			}
			// Child elements that might contain more elements and attributes
			else if (child.getNodeType()==Node.ELEMENT_NODE)
			{
				Resource rChild = rdf.createResource();
				rdfp = map.map(child.getNamespaceURI(), child.getLocalName(), baseNS);
				Property p = rdf.createProperty(rdfp);
				r.addProperty(p, rChild);

				range = map.getPropertyRange(range, rdfp);
				if (range != null && !range.isAnon())
					rChild.addProperty(RDF.type, range);
					
				mapResourceProperties(rChild, (Element)child, range, baseNS);
			}
		}
		
		Collection<OntologyInfo> aliases = map.getOntologiesInfo();
		for(Iterator it = aliases.iterator(); it.hasNext();)
		{
			OntologyInfo alias = (OntologyInfo)it.next();
			rdf.setNsPrefix(alias.getAlias(), alias.getNS());
		}
    }
    
    public static void main(String[] args) throws Exception
	{
    	XSD2OWLMapper map = new XSD2OWLMapper("redefercfg.edgar.rdf");
    	
    	if (args.length != 1)
    		System.exit(-1);
    	
    	File edgarFiling = new File(args[0]);
    	
		System.out.println("Processing "+edgarFiling.getName());
		try 
		{ 
			String ID = edgarFiling.getName().substring(0, edgarFiling.getName().lastIndexOf('.'));
			XBRL2RDFMapper xbrl2rdf = 
				new XBRL2RDFMapper(edgarFiling.toURL(), map, "http://rhizomik.net/semanticxbrl/"+ID);
	        
			xbrl2rdf.mapXBRL2RDF();
			
			FileOutputStream output = new FileOutputStream(ID+".rdf");
    		xbrl2rdf.getModel().write(output, "RDF/XML-ABBREV");

	        System.out.println("Model size = "+xbrl2rdf.getModel().size()+" triples");
		}
		catch (Exception e)
		{ System.err.println(e); }
	}

}

package net.rhizomik.redefer.xml2rdf.app;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import net.rhizomik.redefer.xml2rdf.XML2RDFMapper;
import net.rhizomik.redefer.xml2rdf.XSD2OWLMapper;

import org.xml.sax.SAXException;


/**
 * @author: http://rhizomik.net/~roberto
 *
 */
public class XML2RDFApp 
{
    public static XSD2OWLMapper map = null;
    
    public static void main(String[] args) 
    		throws ParserConfigurationException, SAXException, IOException, URISyntaxException 
    {
        if (args.length < 1)
        {
            System.out.println("Usage: XML2RDFApp xmlfile [rdffile]");
            System.exit(1);
        }
        OutputStream out = null;
        if (args.length == 2)
            out = new FileOutputStream(new File(args[1]));
        else
            out = System.out;
        
        // Build local in-memory mapper
        map = new XSD2OWLMapper();
        File xmlFile = new File(args[0]);
		XML2RDFMapper xml2rdf = new XML2RDFMapper(xmlFile.toURI().toURL(), map);
        xml2rdf.mapXML2RDF();
        xml2rdf.getRDF(out, "RDF/XML-ABBREV");
    }
}
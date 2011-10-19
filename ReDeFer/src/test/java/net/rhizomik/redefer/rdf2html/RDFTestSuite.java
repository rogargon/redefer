package net.rhizomik.redefer.rdf2html;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RDFTestSuite 
{
	public static void main(String[] args) throws IOException 
	{
		String extractor = "http://www.w3.org/2007/08/pyRdfa/extract?uri=";
		String rdf2rdfa = "http://rhizomik.net/redefer-services/rdf2rdfa?rdf=";
		
		BufferedReader testUrls = new BufferedReader(new FileReader("RDFTest.urls"));
		
		String rdfUrl = null;
		while ((rdfUrl=testUrls.readLine())!=null)
		{
			String ntUrl = testUrls.readLine();
			
			Model generated = ModelFactory.createDefaultModel();
			Model reference = ModelFactory.createDefaultModel();
			
			generated.read(extractor+rdf2rdfa+rdfUrl);
			generated.removeAll(null, generated.createProperty("http://www.w3.org/1999/xhtml/vocab#stylesheet"), null);
			
			reference.read(ntUrl, "N-TRIPLE");
			
			if (generated.isIsomorphicWith(reference))
				System.out.println("EQUAL");
			else
			{
				System.out.println("NOT");
				System.out.println("Case: "+rdfUrl);
				generated.write(System.out, "N-TRIPLE");
				System.in.read();
			}
		}
	}
}

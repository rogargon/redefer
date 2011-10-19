package net.rhizomik.redefer.rdf2html;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class MicrodataTestSuite 
{
	public static void main(String[] args) throws IOException 
	{
		String extractor = "http://srv.buzzword.org.uk/microdata-to-turtle.cgi?url=";
		String rdf2micro = "http://rhizomik.net/redefer-services/rdf2microdata?rdf=";
		String rdf2microPars = "&mode=html";
		
		BufferedReader testUrls = new BufferedReader(new FileReader("RDFTest.urls"));
		
		String rdfUrl = null;
		while ((rdfUrl=testUrls.readLine())!=null)
		{
			String ntUrl = testUrls.readLine();
			
			Model generated = ModelFactory.createDefaultModel();
			Model reference = ModelFactory.createDefaultModel();
			
			generated.read(extractor+rdf2micro+rdfUrl+rdf2microPars, "TTL");
			generated.removeAll(null, generated.createProperty("http://www.w3.org/1999/xhtml/vocab#stylesheet"), null);
			generated.removeAll(null, generated.createProperty("http://www.w3.org/1999/xhtml/microdata#item"), null);

			
			reference.read(ntUrl, "N-TRIPLE");
			
			if (generated.isIsomorphicWith(reference))
				System.out.println("EQUAL");
			else
			{
				System.out.println("NOT");
				System.out.println("Case: "+rdfUrl);
				reference.write(System.out, "N-TRIPLE");
				System.out.println("Generated:");
				generated.write(System.out, "N-TRIPLE");
				System.in.read();
			}
		}
	}
}

package net.rhizomik.redefer.rdf2html;

import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class RDFaTestSuite 
{
	public static void main(String[] args) throws IOException 
	{
		int passed = 0;
		int missed = 0;
		String rdfaManifest = "http://www.w3.org/2006/07/SWD/RDFa/testsuite/xhtml1-testcases/rdfa-xhtml1-test-manifest.rdf";
		String extractor = "http://www.w3.org/2007/08/pyRdfa/extract?uri=";
		String rdf2rdfa = "http://rhizomik.net/redefer-services/rdf2rdfa?rdf=";
		
		Model manifest = ModelFactory.createDefaultModel();
		manifest.read(rdfaManifest);
		Property pStatus = manifest.createProperty("http://www.w3.org/2006/03/test-description#reviewStatus");
		Resource rApproved = manifest.createResource("http://www.w3.org/2006/03/test-description#approved");
		Property pInput = manifest.createProperty("http://www.w3.org/2006/03/test-description#informationResourceInput");
		for (ResIterator it = manifest.listSubjectsWithProperty(pStatus, rApproved); it.hasNext();) 
		{
			Resource test = it.nextResource();
			String xhtmlUrl = test.getProperty(pInput).getObject().toString();			
			
			Model generated = ModelFactory.createDefaultModel();
			Model reference = ModelFactory.createDefaultModel();
			
			reference.read(extractor+xhtmlUrl);

			generated.read(extractor+rdf2rdfa+extractor+xhtmlUrl);
			generated.removeAll(null, generated.createProperty("http://www.w3.org/1999/xhtml/vocab#stylesheet"), null);
			generated.removeAll(null, RDF.type, RDFS.Resource);
						
			if (generated.isIsomorphicWith(reference))
			{
				System.out.println("EQUAL");
				passed++;
			}
			else
			{
				System.out.println("NOT");
				System.out.println("Case: "+xhtmlUrl);
				System.out.println("Reference:");
				System.out.println("--------------------------------------");
				reference.write(System.out, "N-TRIPLE");
				System.out.println("Generated:");
				System.out.println("--------------------------------------");
				generated.write(System.out, "N-TRIPLE");
				missed++;
				//System.in.read();
			}
		}
		System.out.println("\nPassed "+passed+" test cases.");
		System.out.println("\nMissed "+missed+" test cases.");
	}
}

package net.rhizomik.redefer.xml2rdf.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import net.rhizomik.redefer.xml2rdf.XBRL2RDFMapper;
import net.rhizomik.redefer.xml2rdf.XML2RDFMapper;
import net.rhizomik.redefer.xml2rdf.XSD2OWLMapper;
import virtuoso.jena.driver.VirtGraph;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.util.NodeFactory;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;

public class CNMV2RDF    
{
    private static final Logger log = Logger.getLogger(CNMV2RDF.class.getName());

	public static String void_ns = "http://rdfs.org/ns/void#";
	public static Property statItem = ResourceFactory.createProperty(void_ns+"statItem");
	public static Property subset = ResourceFactory.createProperty(void_ns+"subset");
	public static Resource numOfTriples = ResourceFactory.createResource(void_ns+"numOfTriples");
	public static Resource Dataset = ResourceFactory.createResource(void_ns+"Dataset");
	
	public static String scovo_ns = "http://purl.org/NET/scovo#";
	public static Property dimension = ResourceFactory.createProperty(scovo_ns+"dimension");
	
    public static String DATA_URI = "http://rhizomik.net/semanticxbrl/cnmv/";
    public static String FEED_URI = "http://rhizomik.net/semanticxbrl/cnmv/feed/";
    public static String BACKUP_DIR = "cnmv";
    
    public String DB_URL = "jdbc:virtuoso://omediadis.udl.cat:1111";
    public String DB_USER = "user";
    public String DB_PASSWD = "password";
    
    public int TotalFilings = 0;
	public XSD2OWLMapper map = null;
    private Model feedModel = null;
    private VirtGraph graph = null;
    //private VirtGraph feedGraph = null;
    
    public CNMV2RDF() throws MalformedURLException, IOException 
    {
    	map = new XSD2OWLMapper("redefercfg.cnmv.rdf");
    }
    
    public CNMV2RDF(Properties config) throws Exception 
    {
    	map = new XSD2OWLMapper("redefercfg.cnmv.rdf");
    	
    	if (!config.containsKey("db_url"))
    		throw new Exception("Missing parameter for Virtuoso: db_url");
    	else if (!config.containsKey("db_user"))
    		throw new Exception("Missing parameter for Virtuoso: db_user");
    	else if (!config.containsKey("db_pass"))
    		throw new Exception("Missing parameter for Virtuoso: db_pass");
    	else if (!config.containsKey("db_graph"))
    		throw new Exception("Missing parameter for Virtuoso: db_graph");
    	else
    	{
    		DB_URL = config.getProperty("db_url");
    		DB_USER = config.getProperty("db_user");
    		DB_PASSWD = config.getProperty("db_pass");
    		DATA_URI = config.getProperty("db_graph");
    		//FEED_URI = DATA_URI+(DATA_URI.endsWith("/")?"":"/")+"feed/";
    		
    		String ruleSet = DATA_URI+(DATA_URI.endsWith("/")?"":"/")+"schemas/rules/";
    		graph = new VirtGraph (DATA_URI, DB_URL, DB_USER, DB_PASSWD);
    		//Add or recalculate inference for the graph
    		String sqlStatement = "rdfs_rule_set('"+ruleSet+"', '"+DATA_URI+"')";
    		graph.getConnection().prepareCall(sqlStatement).execute();
    		
    		//Build a feed of dataset updates
    		//feedGraph = new VirtGraph (FEED_URI, DB_URL, DB_USER, DB_PASSWD);
    		feedModel = ModelFactory.createDefaultModel();
    	}
	}

	private XBRL2RDFMapper cnmvFile2RDF(File cnmvFiling) 
		throws MalformedURLException, ParserConfigurationException, SAXException, IOException 
	{
		log.log(Level.INFO, "Processing "+cnmvFiling.getName());
 
		String ID = cnmvFiling.getName().substring(0, cnmvFiling.getName().lastIndexOf('.'));
		String URI = DATA_URI+(DATA_URI.endsWith("/")?"":"/")+ID;
		XBRL2RDFMapper xbrl2rdf = 
			new XBRL2RDFMapper(cnmvFiling.toURI().toURL(), map, URI);
        
		xbrl2rdf.mapXBRL2RDF();	

		return xbrl2rdf;
	}
	
	private void storeRDFFiling(String filingURI, XBRL2RDFMapper xbrl2rdf)
	{
		// Before loading the triples, check the report hasn't been loaded already
		Node r = Node.createURI(filingURI+"/");
		Node p = RDF.type.asNode();
		Node o = Node.createURI("http://rhizomik.net/ontologies/2007/11/xbrl-instance-2003-12-31.owl#xbrlType");					
		if (!graph.contains(r,p,o))
		{
			graph.getTransactionHandler().begin();
			graph.getBulkUpdateHandler().add(xbrl2rdf.getModel().getGraph());
    		graph.getTransactionHandler().commit();
        
	        TotalFilings++;
	        System.out.println("Added "+xbrl2rdf.getModel().size()+" triples");
	        
	        updateFeed(xbrl2rdf.getModel(), filingURI, xbrl2rdf.getModel().size());
		}
		else
			System.out.println("Ignored (already loaded): "+filingURI);
	}
	
	private void updateFeed(Model filing, String URI, long size)
	{
		Resource dataSet = feedModel.createResource(URI+"/");
        Resource numOfTriplesMeasure = feedModel.createResource();
        dataSet.addProperty(subset, feedModel.createResource(DATA_URI));
        dataSet.addProperty(statItem, numOfTriplesMeasure);
        numOfTriplesMeasure.addProperty(dimension, numOfTriples);
        numOfTriplesMeasure.addLiteral(RDF.value, size);
		NodeIterator it = filing.listObjectsOfProperty(
				filing.getProperty("http://rhizomik.net/ontologies/2007/11/xbrl-instance-2003-12-31.owl#entity"));
		if (it.hasNext())
		{
			dataSet.addProperty(DC.source, it.nextNode());
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		CNMV2RDF transform = new CNMV2RDF();
		
		String filingFileName = null;
		
		if (args.length > 0)
			filingFileName = args[0];        
		else
		{
			System.out.println("Usage: CNMV2RDF filingFileName");
			System.exit(1);
		}
		
		File f = new File(filingFileName);
		XBRL2RDFMapper xbrl2rdf = transform.cnmvFile2RDF(f);
		
		File fOut = new File(f.getPath().substring(0, f.getPath().lastIndexOf('.'))+".rdf");
		xbrl2rdf.getModel().write(new FileOutputStream(fOut), "RDF/XML-ABBREV");

        log.log(Level.INFO, xbrl2rdf.getModel().size()+" triples stored in "+fOut.getName());

		System.exit(0);
	}
}
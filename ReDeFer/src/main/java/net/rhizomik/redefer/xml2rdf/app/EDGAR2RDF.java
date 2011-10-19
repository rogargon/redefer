package net.rhizomik.redefer.xml2rdf.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Comparator;

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

public class EDGAR2RDF    
{
	public static String void_ns = "http://rdfs.org/ns/void#";
	public static Property statItem = ResourceFactory.createProperty(void_ns+"statItem");
	public static Property subset = ResourceFactory.createProperty(void_ns+"subset");
	public static Resource numOfTriples = ResourceFactory.createResource(void_ns+"numOfTriples");
	public static Resource Dataset = ResourceFactory.createResource(void_ns+"Dataset");
	
	public static String scovo_ns = "http://purl.org/NET/scovo#";
	public static Property dimension = ResourceFactory.createProperty(scovo_ns+"dimension");
	
    public static String DATA_URI = "http://rhizomik.net/semanticxbrl/";
    public static String FEED_URI = "http://rhizomik.net/semanticxbrl/feed/";
    public static String BACKUP_DIR = "edgar";
    
    public String DB_URL = "jdbc:virtuoso://omediadis.udl.cat:1111";
    public String DB_USER = "user";
    public String DB_PASSWD = "password";
    
    public int TotalFilings = 0;
	public XSD2OWLMapper map = null;
    private Model feedModel = null;
    private VirtGraph graph = null;
    //private VirtGraph feedGraph = null;
    
    public EDGAR2RDF(Properties config) throws Exception 
    {
    	map = new XSD2OWLMapper("redefercfg.edgar.rdf");
    	
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

	private void edgarFile2RDF(File edgarFiling) 
	{
		System.out.println("Processing "+edgarFiling.getName());
		try 
		{ 
			String ID = edgarFiling.getName().substring(0, edgarFiling.getName().lastIndexOf('.'));
			String URI = DATA_URI+(DATA_URI.endsWith("/")?"":"/")+ID;
			
			// Before processing it, check the report hasn't been loaded already
			Node r = Node.createURI(URI+"/");
			Node p = RDF.type.asNode();
			Node o = Node.createURI("http://rhizomik.net/ontologies/2007/11/xbrl-instance-2003-12-31.owl#xbrlType");					
			if (!graph.contains(r,p,o))
			{
				XBRL2RDFMapper xbrl2rdf = 
					new XBRL2RDFMapper(edgarFiling.toURI().toURL(), map, URI);
	        
				xbrl2rdf.mapXBRL2RDF();
			
				graph.getTransactionHandler().begin();
				graph.getBulkUpdateHandler().add(xbrl2rdf.getModel().getGraph());
	    		graph.getTransactionHandler().commit();
	        
		        TotalFilings++;
		        System.out.println("Added "+xbrl2rdf.getModel().size()+" triples");
		        
		        updateFeed(xbrl2rdf.getModel(), URI, xbrl2rdf.getModel().size());
			}
			else
				System.out.println("Ignored (already loaded): "+ID);
		}
		catch (Exception e)
		{ System.err.println(e); }
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
		// Get filings backup dir from config file
		InputStream propsStream = 
            ClassLoader.getSystemResourceAsStream("redefer_edgar.cfg");
        if (propsStream == null)
            throw new IOException("Configuration resource edgar_dir.cfg not found");
  		Properties properties = new Properties();
        properties.load(propsStream);
        if (!properties.containsKey("backup_dir"))
            throw (new IOException("Property backup_dir not defined in redefer_edgar.cfg"));
        BACKUP_DIR = properties.getProperty("backup_dir");
        
		EDGAR2RDF transform = new EDGAR2RDF(properties);
		
		Date start = new Date(0);
		Date end = new Date(System.currentTimeMillis());
		String filingRegExp = ".*"; // Default pattern for filings to be processed, i.e. all
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		if (args.length > 0)
			filingRegExp = args[0];
		if (args.length > 1)
            start = df.parse(args[1]);
		else if (properties.containsKey("last_rdfication"))
			start = df.parse(properties.getProperty("last_rdfication"));
		if (args.length > 2)
			end = df.parse(args[2]);        
        
		// Process filings from the backup dir matching the regexp filter
		System.out.println("Processing filings from "+BACKUP_DIR);
		System.out.println("since "+start);
        File backupDir = new File(BACKUP_DIR);
        
        File[] files = backupDir.listFiles(new RegExpFilenameFilter(filingRegExp));
        // Sort files so newer are first...
        Arrays.sort(files, new Comparator <File>(){
            public int compare(File f1, File f2)
            {
                return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
            } });

        for (File f: files) 
        {
        	if (f.lastModified() > start.getTime() && f.lastModified() < end.getTime())
        		transform.edgarFile2RDF(f);
		}
        
        // Write last rdfication time to the config file
        URI cfgURI = ClassLoader.getSystemResource("redefer_edgar.cfg").toURI();
        FileOutputStream cfgOut = new FileOutputStream(new File(cfgURI));
        properties.setProperty("last_rdfication", df.format(end));
        properties.store(cfgOut, "");
        
		transform.graph.getTransactionHandler().begin();
		transform.graph.getBulkUpdateHandler().add(transform.feedModel.getGraph());
		transform.graph.getTransactionHandler().commit();
        
		System.out.println("Filings processed: "+transform.TotalFilings);
        System.out.println("Model size = "+transform.graph.getCount()+" triples");

		System.exit(0);
	}
}
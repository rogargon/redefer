package net.rhizomik.redefer.xml2rdf.app;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import net.rhizomik.redefer.xml2rdf.XSD2OWLMapper;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Namespace;
import org.xml.sax.SAXException;

import virtuoso.jena.driver.VirtGraph;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.datatypes.xsd.impl.XSDDateTimeType;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class DownloadEDGARFilings 
{
	public static int TotalFilings = 0;
    public static Namespace EDGAR_NS = Namespace.getNamespace("http://www.sec.gov/Archives/edgar");
    public static String edgarRss = "http://www.sec.gov/Archives/edgar/xbrlrss.xml";
    public static String DATA_URI = "http://rhizomik.net/semanticxbrl/";
    public static String FEED_URI = "http://rhizomik.net/semanticxbrl/feed/";

    public static String BACKUP_DIR = "edgar";
    
    public String DB_URL = "jdbc:virtuoso://omediadis.udl.cat:1111";
    public String DB_USER = "user";
    public String DB_PASSWD = "password";
    
    public static Model model = null;
    private Model feedModel = null;
    private VirtGraph graph = null;
    
    public DownloadEDGARFilings(Properties config) throws Exception 
    {    	
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
    		
    		//Build a feed of dataset updates
    		graph = new VirtGraph (DATA_URI, DB_URL, DB_USER, DB_PASSWD);
    		feedModel = ModelFactory.createDefaultModel();
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
        DownloadEDGARFilings download = new DownloadEDGARFilings(properties);
        if (!properties.containsKey("backup_dir"))
            throw (new IOException("Property backup_dir not defined in redefer_edgar.cfg"));
        BACKUP_DIR = properties.getProperty("backup_dir");
        if (!properties.containsKey("edgar_rss"))
            throw (new IOException("Property edgar_rss not defined in redefer_edgar.cfg"));
        edgarRss = properties.getProperty("edgar_rss");
        
		Date start = new Date(System.currentTimeMillis());
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		if (args.length > 0)
			start = df.parse(args[0]);
		else if (properties.containsKey("last_download"))
			start = df.parse(properties.getProperty("last_download"));
		else
		{
			// If no data provided, the default start date is that of the last filing downloaded
			File backupDir = new File(BACKUP_DIR);
			FilenameFilter filter = new RegExpFilenameFilter(".*\\.xml");
			File[] xmlFiles = backupDir.listFiles(filter);
			if (xmlFiles != null)
			{
		        for (File f : xmlFiles)
		        {
		        	Date last = new Date(f.lastModified());
		        	if (last.after(start))
		        		start = last;
				}
			}
		}
		
		Date end = new Date(System.currentTimeMillis());
		if (args.length > 1)
			end = DateFormat.getInstance().parse(args[1]);
		
		System.out.println("Download filings from "+start+" to "+end);
		
		URL feedUrl = new URL(edgarRss);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(feedUrl));
        
        List<SyndEntry> entries = feed.getEntries();
        Date entryDate = null;
        Date lastEntry = new Date(0);
        int i = 0;
        for (Iterator<SyndEntry> ite = entries.iterator(); ite.hasNext();)
        {
        	SyndEntry entry = ite.next();
        	entryDate = entry.getPublishedDate();
        	if (entryDate.after(lastEntry))
    			lastEntry = entryDate;
        	
        	if (entryDate.before(start) || entryDate.equals(start) || entryDate.after(end))
        		continue;
        	System.out.println("Date: "+entryDate);
        	String title = entry.getTitle();
        	System.out.println("Filing: "+title);
        	
        	ArrayList<Element> l = (ArrayList<Element>)entry.getForeignMarkup();
        	Element e = l.get(0);
        	Element cikElem = e.getChild("cikNumber", EDGAR_NS);
        	String cik = cikElem!=null? cikElem.getText():"";
        	Element f = e.getChild("xbrlFiles", EDGAR_NS);
        	Namespace edgarNS = Namespace.getNamespace("http://www.sec.gov/Archives/edgar");
        	
        	List<Element> files = f.getChildren("xbrlFile", EDGAR_NS);
        	for (Iterator<Element> itf = files.iterator(); itf.hasNext();) 
            {
        		Element file = itf.next();
        		Attribute typeAtt = file.getAttribute("type", edgarNS);
            	Attribute urlAtt = file.getAttribute("url", edgarNS);
        		if (typeAtt!=null && typeAtt.getValue().contains(".INS") && urlAtt!=null)
        			download.EDGARInstance2File(cik, urlAtt.getValue(), entryDate);
            }
		}
        // Write most recent entry time to the config file
        URI cfgURI = ClassLoader.getSystemResource("redefer_edgar.cfg").toURI();
        FileOutputStream cfgOut = new FileOutputStream(new File(cfgURI));
        properties.setProperty("last_download", df.format(lastEntry));
        properties.store(cfgOut, "");
        
        download.graph.getBulkUpdateHandler().add(download.feedModel.getGraph());
        
		System.out.println("Filings processed: "+TotalFilings);

		System.exit(0);
	}

	private void updateFeed(String URI, Date entryDate)
	{
		Resource dataSet = feedModel.createResource(URI);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dataSet.addProperty(DC.date, df.format(entryDate), XSDDatatype.XSDdateTime);
	}
	
	private void EDGARInstance2File(String cik, String instanceURLString, Date entryDate) 
	{
		System.out.println(instanceURLString);
		try 
		{ 
			URL instanceURL = new URL(instanceURLString);
			String ID = instanceURL.getFile().substring(instanceURL.getFile().lastIndexOf('/')+1, instanceURL.getFile().lastIndexOf('.'));
			if (cik!="") cik+= "_";
			
			File f = new File(BACKUP_DIR+"/"+cik+ID+".xml");
			BufferedOutputStream o = new BufferedOutputStream(new FileOutputStream(f), 8192);
			byte[] b = new byte[8192];
			BufferedInputStream i = new BufferedInputStream(instanceURL.openStream(), 8192);
			int l;
			while((l=i.read(b))>0)
			{
				o.write(b, 0, l);
			}
			o.close();
			System.out.println("Written to: "+f.getAbsolutePath());
			String URI = DATA_URI+(DATA_URI.endsWith("/")?"":"/")+cik+ID+"/";
			updateFeed(URI, entryDate);
			TotalFilings++;
		}
		catch (Exception e)
		{ System.err.println(e); }		
	}
}
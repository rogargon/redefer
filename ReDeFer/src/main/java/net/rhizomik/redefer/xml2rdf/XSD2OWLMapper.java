package net.rhizomik.redefer.xml2rdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.ontology.BooleanClassDescription;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author http://rhizomik.net/~roberto
 */
public class XSD2OWLMapper
{
	private HashMap mappings = null;
	private HashMap aliases = null;
	private int aliasID;
	private OntModel ontologies = null;
	private IDBConnection conn = null;
	private String rdfrns = "http://rhizomik.net/redefer#";
	private Property xmlnsProp = new PropertyImpl("http://rhizomik.net/redefer#xmlns");
	private Property aliasProp = new PropertyImpl("http://rhizomik.net/redefer#alias");
	private Property rdfnsProp = new PropertyImpl("http://rhizomik.net/redefer#rdfns");
	private Property rdfsrcProp = new PropertyImpl("http://rhizomik.net/redefer#rdfsrc");
	private Resource MappingRes = new PropertyImpl("http://rhizomik.net/redefer#Mapping");

	public XSD2OWLMapper(String bd, String user, String passwd, String className, String type) 
		throws ClassNotFoundException, MalformedURLException, IOException, SQLException
	{
	    Class.forName(className);
	    this.conn = new DBConnection(bd, user, passwd, type);
	    ModelMaker maker = ModelFactory.createModelRDBMaker(conn);
	    OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);
	    spec.setBaseModelMaker(maker);
	    spec.getDocumentManager().setProcessImports(false);
	    Model base = maker.createFreshModel();
	    this.ontologies = ModelFactory.createOntologyModel(spec, base);
		this.mappings = new HashMap();
		this.aliases = new HashMap();
		this.aliasID = 1;
		buildMappingsHash();
	}
	
	public XSD2OWLMapper(String redefercfg) throws MalformedURLException, IOException
	{
	    OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);
	    spec.getDocumentManager().setProcessImports(false);
	    this.ontologies = ModelFactory.createOntologyModel(spec, null);
	    this.mappings = new HashMap();
	    this.aliases = new HashMap();
		this.aliasID = 1;
		loadMappings(redefercfg);
	}
	
	public XSD2OWLMapper() throws MalformedURLException, IOException
	{
		this("redefercfg.local.rdf");
	}
	
	public void initialise() 
		throws MalformedURLException, IOException, SQLException
	{
	    conn.cleanDB();
	    ModelMaker maker = ModelFactory.createModelRDBMaker(conn);
	    OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);
	    spec.setBaseModelMaker(maker);
	    spec.getDocumentManager().setProcessImports(false);
	    Model base = maker.createFreshModel();
	    this.ontologies = ModelFactory.createOntologyModel(spec, base);
	    loadMappings("redefercfg.rdf");
	}
	
	public void addMapping(String xmlns, String rdfns) 
		throws MalformedURLException, IOException
	{
		this.addMapping(xmlns, "ns"+(aliasID++), rdfns, rdfns);
	}
    public void addMapping(String xmlns, String alias, String rdfns) 
    	throws MalformedURLException, IOException
    {
        this.addMapping(xmlns, alias, rdfns, rdfns);
    }
	public void addMapping(String xmlns, String alias, String rdfns, String url) 
		throws MalformedURLException, IOException
	{
		mappings.put(xmlns, new OntologyInfo(alias, rdfns, url));
		aliases.put(alias, xmlns);
	}

    public String map(String xmlns, String localname, String base)
    {
        String ns = null;
        if (xmlns!=null)
            ns = xmlns;
        else
        	ns = base;

        String mappedNS = null;
        if (mappings.containsKey(ns))
        	mappedNS = ((OntologyInfo)mappings.get(ns)).getNS();
        else
            mappedNS = ns;
        
        mappedNS = mappedNS.replace('\\','/');
        if(!mappedNS.endsWith("#") && !mappedNS.endsWith("/") )
            mappedNS+="#";
        if(!mappedNS.startsWith("http://") && !mappedNS.startsWith("urn:"))
        	mappedNS = "http://"+mappedNS;
        
        // First avoid bad localnames with '#'
        String safeLocalname = localname.replace("#","");
        // Generate the mapped URI by concatenating mapped NS and safe localname
        String mappedURI = mappedNS + safeLocalname;
        
        try { new URI(mappedURI); }
        catch (URISyntaxException e)
        {
        	mappedURI = rdfrns + safeLocalname;
        	System.out.println("Error in mapped URI: "+mappedURI+"\n"+e);
        }
        
        return mappedURI;
    }
    
    public String getNSFromAlias(String alias)
    {
    	String ns = alias;
        if (aliases.containsKey(alias))
        	ns = (String)aliases.get(alias);
        return ns;
    }
    
    public Resource getOntologyClass(String uri, String baseNS) 
    {
        int i;
        String ns;
        if ((i=uri.indexOf(':'))>0)
            ns = uri.substring(0, i);
        else
            ns = baseNS;
        String id = uri.substring(i+1, uri.length());
        if (ns.indexOf(':')<0) //if it is not a URI
            ns = getNSFromAlias(ns);
        return ontologies.getResource(map(ns, id, baseNS)); //getOntClass
    }
    
    public OntClass getPropertyRange(OntClass domain, String property)
    { 	
    	// Get range restriction for property
    	OntProperty p = ontologies.getOntProperty(property);
		OntResource range = null;
    	if (p!=null && p.getRange()!=null)
    		range = p.getRange();
    	// Get property value restriction for domain class if known
		Resource value = null;
    	if (domain != null && p!=null)
			value = getValueRestriction(domain, p);
		// If present take the stronger restriction, the value restriction
		OntClass r = null;
 		if (value != null && value.canAs(OntClass.class))
    		r = (OntClass)value.as(OntClass.class);
    	else if (range != null && range.canAs(OntClass.class))
    		r = range.asClass();
    	return r;
    }
    
    private Resource getValueRestriction(OntClass c, OntProperty p)
    {
    	Resource value = null;
		if (c.isRestriction() && c.asRestriction().onProperty(p)) 
		{
		  Restriction r = c.asRestriction();
		  if (r.isAllValuesFromRestriction())
			  value = r.asAllValuesFromRestriction().getAllValuesFrom();
		  else if (r.isSomeValuesFromRestriction())
			  value = r.asSomeValuesFromRestriction().getSomeValuesFrom();
		}
		for (Iterator i=c.listSuperClasses(false);i.hasNext()&&value==null;)
		{
			OntClass cs = (OntClass)i.next();
			value = getValueRestriction(cs, p);
		}
		for (Iterator i=c.listEquivalentClasses();i.hasNext()&&value==null;)
		{
			OntClass cs = (OntClass)i.next();
			value = getValueRestriction(cs, p);
		}
		if (c.isUnionClass() || c.isIntersectionClass())
		{
			BooleanClassDescription cb = null;
			if (c.isUnionClass()) cb=c.asUnionClass();
			else cb=c.asIntersectionClass();
			for(Iterator it=cb.listOperands();it.hasNext()&&value==null;)
			{
				OntClass co = (OntClass)it.next();
				value = getValueRestriction(co, p);
			}
		}
        return value;
    }

    public Collection getOntologiesInfo()
    {
    	return mappings.values();
    }

    /**
     * @param string
     * @throws IOException
     * @throws MalformedURLException
     */
    public void loadMappings(String redefercfg) throws MalformedURLException, IOException
    {
        InputStream cfgStream = getClass().getClassLoader().getResourceAsStream(redefercfg);
        if (cfgStream == null)
            System.out.println("ReDeFer configuration resource not found, "+redefercfg);
        else
        {   
            ontologies.read(cfgStream, "");
            buildMappingsHash();
            
            // First, define local locations for ontologies if needed
            OntDocumentManager mgr = ontologies.getDocumentManager();
            for(Iterator it=mappings.keySet().iterator(); it.hasNext();)
            {
                String xmlns = (String)it.next();
                OntologyInfo info = (OntologyInfo)mappings.get(xmlns);
                if (info.getURL().indexOf("http")>=0)
                {
                	URL local = new URL(info.getURL());
                	mgr.addAltEntry(info.getNS(),local.toString());
                }
                else
                {
                	URI local = new File(info.getURL()).toURI();
            	    mgr.addAltEntry(info.getNS(),local.toString());
                }
            }
            // Then, load the ontologies
            for(Iterator it=mappings.keySet().iterator(); it.hasNext();)
            {
            	String xmlns = (String)it.next();
                OntologyInfo info = (OntologyInfo)mappings.get(xmlns);
                ontologies.read(info.getNS());
            }
        }
    }

    /**
     * @return
     * @throws IOException
     * @throws MalformedURLException
     */
    private void buildMappingsHash() throws MalformedURLException, IOException
    {
        for(ResIterator it=ontologies.listSubjectsWithProperty(RDF.type, MappingRes); 
        	it.hasNext();)
        {
            Resource r = it.nextResource();
            String xmlns=null, alias=null, rdfns=null, rdfsrc=null;
            if(r.hasProperty(xmlnsProp))
                xmlns = r.getProperty(xmlnsProp).getString();
            if(r.hasProperty(aliasProp))
                alias = r.getProperty(aliasProp).getString();
            if(r.hasProperty(rdfnsProp))
                rdfns = r.getProperty(rdfnsProp).getString();
            if(r.hasProperty(rdfsrcProp))
                rdfsrc = r.getProperty(rdfsrcProp).getString();
            this.addMapping(xmlns, alias, rdfns, rdfsrc);
        }
    }

    /**
     * 
     */
    public String getMappings()
    {
        Model mappings = ModelFactory.createDefaultModel();
        for(ResIterator it=ontologies.listSubjectsWithProperty(RDF.type, MappingRes); 
    	it.hasNext();)
        {
	        Resource r = it.nextResource(); Property p = null; RDFNode o = null;
	        Selector selector = new SimpleSelector(r, p, o);
	        for(StmtIterator it2=ontologies.listStatements(selector); it2.hasNext();)
	            mappings.add((Statement)it2.next());
        }
        StringWriter w = new StringWriter();
        mappings.write(w);
        return w.getBuffer().toString();
    }
}

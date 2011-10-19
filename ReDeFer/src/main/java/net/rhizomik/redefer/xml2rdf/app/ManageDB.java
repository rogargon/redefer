package net.rhizomik.redefer.xml2rdf.app;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.db.ModelRDB;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.SelectorImpl;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class ManageDB 
{
    public static final String DB_URL = "jdbc:mysql://localhost/semanticxbrl";
    public static final String DB_USER = "semanticxbrl";
    public static final String DB_PASSWD = "rhizomer";
    public static final String DB = "MySQL";
    public static final String DB_DRIVER = "com.mysql.jdbc.Driver";
    
	public static void main(String[] args) throws SQLException 
	{
        try { Class.forName(DB_DRIVER); }
        catch (Exception e)
        {
        	System.err.println( "Failed to load the driver for the database: " + e.getMessage() );
        }
        
        IDBConnection conn = new DBConnection(DB_URL, DB_USER, DB_PASSWD, DB);
        
        if (args.length > 0 && args[0].equalsIgnoreCase("-clean"))
        	conn.cleanDB();
        
        //conn.getDriver().setDoCompressURI(true);
        
        ModelMaker maker = ModelFactory.createModelRDBMaker(conn);
        ModelRDB dbModel = (ModelRDB)maker.createModel("semanticxbrl", false );
        
        int cacheSize = conn.getDriver().getCompressCacheSize();
        conn.getDriver().setCompressCacheSize(cacheSize*2);
        dbModel.setDoDuplicateCheck( false );
        
        Reasoner r = ReasonerRegistry.getTransitiveReasoner();
        Model model = ModelFactory.createInfModel(r, dbModel);
                
        System.out.println("Model size = "+model.size()+" triples");
        System.exit(0);
	}
}


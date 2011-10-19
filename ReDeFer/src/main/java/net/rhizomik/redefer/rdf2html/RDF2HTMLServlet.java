package net.rhizomik.redefer.rdf2html;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.rhizomik.redefer.util.URIResolverImpl;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RDF2HTMLServlet extends HttpServlet
{
	private URIResolver resolver = null;
	private String transformation = "xsl/rdf2html.xsl"; //Default transformation to apply
	private int MAX_INPUT_LENGTH = 200000;
	
    public void doGet(javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException
    {

        performTask(request, response);
    }

    public void doPost(javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException
    {

        performTask(request, response);
    }

    public String getServletInfo()
    {
        return super.getServletInfo();
    }

    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        if (config.getInitParameter("xsl") != null)
        	transformation = config.getInitParameter("xsl");
        if (config.getInitParameter("maxlength") != null)
        	MAX_INPUT_LENGTH = Integer.parseInt(config.getInitParameter("maxlength"));
        
        resolver = new URIResolverImpl(config.getServletContext().getRealPath("/"));
    }

    public void performTask(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException
    {
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");
    	//response.setContentType("text/html");

        PrintWriter out = new PrintWriter(response.getOutputStream());
        try
        {
            String input = (String) request.getSession().getAttribute("rdf");
            if (input == null)
                input = (String) request.getParameter("rdf");
    		String language = (String) request.getSession().getAttribute("language");
    		if (language == null)
    		    language = (String) request.getParameter("language");
    		String mode = (String) request.getSession().getAttribute("mode");
    		if (mode == null)
    			mode = (String) request.getParameter("mode");
    		String namespaces = (String) request.getSession().getAttribute("namespaces");
    		if (namespaces == null)
    			namespaces = (String) request.getParameter("namespaces");
    		String logo = (String) request.getSession().getAttribute("logo");
    		if (logo == null)
    			logo = (String) request.getParameter("logo");
            
            InputSource iSource;
            Model data = ModelFactory.createDefaultModel();
            try
            {
                URL rdfURL = new URL(input);
                HttpURLConnection urlConn = (HttpURLConnection)rdfURL.openConnection();
                urlConn.setRequestProperty("Accept", "application/rdf+xml, application/xml;q=0.1, text/xml;q=0.1");
                if (urlConn.getContentLength() > MAX_INPUT_LENGTH)
                	throw new ServletException("Sorry, this service is not able to transform inputs bigger than "+MAX_INPUT_LENGTH+" bytes");
                data.read(urlConn.getInputStream(), input, "RDF/XML");
                ByteArrayOutputStream o = new ByteArrayOutputStream();
        		data.write(o, "RDF/XML-ABBREV");
        		o.flush();
        		String rdfxml = o.toString("UTF8");
                iSource = new InputSource(new StringReader(rdfxml));
            } 
            catch (MalformedURLException e)
            {
                // If rdf is not an URL, consider it is directly the RDF to render
            	data.read(new StringReader(input), "", "RDF/XML");
            	ByteArrayOutputStream o = new ByteArrayOutputStream();
        		data.write(o, "RDF/XML-ABBREV");
        		o.flush();
        		String rdfxml = o.toString("UTF8");
            	iSource = new InputSource(new StringReader(rdfxml));
            }
            
            SAXParserFactory pFactory = SAXParserFactory.newInstance();
            pFactory.setNamespaceAware(true);
            pFactory.setValidating(false);
            XMLReader xmlReader = pFactory.newSAXParser().getXMLReader();
            
            TransformerFactory tFactory = TransformerFactory.newInstance();
            tFactory.setURIResolver(resolver);
            
            String base = getServletContext().getRealPath("/");
			Source xslSource = new StreamSource(new File(base+transformation));
            // Generate the transformer
            Transformer transformer = tFactory.newTransformer(xslSource);
            if (language!=null) transformer.setParameter("language", language);
            if (mode!=null)
            {
            	transformer.setParameter("mode", mode);
            	if (mode.equals("snippet") || mode.equals("rhizomer"))
            		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            	if (mode.equals("html"))
            	{
                	transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                	if (this.getServletName().equals("RDF2Microdata"))
                	{
                    	transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "HTML");
                	}
                	else
                	{
                    	transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD XHTML+RDFa 1.0//EN");
                    	transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd");
                	}                		
            	}
            }
            else
            	transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            
            if (namespaces!=null) transformer.setParameter("namespaces", namespaces);
            if (logo!=null) transformer.setParameter("logo", logo);
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF8");
            // Perform the transformation, sending the result to output
            transformer.transform(new SAXSource(xmlReader, iSource), new StreamResult(out));
            out.close();
        } 
        catch (Exception e)
        {
        	throw new ServletException(e);
        }
    }
}
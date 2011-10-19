package net.rhizomik.redefer.xml2rdf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
* @author: http://rhizomik.net/~roberto
 *
 */
public class XML2RDFServlet extends HttpServlet 
{
    public static XSD2OWLMapper map = null;
    /**
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws MalformedURLException
     * 
     */
    public XML2RDFServlet() 
    {
        super();
    }
	public void doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, java.io.IOException {

		performTask(request, response);

	}
	public void doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, java.io.IOException {

		performTask(request, response);

	}
	public String getServletInfo() 
	{
		return super.getServletInfo();
	}
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		try
		{
			if (config.getInitParameter("db") != null)
				/* Map DB version */
			    map = new XSD2OWLMapper(config.getInitParameter("db"),
			        config.getInitParameter("user"), config.getInitParameter("passwd"),
			        config.getInitParameter("driver"), config.getInitParameter("type"));
			else
				map = new XSD2OWLMapper(config.getInitParameter("config"));
		}
		catch(Exception e)
		{ throw new ServletException(e); }
	}
	public void performTask(HttpServletRequest request, HttpServletResponse response)
	{
	    OutputStream out = null;
	    PrintWriter writer = null;
	    try
	    {
	        out = response.getOutputStream();
	        writer = new PrintWriter(out);
	        String reset = request.getParameter("reset");
	        String xml = request.getParameter("xml");
	        
	        if (reset!=null && reset.equals("true"))
	        {
	            map.initialise();
	            response.setContentType("text/html");
	            writer.write("Reset done");
	        }
	        else if (xml!=null)
	        {
	            URL xmlURL = new URL(request.getParameter("xml"));
				XML2RDFMapper xml2rdf = new XML2RDFMapper(xmlURL, map);
	            xml2rdf.mapXML2RDF();
	            response.setContentType("text/xml");
	            xml2rdf.getRDF(out, "RDF/XML-ABBREV");
	        }
	        else
	        {
	            response.setContentType("text/xml");
	            writer.write(map.getMappings());
	        }
	        writer.close();
			out.close();
	    }
	    catch(Exception e)
	    {
	        writer.println(e);
	    }
        finally
        {	try{writer.close(); out.close();}catch(Exception e){}	}
	}
}

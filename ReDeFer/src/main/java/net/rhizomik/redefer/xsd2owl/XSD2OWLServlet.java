package net.rhizomik.redefer.xsd2owl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;

import net.rhizomik.redefer.util.URIResolverImpl;

/**
 * @author http://rhizomik.net/~roberto
 *
 */
public class XSD2OWLServlet extends HttpServlet
{
	private URIResolver resolver = null;
	private int MAX_INPUT_LENGTH = 200000;

	public XSD2OWLServlet()
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
        if (config.getInitParameter("maxlength") != null)
        	MAX_INPUT_LENGTH = Integer.parseInt(config.getInitParameter("maxlength"));
        
        resolver = new URIResolverImpl(config.getServletContext().getRealPath("/"));
        //System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
	}
	public void performTask(HttpServletRequest request, HttpServletResponse response) throws ServletException 
	{
		try
		{
			String xsdURL = (String)request.getParameter("xsd");
			
            System.out.println("\n>>XSD2OWL Access<<\n"+
            		"RemoteAddr: "+request.getRemoteAddr()+"\n"+
            		"RequestXSD: "+xsdURL+"\n");
			
			URLConnection conn = new URL(xsdURL).openConnection();
			int length = conn.getContentLength();
			if (length > MAX_INPUT_LENGTH)
				throw new ServletException("Sorry, this service is not able to transform inputs bigger than "+MAX_INPUT_LENGTH+" bytes");
			InputStreamReader input = new InputStreamReader(new URL(xsdURL).openStream());
			TransformerFactory tFactory = TransformerFactory.newInstance();
			tFactory.setURIResolver(resolver);
			Source xmlSource =	new StreamSource(input);
			String base = getServletContext().getRealPath("/");
			Source xslSource = new StreamSource(new File(base+"xsl/xsd2owl.xsl"));
			Transformer transformer = tFactory.newTransformer(xslSource);
			
			StringWriter writer = new StringWriter();
			transformer.transform(xmlSource, new StreamResult(writer));
			String result = new String(writer.getBuffer());
			result = result.replaceAll("&amp;", "&");
			
			response.setContentType("text/xml");
			response.getWriter().write(result);
			response.getWriter().close();
		}
        catch (Exception e)
        {
        	throw new ServletException(e);
        }
	}
	
	public static void main(String[] args) throws MalformedURLException, IOException, TransformerException
	{
		URIResolver resolver = new URIResolverImpl("src/main/webapp/");
		
		File fIn = new File(args[0]);
		InputStreamReader input = new InputStreamReader(new FileInputStream(fIn));
		
		TransformerFactory tFactory = TransformerFactory.newInstance();
		tFactory.setURIResolver(resolver);
		Source xmlSource =	new StreamSource(input);
		Source xslSource = new StreamSource(new File("src/main/webapp/"+"xsl/xsd2owl.xsl"));
		Transformer transformer = tFactory.newTransformer(xslSource);
		
		File fOut = new File(fIn.getPath().substring(0, fIn.getPath().lastIndexOf('.'))+".owl");
		
		transformer.transform(xmlSource, new StreamResult(fOut));
	}
}
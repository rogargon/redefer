package net.rhizomik.redefer.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

@SuppressWarnings("serial")
public class IsIsomorphicServlet extends HttpServlet
{	
	String extractor = "http://www.w3.org/2007/08/pyRdfa/extract?uri=";
	String rdf2rdfa = "http://rhizomik.net/redefer-services/rdf2rdfa?rdf=";
	
	public void doGet(javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response)
            throws ServletException, IOException
    {

        performTask(request, response);
    }

    public void doPost(javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response)
    		throws ServletException, IOException
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
    }
    
    private Model readModelURL(URL url) throws IOException
    {
    	Model m = ModelFactory.createDefaultModel();
		
		HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
        //urlConn.setRequestProperty("Accept", "application/rdf+xml, application/xml;q=0.1, text/xml;q=0.1");
        
        String encoding = urlConn.getContentType();
        if (encoding == null)
        	m.read(url.toString(), null, "RDF/XML");
        if (encoding.contains("turtle"))
        	m.read(url.toString(), null, "TTL");
        else if (encoding.contains("n3"))
        	m.read(url.toString(), null, "N3");
        else if (encoding.contains("triple"))
        	m.read(url.toString(), null, "N-TRIPLE");
        else
        	m.read(url.toString(), null, "RDF/XML");
        
        return m;
    }

    public void performTask(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException
    {
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");
    	response.setContentType("text/html");

        PrintWriter out = new PrintWriter(response.getOutputStream());

        String reference = (String) request.getSession().getAttribute("reference");
        if (reference == null)
        	reference = (String) request.getParameter("reference");
		String generated = (String) request.getSession().getAttribute("generated");
		if (generated == null)
			generated = (String) request.getParameter("generated");
		
		if (reference==null || generated==null)
			throw new ServletException("Both a reference parameter and a generated parameter should be provided");

		URL referenceURL = new URL(reference);
		URL generatedURL = new URL(generated);
	
		Model rModel = readModelURL(referenceURL);
		Model gModel = readModelURL(generatedURL);

		gModel.removeAll(null, gModel.createProperty("http://www.w3.org/1999/xhtml/vocab#stylesheet"), null);
		gModel.removeAll(null, gModel.createProperty("http://www.w3.org/1999/xhtml/microdata#item"), null);
		//gModel.removeAll(null, RDF.type, RDFS.Resource);

		out.println("<h2>Both models are:");
		if (gModel.isIsomorphicWith(rModel))
		{
			out.println("<b>EQUAL</b></h2>");
			out.println("<p>Thought there might be syntactical differences in the serialisation...</p>");
		}
		else
			out.println("<b>NOT EQUAL</b></h2>");
		
		StringWriter rWriter = new StringWriter();
		StringWriter gWriter = new StringWriter();
		rModel.write(rWriter, "N-TRIPLE");
		gModel.write(gWriter, "N-TRIPLE");

		diff_match_patch diff = new diff_match_patch();
		LinkedList<Diff> diffs = diff.diff_main(rWriter.toString(), gWriter.toString());
		diff.diff_cleanupSemantic(diffs);
		
		out.println("<h3>Differences</h3>");
		out.println(diff.diff_prettyHtml(diffs));
		out.close();
	}
}

package net.rhizomik.redefer.xml2rdf;

/**
 * @author http://rhizomik.net/~roberto
 *
 */
public class OntologyInfo
{
	private String alias;
	private String ns;
	private String url;
		
	public OntologyInfo(String alias, String ns, String url)
	{
		this.alias = alias;
		this.ns = ns;
		this.url = url;
	}
	public String getAlias()
	{
		return this.alias;
	}
	public String getNS()
	{
		return this.ns;
	}
	public String getURL()
	{
		return this.url;
	}
}

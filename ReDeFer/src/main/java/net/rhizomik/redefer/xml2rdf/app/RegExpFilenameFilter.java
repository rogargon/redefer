package net.rhizomik.redefer.xml2rdf.app;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExpFilenameFilter implements FilenameFilter
{
	private Pattern p;
	
	public RegExpFilenameFilter(String regexp) 
	{
		p = Pattern.compile(regexp);
	}
	public boolean accept(File dir, String name) 
	{
		Matcher m = p.matcher(name);
		return m.matches();
	}
}

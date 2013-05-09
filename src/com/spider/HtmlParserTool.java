package com.spider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.beans.StringBean;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class HtmlParserTool
{
	/**
	 * @param args
	 */
	private  Parser parser =  null;
	public static String[] entities = {"&nbsp;", "&quot;", "&apos;", "&amp;", "&lt;", "&gt;"};
	public static String[] tranfers = {" ", "\"", "'", "&", "<", ">"};
	public static String replaceAll(String content)
	{
		StringBuilder result = new StringBuilder();
		for(int i = 0; i < content.length(); ++i)
		{
			char ch = content.charAt(i);
			if(ch == '&')
			{
				String temp = null;
				int j;
				for(j = 0; j < entities.length; ++j)
				{
					int beg = i;
					int end = Math.min(beg + entities[j].length(), content.length());
					temp = content.substring(beg, end);
					//System.out.println("temp = " + temp);
					if( entities[j].equals(temp) )
					{
						result.append(tranfers[j]);
						i = end - 1;
						break;
					}
				}
				if(j >= entities.length)
					result.append(ch);
			}
			else result.append(ch);
		}
		return result.toString();
	}
	public void test()
	{
		try
		{
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet("http://blog.csdn.net/hustluy/article/details/8829159");
			
			
			String content = httpClient.execute(httpGet, new BasicResponseHandler());
			//System.out.println(content);
			parser = Parser.createParser(content, "ISO8859-1");
			
			NodeFilter nodeFilter = new NodeFilter()
			{

				@Override
				public boolean accept(Node node)
				{
					// TODO Auto-generated method stub
					if(node instanceof Div)
					{
						String attribute = ((Div)node).getAttribute("id");
						if(attribute != null && attribute.equalsIgnoreCase("article_content"))
							return true;
						else return false;
					}
					else return false;
				}
				
			};

			
			NodeList nodeList = parser.extractAllNodesThatMatch(nodeFilter);
			for(int i = 0; i < nodeList.size(); ++i)
			{
				Node node = nodeList.elementAt(i);
				String articleContent = this.replaceAll(node.toPlainTextString());
				System.out.println(articleContent);
			}
			
		} catch (ParserException e)
		{
			e.printStackTrace();
		} catch (ClientProtocolException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public static void scanRootURL(String url) 
			throws ClientProtocolException, IOException, ParserException, URISyntaxException
	{
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		BasicResponseHandler responseHandler = new BasicResponseHandler();
		String content = httpClient.execute(httpGet, responseHandler);
		Parser parser = new Parser(content);
		NodeFilter nodeFilter = new NodeFilter()
		{

			@Override
			public boolean accept(Node node)
			{
				// TODO Auto-generated method stub
				if(node instanceof LinkTag)
				{
					return true;
				}
				return false;
			}
			
		};
		NodeList nodeList = parser.extractAllNodesThatMatch(nodeFilter);
		for(int i = 0; i < nodeList.size(); ++i)
		{
			//System.out.println();
			//httpGet.abort();
			String tempUrl = ((LinkTag)nodeList.elementAt(i)).getLink();
			if(tempUrl.indexOf("http://") == -1)
				tempUrl = "http://blog.csdn.net" + tempUrl;
			try
			{
				httpGet.setURI(new URI(tempUrl));
			
			String temp = httpClient.execute(httpGet, responseHandler);
			//System.out.println(temp);
			}
			catch(Exception e)
			{
				System.out.println(tempUrl);
				e.printStackTrace();
			}
		}
		
	}
	public static void main(String[] args) throws Exception
	{
		// TODO Auto-generated method stub
		new HtmlParserTool().test();
		//HtmlParserTool.scanRootURL("http://blog.csdn.net");
	}

}

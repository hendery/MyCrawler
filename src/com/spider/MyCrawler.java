package com.spider;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;


public class MyCrawler
{
	private static String rootUrl = "http://blog.csdn.net";
	private static String prefix = "http://blog.csdn.net";
	private Queue<String> queue = new LinkedList<String>();
	private  HashSet<String> visitedURLs = new HashSet<String>();
	private Pattern articleURLPattern = Pattern.compile(this.prefix 
			+ "(/\\w+/article/details/\\d+)$");
	private static int count = 0;
	private HttpClient httpClient = null;
	private static File parentDir = new File("data");
	
	public void beginCrawling(String seedURL)
	{
		this.httpClient = new DefaultHttpClient();
		this.queue.add(seedURL);
		while(!this.queue.isEmpty() && count <= 1000)
		{
			String url = this.queue.poll();
			String html = null;
			try
			{
				html = this.downloadPageFrom(url);
			}
			catch(Exception e)
			{
				HttpResponseException he = (HttpResponseException)e;
				System.out.println(he.getMessage() + "-----" + "program will sleep!");
				if(he.getStatusCode() == HttpStatus.SC_FORBIDDEN)
				{
					try
					{
						Thread.sleep(1000000);
						html = this.downloadPageFrom(url);
					} catch (InterruptedException e1)
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
			this.retrieveURLs(html);
			if( articleURLPattern.matcher(url).find() )
			{
				System.out.println(url + "is an article!"); 
				this.extractArticleFromPage(url, html);
			}
			else System.out.println(url + " is not an article!");
		}
	}
	
	public String downloadPageFrom(String url)
	{
		HttpGet httpGet = new HttpGet(url);
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String html = null;
		try
		{
			html = httpClient.execute(httpGet, responseHandler);
			//this.retrieveUrl(html);
			
		} catch (ClientProtocolException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally
		{
			httpGet.abort();
		}
		return html;
	}
	
	public void retrieveURLs(String html)
	{
		// retrivel url like http://blog.csdn.net/username(/article/details/id) 
		//                         or /username/article/details/id
		try
		{
			Parser htmlParser = new Parser(html);
			
			LinkRegexFilter outLinkFilter = new LinkRegexFilter(this.prefix 
					+ "/\\w+(/article/details/\\d+)?$");
			LinkRegexFilter inLinkFilter = new LinkRegexFilter("(/\\w+/article/details/\\d+)$");
			OrFilter orLinkFilter = new OrFilter(inLinkFilter, outLinkFilter);
			
			NodeList nodeList = htmlParser.parse(orLinkFilter);
			for(int i = 0; i < nodeList.size(); ++i)
			{
				LinkTag linkTag = (LinkTag)nodeList.elementAt(i);
				String link = linkTag.getLink();
				if( link.indexOf(this.prefix) == -1)
					link = this.prefix + link;
				if( !visitedURLs.contains(link) )
				{
					//System.out.println(link);
					queue.add(link);
					visitedURLs.add(link);
				}
			}
		} catch (ParserException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String fetchFilepath(String url)
	{
		int beg = this.prefix.length() + 1;
		int end = url.indexOf('/', beg);
		if( !this.parentDir.exists() )
			this.parentDir.mkdir();
		String dirname = this.parentDir.toString() + File.separator + url.substring(beg, end);
		File dir = new File(dirname);
		if( !dir.exists() )
		{
			if( !dir.mkdir() )
			{
				System.out.println("Fails to create " + dirname);
				System.exit(1);
			}
		}
		String filename = url.substring(url.lastIndexOf('/')+1);
		return  dirname + File.separator + filename;
	}
	
	public void extractArticleFromPage(String url, String html)
	{
		String filepath = fetchFilepath(url);
		Parser htmlParser = null;
		FileWriter fw = null;
		BufferedWriter bw = null;
		try
		{	
			fw = new FileWriter(filepath);
			bw = new BufferedWriter(fw);
			htmlParser = new Parser(html);
			NodeFilter articleFilter = new NodeFilter()
			{
				@Override
				public boolean accept(Node node)
				{
					// TODO Auto-generated method stub
					if( (node instanceof Div) )
					{
						Div div = (Div)node;
						if( div.getAttribute("id") != null 
								&& div.getAttribute("id").equalsIgnoreCase("article_content")
								&& div.getAttribute("class") != null
								&& div.getAttribute("class").equalsIgnoreCase("article_content") )
							return true;
						else return false;
					}
					return false;
				}
			};
			NodeList nodeList = htmlParser
					.extractAllNodesThatMatch(articleFilter);
			for (int i = 0; i < nodeList.size(); ++i)
			{
				Node node = nodeList.elementAt(i);
				String partContent = HtmlParserTool.replaceAll(node
						.toPlainTextString());
				bw.write(partContent);
			}
			System.out.println(filepath + "has been loaded!");
			bw.close();
			fw.close();
			
		} catch (ParserException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	
	public static void main(String[] args) throws Exception
	{
		//new MyCrawler().downloadPageFrom("http://blog.csdn.net/hustluy/article/details/8482430");
		
		//Generate a dir to store data crawled
		//this is a update
		new MyCrawler().beginCrawling(MyCrawler.rootUrl);
		Thread.sleep(1000);
		System.out.println("Game Over!");
	}

}

/*Pattern pattern = Pattern.compile("<a href=\"/hejingyuan6/article/details/\\d+\">");
Matcher matcher = pattern.matcher(content);
while(matcher.find())
{
	String matcherResult = matcher.group();
	System.out.println(matcherResult);
	String suffix = 
			matcherResult.substring(matcherResult.indexOf('/'), matcherResult.lastIndexOf('\"'));
	String url = MyCrawler.prefix + suffix;
	if( !visitedUrls.contains(url) )
	{
		visitedUrls.add(url);
		this.downloadPageFrom(url);
	}
}*/

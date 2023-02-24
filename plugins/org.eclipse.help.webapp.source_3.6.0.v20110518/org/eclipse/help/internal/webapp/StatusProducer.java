/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.help.internal.webapp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IHelpContentProducer;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.remote.RemoteStatusData;
import org.eclipse.help.internal.protocols.HelpURLStreamHandler;
import org.eclipse.help.internal.util.ProductPreferences;
import org.eclipse.help.internal.webapp.data.WebappPreferences;


public class StatusProducer implements IHelpContentProducer {

	// Remote Status 'page' href
	public static final String REMOTE_STATUS_HREF = "NetworkHelpStatus.html"; //$NON-NLS-1$
	// Remote Status from bad topic 'page' href
	public static final String MISSING_TOPIC_HREF = "MissingTopicStatus.html"; //$NON-NLS-1$
	// Remote Status from bad topic 'page' href
	public static final String SEARCH_REMOTE_STATUS_HREF = "SearchNetworkHelpStatus.html"; //$NON-NLS-1$
	// Default TAB size
	private static final String TAB = "  "; //$NON-NLS-1$
	// index.jsp
	private static final String INDEX = "/index.jsp"; //$NON-NLS-1$
	
	// HTML constants
	private static final String BEGIN_HEAD_HTML = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n" //$NON-NLS-1$
		+ "<html>\n" //$NON-NLS-1$
		+ tab(1)+"<head>\n" //$NON-NLS-1$
		+ tab(2)+"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"; //$NON-NLS-1$
	
	private static final String END_HEAD_HTML = tab(1)+"</head>\n"; //$NON-NLS-1$

	private static final String END_BODY_HTML = tab(2)+"</div>\n"+tab(1)+"</body>\n</html>"; //$NON-NLS-1$ //$NON-NLS-2$
	
	
	
	public InputStream getInputStream(String pluginID, String href, Locale locale) {

		// Only accept requests for our pages.  Otherwise
		// return null so Eclipse tries to find the right help
		if (!href.equalsIgnoreCase(REMOTE_STATUS_HREF) && 
			!href.equalsIgnoreCase(MISSING_TOPIC_HREF) &&
			!href.equalsIgnoreCase(SEARCH_REMOTE_STATUS_HREF))
			return null;
		
		StringBuffer pageBuffer = new StringBuffer();
		
		
		// Get all remote sites, and subset of non-working sites
		ArrayList remoteSites = RemoteStatusData.getRemoteSites();
		ArrayList badSites = RemoteStatusData.checkSitesConnectivity(remoteSites);
		RemoteStatusData.clearCache();

		// Check to see if there are any enabled remote sites.
		// If not, return null - default topic not found will display
		if (remoteSites.isEmpty())
		{
			return null;
		}
		
		// If this is a call from an invalid topic,
		// check to see if a predefined error page exists
		// in the preferences
		if (href.equalsIgnoreCase(MISSING_TOPIC_HREF)){
            String errorPage = Platform.getPreferencesService().getString(
            		HelpBasePlugin.PLUGIN_ID, 
            		"page_not_found",  //$NON-NLS-1$
            		null, 
            		null);
			if (errorPage != null && errorPage.length() > 0) {	

				URL helpURL;
				try {
					helpURL = new URL("help", //$NON-NLS-1$
								null, -1, errorPage,
								HelpURLStreamHandler.getDefault());
					return helpURL.openStream();
				} catch (MalformedURLException e) {
					HelpWebappPlugin.logError("Unable to locate error page: "+errorPage, e); //$NON-NLS-1$
				} catch (IOException e) {
					HelpWebappPlugin.logError("Unable to open error page: "+errorPage, e); //$NON-NLS-1$
				}
				
			}
		}
		

		// Write HTML header and body beginning.
		pageBuffer.append(getHtmlHead(locale));
		pageBuffer.append(getBeginHtmlBody());
		
		

		// Check to see if all remote sites failed,
		// or just a subset
		boolean allFailed;
		if (remoteSites.size()==badSites.size())
		{
			allFailed = true;
			pageBuffer.append(tab(3)+"<h1>"+WebappResources.getString("allRemoteHelpUnavailable", locale)+"</h1>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		else
		{
			allFailed = false;
			pageBuffer.append(tab(3)+"<h1>"+WebappResources.getString("someRemoteHelpUnavailable", locale)+"</h1>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		

		// Add a close link to top
		if (href.equalsIgnoreCase(REMOTE_STATUS_HREF))
		{
			WebappPreferences prefs = new WebappPreferences();
			String homepage = "/help/topic"+prefs.getHelpHome(); //$NON-NLS-1$
			
			pageBuffer.append(tab(3)+"<div style=\"position:absolute;right:4px;top:4px;\">\n"); //$NON-NLS-1$
			pageBuffer.append(tab(4)+"<table>\n"+tab(5)+"<tr>\n"); //$NON-NLS-1$ //$NON-NLS-2$
			pageBuffer.append(tab(6)+"<td style=\"background-color:white;border-width:1px;border-style:solid;border-color:grey;\">"+makeAnchor(homepage,WebappResources.getString("Close", locale),"style=\"font-size:.8em;\"",false)+"</td>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			pageBuffer.append(tab(5)+"</tr>\n"+tab(4)+"</table>\n"+tab(3)+"</div>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$		
		}
		
		
		if (href.equalsIgnoreCase(MISSING_TOPIC_HREF))
			pageBuffer.append(tab(3)+"<p>"+WebappResources.getString("topicUnavailable",locale)+"</p>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		// Write potential causes, based on some or
		// all sites failing.
		pageBuffer.append(tab(3)+"<p>"+WebappResources.getString("potentialCauses",locale)+"</p>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		pageBuffer.append(tab(3)+"<ul>\n"); //$NON-NLS-1$
		pageBuffer.append(tab(4)+"<li>"+WebappResources.getString("serversCouldBeDown",locale)+"</li>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		pageBuffer.append(tab(4)+"<li>"+WebappResources.getString("mayNeedProxy",locale)+"</li>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (allFailed)
			pageBuffer.append(tab(4)+"<li>"+WebappResources.getString("networkCouldBeDown",locale)+"</li>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		pageBuffer.append(tab(3)+"</ul>\n"); //$NON-NLS-1$
		
		
		// Check for bad sites, and write them

		if (remoteSites.size()>badSites.size())
		{
			pageBuffer.append(tab(3)+"<h2>"+WebappResources.getString("sitesWithConnectivity", locale)+"</h2>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			pageBuffer.append(tab(3)+"<ul>\n"); //$NON-NLS-1$
			for (int r=0;r<remoteSites.size();r++)
			{
				if (!badSites.contains(remoteSites.get(r)))
					pageBuffer.append(tab(4)+"<li>"+makeAnchor(remoteSites.get(r)+INDEX,remoteSites.get(r)+INDEX,"",true)+"</li>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			pageBuffer.append(tab(3)+"</ul>\n"); //$NON-NLS-1$
		}
		else
			pageBuffer.append(tab(3)+WebappResources.getString("noRemoteSitesAvailable", locale)+"</br>\n"); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (!badSites.isEmpty())
		{
			pageBuffer.append(tab(3)+"<h2>"+WebappResources.getString("sitesWithoutConnectivity", locale)+"</h2>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			pageBuffer.append(tab(3)+"<ul>\n"); //$NON-NLS-1$
			
			for (int b=0;b<badSites.size();b++)
				pageBuffer.append(tab(4)+ "<li>"+badSites.get(b)+INDEX+"</li>\n"); //$NON-NLS-1$ //$NON-NLS-2$
			
			pageBuffer.append(tab(3)+ "</ul>\n"); //$NON-NLS-1$
		}
		
		String activeLink = 
			MessageFormat.format(
					WebappResources.getString("remotePreferences", locale), //$NON-NLS-1$
					new String[]{getActiveLink(locale)});

		pageBuffer.append(tab(3)+activeLink);

		pageBuffer.append(END_BODY_HTML);
		
		
//		String tmp = pluginID+" , "+href; //$NON-NLS-1$
//		System.out.println(tmp);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(pageBuffer.toString().getBytes());
		
		return bais;
	}

	

	/*
	 * Build the HTML header
	 */
	private String getHtmlHead(Locale locale)
	{
		return BEGIN_HEAD_HTML + '\n'
			+ tab(2) + "<meta name=\"copyright\" content=\"Copyright (c) IBM Corporation and others 2000, 2009. This page is made available under license. For full details see the LEGAL in the documentation book that contains this page.\" >\n" //$NON-NLS-1$
			+ tab(2) + "<title>"+WebappResources.getString("remoteStatusTitle", locale)+"</title>\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			+ tab(2) + "<link rel=\"stylesheet\" href=\"PLUGINS_ROOT/org.eclipse.help.base/doc/book.css\" charset=\"utf-8\" type=\"text/css\">\n" //$NON-NLS-1$
			+ tab(2) + "<script language=\"JavaScript\" src=\"PLUGINS_ROOT/org.eclipse.help/livehelp.js\"> </script>\n" //$NON-NLS-1$
			+ tab(2) + "<script type=\"text/javascript\" src=\"../../../content/org.eclipse.help/livehelp.js\"></script>\n" //$NON-NLS-1$
			+ END_HEAD_HTML;
	}
	
	/*
	 * Build the beginning of the HTML body
	 */
	private String getBeginHtmlBody()
	{
		String body = tab(1);
		
		if (ProductPreferences.isRTL())
			body += "<body dir=\"rtl\">"; //$NON-NLS-1$
		else
			body += "<body>"; //$NON-NLS-1$
		
		
		return body + '\n'
			+ tab(2) + "<div id=\"banner\"><img src=\"PLUGINS_ROOT/org.eclipse.help.base/doc/help_banner.jpg\" alt=\"Help banner\" width=\"1600\" height=\"36\"></div>\n" //$NON-NLS-1$
			+ tab(2) + "<div id=\"content\">\n"; //$NON-NLS-1$
	}
	
	/*
	 * Build the active help link that opens the
	 * remote infocenter content preferences
	 */
	private String getActiveLink(Locale locale)
	{
		return "<img src=\"PLUGINS_ROOT/org.eclipse.help/command_link.png\"/>"  //$NON-NLS-1$
			+ "<a class=\"command-link\""  //$NON-NLS-1$
			+ " href='javascript:executeCommand(\"org.eclipse.ui.window.preferences(preferencePageId=org.eclipse.help.ui.contentPreferencePage)\")'>" //$NON-NLS-1$
			+ WebappResources.getString("remotePreferencesMenuSelect", locale)+"</a>"; //$NON-NLS-1$ //$NON-NLS-2$
	}	
	
	/*
	 * Generate an HTML anchor.  Anchors
	 * will open in a new window / tab based on
	 * newWindow arg.
	 */
	private String makeAnchor(String url,String title,String style,boolean newWindow)
	{
		String target=""; //$NON-NLS-1$
		if (newWindow)
			target = "target=\"_blank\" ";  //$NON-NLS-1$
		
		return "<a "+style+" "+target+"href=\""+url+"\">"+title+"</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}
	
	/*
	 * Generate tabbed spacing for HTML elements
	 */
	private static String tab(int count)
	{
		String tabs = ""; //$NON-NLS-1$
		for (int i=0;i<count;i++)
			tabs+=TAB;
		return tabs;
	}	
	
}

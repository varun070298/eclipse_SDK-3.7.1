/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.help.internal.webapp.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.webapp.HelpWebappPlugin;
import org.eclipse.help.internal.webapp.WebappResources;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import com.ibm.icu.text.Collator;

public class AboutServlet extends HttpServlet {

	private static final int NUMBER_OF_COLUMNS = 4;
	
	private class PluginDetails {
		private String[] columns = new String[4];
		
		PluginDetails(String[] columns) {
			this.columns = columns;
			for (int i = 0 ; i < NUMBER_OF_COLUMNS; i++) {
				if (columns[i] == null) {
					columns[i] = ""; //$NON-NLS-1$
				}
			}
		}
	}
	
	private class PluginComparator implements Comparator {
		
        public PluginComparator(int column) {
        	this.column = column;
		}
		private int column;
		
		public int compare(Object o1, Object o2) {
			PluginDetails pd1 = (PluginDetails) o1;
			PluginDetails pd2 = (PluginDetails) o2;
			return Collator.getInstance().compare(pd1.columns[column], pd2.columns[column]);
		}

	}

	public AboutServlet() {
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1426745453574711075L;
	private static final String XHTML_1 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n<title>"; //$NON-NLS-1$
	private static final String XHTML_2 = "</title>\n <style type = \"text/css\"> td { padding-right : 10px; }</style></head>\n<body>\n"; //$NON-NLS-1$
	private static final String XHTML_3 = "</body>\n</html>"; //$NON-NLS-1$
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		resp.setContentType("text/html; charset=UTF-8"); //$NON-NLS-1$
		Locale locale = req.getLocale();
		StringBuffer buf = new StringBuffer();
		buf.append(XHTML_1);
		String showParam = req.getParameter("show"); //$NON-NLS-1$
		if ("agent".equalsIgnoreCase(showParam)) { //$NON-NLS-1$
			getAgent(req, resp);
			return;
		}
		if ("preferences".equalsIgnoreCase(showParam)) { //$NON-NLS-1$
			getPreferences(req, resp);
			return;
		}
		String sortParam = req.getParameter("sortColumn"); //$NON-NLS-1$
		int sortColumn = 3;
		if (sortParam != null) {
			try {
				sortColumn = Integer.parseInt(sortParam);
			} catch (NumberFormatException e) {
			}	
		}

		String title = WebappResources.getString("aboutPlugins", locale); //$NON-NLS-1$
		buf.append(UrlUtil.htmlEncode(title));
		buf.append(XHTML_2);
		buf.append("<table>");  //$NON-NLS-1$
		List plugins = new ArrayList();

		Bundle[] bundles = HelpWebappPlugin.getContext().getBundles();
		for (int k = 0; k < bundles.length; k++) {
         	plugins.add(pluginDetails(bundles[k]));
        }
		
        Comparator pluginComparator = new PluginComparator(sortColumn);
		Collections.sort(plugins, pluginComparator );
		String[] headerColumns = new String[]{
		    WebappResources.getString("provider", locale), //$NON-NLS-1$
		    WebappResources.getString("pluginName", locale), //$NON-NLS-1$
		    WebappResources.getString("version", locale), //$NON-NLS-1$
		    WebappResources.getString("pluginId", locale) //$NON-NLS-1$
		};
		PluginDetails header = new PluginDetails(headerColumns);
		buf.append(headerRowFor(header));
        for (Iterator iter = plugins.iterator(); iter.hasNext();) {
        	PluginDetails details = (PluginDetails) iter.next();
        	buf.append(tableRowFor(details));
        }
		buf.append("</table>"); //$NON-NLS-1$
		buf.append(XHTML_3);
		String response = buf.toString();
		resp.getWriter().write(response);		
	}

	private void getPreferences(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		StringBuffer buf = new StringBuffer();
		buf.append(XHTML_1);
		String title = WebappResources.getString("preferences", req.getLocale()); //$NON-NLS-1$
		buf.append(UrlUtil.htmlEncode(title));
		buf.append(XHTML_2);
		buf.append("<h1>"); //$NON-NLS-1$
		buf.append(title);
		buf.append("</h1>"); //$NON-NLS-1$
		PreferenceWriter writer = new PreferenceWriter(buf, req.getLocale());
		writer.writePreferences();
		buf.append(XHTML_3);
		String response = buf.toString();
		resp.getWriter().write(response);	
	}

	private void getAgent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		StringBuffer buf = new StringBuffer();
		buf.append(XHTML_1);
		String title = WebappResources.getString("userAgent", req.getLocale()); //$NON-NLS-1$
		buf.append(UrlUtil.htmlEncode(title));
		buf.append(XHTML_2);
		buf.append("<h1>"); //$NON-NLS-1$
		buf.append(title);
		buf.append("</h1>"); //$NON-NLS-1$
		String agent = req.getHeader("User-Agent"); //$NON-NLS-1$
		buf.append(UrlUtil.htmlEncode(agent));
		buf.append(XHTML_3);
		String response = buf.toString();
		resp.getWriter().write(response);	
	}

	private String headerRowFor(PluginDetails details) {
		String row = "<tr>\n"; //$NON-NLS-1$
		for (int i = 0; i < 4; i++) {
			row += ("<td><a href = \"about.html?sortColumn="); //$NON-NLS-1$
			row += i;
			row += "\">"; //$NON-NLS-1$
			row += UrlUtil.htmlEncode(details.columns[i]);
			row += "</a></td>\n"; //$NON-NLS-1$
		}
		row += "</tr>"; //$NON-NLS-1$
		return row;
	}
	
	private String tableRowFor(PluginDetails details) {
		String row = "<tr>\n"; //$NON-NLS-1$
		for (int i = 0; i < 4; i++) {
			row += ("<td>"); //$NON-NLS-1$
			row += UrlUtil.htmlEncode(details.columns[i]);
			row += "</td>\n"; //$NON-NLS-1$
		}
		row += "</tr>"; //$NON-NLS-1$
		return row;
	}

	private Object pluginDetails(Bundle bundle) {
		String[] values = new String[] {
		    getResourceString(bundle, Constants.BUNDLE_VENDOR),
	        getResourceString(bundle, Constants.BUNDLE_NAME),
	        getResourceString(bundle, Constants.BUNDLE_VERSION),
	        bundle.getSymbolicName()
		};
		PluginDetails details = new PluginDetails(values);
	    
		return details;
	}
	
	private static String getResourceString(Bundle bundle, String headerName) {
	    String value = (String) bundle.getHeaders().get(headerName);
	    return value == null ? null : Platform.getResourceString(bundle, value);
    }

}

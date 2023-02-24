/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.internal.webapp.data;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.UAContentFilter;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.HelpEvaluationContext;
import org.eclipse.help.internal.base.remote.RemoteHelp;
import org.eclipse.help.internal.base.scope.ScopeUtils;

/**
 * Helper class for tocView.jsp initialization
 */
public class TocData extends ActivitiesData {
	// Request parameters
	private String tocParameter;
	private String topicHref;
	private String expandPathParam;

	// help form of selected topic href
	private String topicHelpHref;
	// Selected TOC
	private int selectedToc;
	// path from TOC to the root topic of the TOC fragment
	private int[] rootPath = null;
	// path from TOC to the selected topic, excluding TOC;
	private ITopic[] topicPath = null;
	
	// String representing the topic path as numbers separated by underscores e.g. 2_4_3
	private String numericPath;

	// List of TOC's, unfiltered
	private IToc[] tocs;
	
	// images directory
	private String imagesDirectory;
	
	// Scope
	private AbstractHelpScope scope;
	/**
	 * Constructs the xml data for the contents page.
	 * 
	 * @param context
	 * @param request
	 */
	public TocData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		super(context, request, response);


		this.tocParameter = request.getParameter("toc"); //$NON-NLS-1$
		this.topicHref = request.getParameter("topic"); //$NON-NLS-1$
		this.expandPathParam = request.getParameter("expandPath"); //$NON-NLS-1$
		this.scope = RequestScope.getScope(request, response, false);
		
		
		if (tocParameter != null && tocParameter.length() == 0)
			tocParameter = null;
		if (topicHref != null && topicHref.length() == 0)
			topicHref = null;
		if (expandPathParam != null && expandPathParam.length() == 0)
				expandPathParam = null;
		
		String anchor = request.getParameter("anchor"); //$NON-NLS-1$
		if (topicHref != null && anchor != null) {
			topicHref = topicHref + '#' + anchor;
		}
		
		// initialize rootPath
		String pathStr = request.getParameter("path"); //$NON-NLS-1$
		if (pathStr != null && pathStr.length() > 0) {
			String[] paths = pathStr.split("_", -1); //$NON-NLS-1$
			int[] indexes = new int[paths.length];
			boolean indexesOK = true;
			for (int i = 0; i < paths.length; i++) {
				try {
					indexes[i] = Integer.parseInt(paths[i]);
				} catch (NumberFormatException nfe) {
					indexesOK = false;
					break;
				}
				if (indexesOK) {
					rootPath = indexes;
				}
			}
		}

		imagesDirectory = preferences.getImagesDirectory();

		loadTocs();
	}

	public boolean isRemoteHelpError() {
		boolean isError = (RemoteHelp.getError() != null);
		if (isError) {
			RemoteHelp.clearError();
		}
		return isError;
	}
	
	// Accessor methods to avoid exposing help classes directly to JSP.
	// Note: this seems ok for now, but maybe we need to reconsider this
	//       and allow help classes in JSP's.
	
	public int getTocCount() {
		return tocs.length;
	}

	public String getTocLabel(int i) {
		return tocs[i].getLabel();
	}

	public String getTocHref(int i) {
		return tocs[i].getHref();
	}

	public String getTocDescriptionTopic(int i) {
		return UrlUtil.getHelpURL(tocs[i].getTopic(null).getHref());
	}

	/**
	 * Returns the selected TOC
	 * 
	 * @return int
	 */
	public int getSelectedToc() {
		return selectedToc;
	}

	/**
	 * Returns the topic to display. If there is a TOC, return its topic
	 * description. Return null if no topic is specified and there is no toc
	 * description.
	 * 
	 * @return String
	 */
	public String getSelectedTopic() {
		if (topicHref != null && topicHref.length() > 0)
			return UrlUtil.getHelpURL(topicHref);
		else
		if (selectedToc == -1)
			return null;
		IToc toc = tocs[selectedToc];
		ITopic tocDescription = toc.getTopic(null);
		if (tocDescription != null)
			return UrlUtil.getHelpURL(tocDescription.getHref());
		return UrlUtil.getHelpURL(null);
	}

	/**
	 * Returns a list of all the TOC's as xml elements. Individual TOC's are not
	 * loaded yet.
	 * 
	 * @return IToc[]
	 */
	public IToc[] getTocs() {
		return tocs;
	}

	/**
	 * Check if given TOC is visible
	 * 
	 * @param toc
	 * @return true if TOC should be visible
	 */
	public boolean isEnabled(int toc) {
		return ScopeUtils.showInTree(tocs[toc], scope);
	}
	/**
	 * Check if given TOC is visible
	 * 
	 * @param toc
	 * @return true if TOC should be visible
	 */
	private boolean isEnabled(IToc toc) {
		if(!isAdvancedUI()){
			// activities never filtered for basic browsers
			return true;
		}
		return HelpBasePlugin.getActivitySupport().isEnabled(toc.getHref()) &&
			!UAContentFilter.isFiltered(toc, HelpEvaluationContext.getContext());
	}

	private void loadTocs() {
		tocs = HelpPlugin.getTocManager().getTocs(getLocale());
		// Find the requested TOC
		selectedToc = -1;
		if (isExpandPath()) {
			getEnabledTopicPath();
		} else if (tocParameter != null && tocParameter.length() > 0) {
			for (int i = 0; selectedToc == -1 && i < tocs.length; i++) {
				if (tocParameter.equals(tocs[i].getHref())) {
					selectedToc = i;
				}
			}
		} else {
			// try obtaining the TOC from the topic
			TopicFinder finder = new TopicFinder(topicHref, tocs, scope);
			topicPath = finder.getTopicPath();
			selectedToc = finder.getSelectedToc();
			numericPath = finder.getNumericPath();
		}
	}
	
	private void getEnabledTopicPath() {
		int[] path = UrlUtil.splitPath(expandPathParam);
		if (path != null) {
			// path[0] is the index of enabled TOCS, convert to an index into all TOCS
			int enabled = path[0] + 1;
			for (int i = 0;  enabled > 0 && i < tocs.length; i++) {
				if (ScopeUtils.showInTree(tocs[i], scope)) {
					enabled--;
					if (enabled == 0) {
						selectedToc = i;
					}
				}
			}
			if (selectedToc != -1) {
		        topicPath = decodePath(path, tocs[selectedToc], scope);
			}
		} else {
			selectedToc = -1;
		}
	}

	public static ITopic[] decodePath(int[] path, IToc toc, AbstractHelpScope scope) {
		ITopic[] topicPath = new ITopic[path.length - 1];
		try {
			if (path.length > 1) {
				ITopic[] topics = toc.getTopics();
				ITopic[] enabledTopics = ScopeUtils.inScopeTopics(topics, scope);
				topicPath[0] = enabledTopics[path[1]];
			}
			for (int i = 1; i < topicPath.length; i++) {
				ITopic[] topics = topicPath[i-1].getSubtopics();
				ITopic[] enabledTopics = ScopeUtils.inScopeTopics(topics, scope);
				topicPath[i] = enabledTopics[path[i+1]];
			}
		} catch (RuntimeException e) {
			return null;
		}
		return topicPath;
	}

	/**
	 * Generates the HTML code (a tree) for a TOC.
	 * 
	 * @param toc
	 * @param out
	 * @throws IOException
	 */
	public void generateBasicToc(int toc, Writer out) throws IOException {
		ITopic[] topics = getEnabledSubtopics(tocs[toc]);
		for (int i = 0; i < topics.length; i++) {
			generateBasicTopic(topics[i], out);
		}

	}

	private void generateBasicTopic(ITopic topic, Writer out)
			throws IOException {

		out.write("<li>"); //$NON-NLS-1$
		ITopic[] topics = getEnabledSubtopics(topic);
		boolean hasNodes = topics.length > 0;
		if (hasNodes) {
			out.write("<nobr>"); //$NON-NLS-1$
			out.write("<a "); //$NON-NLS-1$
			if (getSelectedTopicHelpHref().equals(topic.getHref())) {
				out.write("name=\"selectedItem\" "); //$NON-NLS-1$
			}
			out.write("href="+"\"" + UrlUtil.getHelpURL(topic.getHref())+"\"" + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			out.write("<img src='"); //$NON-NLS-1$
			out.write(imagesDirectory);
			out.write("/container_obj.gif' alt=\"\" border=0>&nbsp;"); //$NON-NLS-1$
			out.write(UrlUtil.htmlEncode(topic.getLabel()));
			out.write("</a>"); //$NON-NLS-1$
			out.write("</nobr>"); //$NON-NLS-1$

			out.write("<ul>\n"); //$NON-NLS-1$

			for (int i = 0; i < topics.length; i++) {
				generateBasicTopic(topics[i], out);
			}

			out.write("</ul>\n"); //$NON-NLS-1$
		} else {
			out.write("<nobr>"); //$NON-NLS-1$
			out.write("<a "); //$NON-NLS-1$
			if (getSelectedTopicHelpHref().equals(topic.getHref())) {
				out.write("name=\"selectedItem\" "); //$NON-NLS-1$
			}
			out.write("href="+"\"" + UrlUtil.getHelpURL(topic.getHref()) +"\""+ ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			out.write("<img src='"); //$NON-NLS-1$
			out.write(imagesDirectory);
			out.write("/topic.gif' alt=\"\" border=0>&nbsp;"); //$NON-NLS-1$
			out.write(UrlUtil.htmlEncode(topic.getLabel()));
			out.write("</a>"); //$NON-NLS-1$
			out.write("</nobr>"); //$NON-NLS-1$
		}

		out.write("</li>\n"); //$NON-NLS-1$
	}
	/**
	 * @return String - help form of selected topic URL, or ""
	 */
	private String getSelectedTopicHelpHref() {
		if (topicHelpHref == null) {
			String topic = getSelectedTopic();
			if (topic == null || topic.length() == 0) {
				topicHelpHref = ""; //$NON-NLS-1$
				return topicHelpHref;
			}
			int index = topic.indexOf("/topic/"); //$NON-NLS-1$
			if (index != -1)
				topic = topic.substring(index + 6);
			index = topic.indexOf('?');
			if (index != -1)
				topic = topic.substring(0, index);
			topicHelpHref = topic;
			if (topic == null) {
				topicHelpHref = ""; //$NON-NLS-1$
			}
		}
		return topicHelpHref;
	}
	/**
	 * Obtains children topics for a given navigation element. Topics from TOCs
	 * not matching enabled activities are filtered out.
	 * 
	 * @param element ITopic or IToc
	 * @return ITopic[]
	 */
	public ITopic[] getEnabledSubtopics(Object element) {
		List topics = getEnabledSubtopicList(element);
		return (ITopic[])topics.toArray(new ITopic[topics.size()]);
	}
	/**
	 * Obtains children topics for a given navigation element. Topics from TOCs
	 * not matching enabled activities are filtered out.
	 * 
	 * @param navigationElement
	 * @return List of ITopic
	 */
	private List getEnabledSubtopicList(Object element) {
		if (element instanceof IToc && !isEnabled((IToc) element))
			return Collections.EMPTY_LIST;
		List children;
		if (element instanceof IToc) {
			children = Arrays.asList(((IToc)element).getTopics());
		}
		else if (element instanceof ITopic) {
			children = Arrays.asList(((ITopic)element).getSubtopics());
		}
		else {
			// unknown element type
			return Collections.EMPTY_LIST;
		}
		List childTopics = new ArrayList(children.size());
		for (Iterator childrenIt = children.iterator(); childrenIt.hasNext();) {
			Object c = childrenIt.next();
			if ((c instanceof ITopic)) {
				// add topic only if it will not end up being an empty
				// container
				if (((((ITopic) c).getHref() != null && ((ITopic) c)
						.getHref().length() > 0) || getEnabledSubtopicList(c).size() > 0) &&
						!UAContentFilter.isFiltered(c, HelpEvaluationContext.getContext())) {
					childTopics.add(c);
				}
			} else {
				// it is a Toc, Anchor or Link,
				// which may have children attached to it.
				childTopics.addAll(getEnabledSubtopicList(c));
			}
		}
		return childTopics;
	}
	private void generateTopicLinks(ITopic topic, Writer w, int indent) {
        String topicHref = topic.getHref();
        try {
            if (indent == 0)
                w.write("<b>"); //$NON-NLS-1$
            for (int tab = 0; tab < indent; tab++) {
                w.write("&nbsp;&nbsp;"); //$NON-NLS-1$
            }
            if (topicHref != null && topicHref.length() > 0) {
                w.write("<a href=\""); //$NON-NLS-1$
                if ('/' == topicHref.charAt(0)) {
                    w.write("topic"); //$NON-NLS-1$
                }
                w.write(topicHref);
                w.write("\">"); //$NON-NLS-1$
                w.write(UrlUtil.htmlEncode(topic.getLabel()));
                w.write("</a>"); //$NON-NLS-1$
            } else {
                w.write(UrlUtil.htmlEncode(topic.getLabel()));
            }
            w.write("<br>\n"); //$NON-NLS-1$
            if (indent == 0)
                w.write("</b>"); //$NON-NLS-1$
        } catch (IOException ioe) {
        }
        ITopic[] topics = topic.getSubtopics();
        for (int i = 0; i < topics.length; i++) {
            generateTopicLinks(topics[i], w, indent + 1);
        }
    }

    public void generateLinks(Writer out) {
        for (int i = 0; i < tocs.length; i++) {
            IToc toc = tocs[i];
            ITopic tocTopic = toc.getTopic(null);
            generateTopicLinks(tocTopic, out, 0);
            ITopic[] topics = toc.getTopics();
            for (int t = 0; t < topics.length; t++) {
                generateTopicLinks(topics[t], out, 1);
            }
        }

    }
    
    public ITopic[] getTopicPathFromRootPath(IToc toc) {
		ITopic[] topicPath;
		// Determine the topicPath from the path passed in as a parameter
		int[] rootPath = getRootPath();
		if (rootPath == null) {
			return null;
		}
		int pathLength = rootPath.length;
		topicPath = new ITopic[pathLength];
		ITopic[] children = toc.getTopics();
		for (int i = 0; i < pathLength; i++) {
			int index = rootPath[i];
			if (index < children.length) {
				topicPath[i] = children[index];
				children = topicPath[i].getSubtopics();
			} else {
				return null;  // Mismatch between expected and actual children
			}
		}
		return topicPath;
	}
    
    public ITopic[] getTopicPath() {
    	return topicPath;
    }

    public int[] getRootPath() {
    	return rootPath;
    }
    
    public String getTopicHref() {
    	return topicHref;
    }
    
    public String getNumericPath() {
    	return numericPath;
    }
    
    public boolean isExpandPath() {
        return expandPathParam != null;
    }
}

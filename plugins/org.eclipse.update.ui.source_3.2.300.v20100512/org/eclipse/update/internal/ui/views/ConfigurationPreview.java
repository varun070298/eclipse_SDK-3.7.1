/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.views;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.operations.*;

public class ConfigurationPreview implements IUpdateModelChangedListener {
	private ScrolledForm form;
	private ConfigurationView view;
	private FormToolkit toolkit;
	private FormText desc;
	private FormText taskList;
	private IPreviewTask[] tasks;
	
	public ConfigurationPreview(ConfigurationView view) {
		this.view = view;
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		model.addUpdateModelChangedListener(this);
	}
	public void dispose() {
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		model.removeUpdateModelChangedListener(this);
		toolkit.dispose();
	}

	public void objectsAdded(Object parent, Object[] children) {
	}
	public void objectsRemoved(Object parent, Object[] children) {
	}
	public void objectChanged(Object object, String property) {
	}

	public Control getControl() {
		return form;
	}

	public void createControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		TableWrapLayout layout = new TableWrapLayout();
		form.getBody().setLayout(layout);
		layout.leftMargin = 10;
		layout.rightMargin = 5;
		layout.topMargin = 10;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 20;
		IHyperlinkListener urlAction = new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				String url = (String) e.getHref();
				if (url != null)
					UpdateUI.showURL(url);
			}
		};
		IHyperlinkListener taskAction = new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				String indexArg = (String) e.getHref();
				try {
					int index = Integer.parseInt(indexArg);
					if (tasks != null)
						tasks[index].run();
				} catch (NumberFormatException ex) {
				}
			}
		};
		//taskAction.setStatusLineManager(view.getConfigurationWindow().getStatusLineManager());
		desc = toolkit.createFormText(form.getBody(), true);
		desc.setHyperlinkSettings(toolkit.getHyperlinkGroup());
		desc.addHyperlinkListener(urlAction);
		desc.setText("", false, false); //$NON-NLS-1$
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		desc.setLayoutData(td);
		taskList = toolkit.createFormText(form.getBody(), false);
		taskList.setHyperlinkSettings(toolkit.getHyperlinkGroup());
		taskList.addHyperlinkListener(taskAction); 
		taskList.setText("", false, false); //$NON-NLS-1$
		//factory.setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_ROLLOVER);
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		taskList.setLayoutData(td);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.update.ui.SiteForm"); //$NON-NLS-1$
	}
	public void setSelection(IStructuredSelection selection) {
		Object object = selection.getFirstElement();
		tasks = view.getPreviewTasks(object);
		String title = getObjectLabel(selection.size()>1?selection:object);
		form.setText(title);
		String description = getObjectDescription(selection.size()>1?selection:object);
		boolean tags = description.startsWith("<form>"); //$NON-NLS-1$
		desc.setText(description, tags, !tags);
		String taskText = getTasksText();
		taskList.setText(taskText, true, false);
		form.reflow(true);
	}
	private String getObjectLabel(Object object) {
		if (object == null)
			return ""; //$NON-NLS-1$
		TreeViewer viewer = view.getTreeViewer();
		LabelProvider provider = (LabelProvider) viewer.getLabelProvider();
		if (object instanceof IStructuredSelection) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			int counter=1;
			for (Iterator iter = ((IStructuredSelection)object).iterator();
				iter.hasNext();) {
				pw.println(counter+". "+provider.getText(iter.next())); //$NON-NLS-1$
				counter++;
			}
			return sw.toString();
		}
		return provider.getText(object);
	}
	private String getObjectDescription(Object object) {
		if (object instanceof IFeatureAdapter) {
			return getFeatureDescription((IFeatureAdapter) object);
		}
		if (object instanceof IConfiguredSiteAdapter) {
			return UpdateUIMessages.ConfigurationPreviewForm_install; 
		}
		if (object instanceof ILocalSite) {
			return UpdateUIMessages.ConfigurationPreviewForm_configDescription; 
		}
		return ""; //$NON-NLS-1$
	}
	private String getFeatureDescription(IFeatureAdapter adapter) {
		try {
			IFeature feature = adapter.getFeature(null);
			IURLEntry entry = feature.getDescription();
			if (entry != null) {
				String text = entry.getAnnotation();
				if (text != null) {
					URL url = entry.getURL();
					if (url == null)
						return text;
					else {
						String link = " <a href=\"" + url //$NON-NLS-1$
								+ "\">"+UpdateUIMessages.MoreInfoGenerator_moreInfo+"</a>";   //$NON-NLS-1$//$NON-NLS-2$
						String fullText = "<form><p>" + text + link //$NON-NLS-1$
								+ "</p></form>"; //$NON-NLS-1$ 
						return fullText;
					}
				}
			}
		} catch (CoreException e) {
		}
		return ""; //$NON-NLS-1$
	}
	private String getTasksText() {
		if (tasks == null || tasks.length == 0)
			return "<form/>"; //$NON-NLS-1$
		boolean hasEnabledTasks = false;
		for (int i = 0; i < tasks.length; i++) {
			if (tasks[i].isEnabled()) {
				hasEnabledTasks = true;
				break;
			}
		}
		if (!hasEnabledTasks)
			return "<form/>"; //$NON-NLS-1$
		StringBuffer buf = new StringBuffer();
		buf.append("<form><p><b>"); //$NON-NLS-1$
		buf.append(UpdateUIMessages.ConfigurationPreviewForm_AvailableTasks); 
		buf.append("</b></p>"); //$NON-NLS-1$
		for (int i = 0; i < tasks.length; i++) {
			IPreviewTask task = tasks[i];
			if (task.isEnabled() == false)
				continue;
			buf.append("<li style=\"text\" indent=\"0\"><a href=\"" + i + "\">" //$NON-NLS-1$ //$NON-NLS-2$
					+ task.getName() + "</a></li>"); //$NON-NLS-1$  
			buf.append("<li style=\"text\" indent=\"10\" vspace=\"false\">" //$NON-NLS-1$
					+ task.getDescription() + "</li>"); //$NON-NLS-1$ 
		}
		buf.append("</form>"); //$NON-NLS-1$
		return buf.toString();
	}
}

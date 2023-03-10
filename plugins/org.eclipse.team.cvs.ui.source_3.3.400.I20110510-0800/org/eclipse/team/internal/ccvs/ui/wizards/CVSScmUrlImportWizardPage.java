/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.net.URI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.internal.ccvs.core.filesystem.CVSURI;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.ui.IScmUrlImportWizardPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class CVSScmUrlImportWizardPage extends WizardPage implements IScmUrlImportWizardPage {
	
	private RepositoryProviderType provider;
	private ScmUrlImportDescription[] descriptions;
	private Button useHead;
	private TableViewer bundlesViewer;
	private Label counterLabel;

	private static final String CVS_PAGE_USE_HEAD = "org.eclipse.team.cvs.ui.import.page.head"; //$NON-NLS-1$

	class CVSLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return getStyledText(element).getString();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
		 */
		public void update(ViewerCell cell) {
			StyledString string = getStyledText(cell.getElement());
			cell.setText(string.getString());
			cell.setStyleRanges(string.getStyleRanges());
			cell.setImage(getImage(cell.getElement()));
			super.update(cell);
		}

		private StyledString getStyledText(Object element) {
			StyledString styledString = new StyledString();
			if (element instanceof ScmUrlImportDescription) {
				ScmUrlImportDescription description = (ScmUrlImportDescription) element;
				String project = description.getProject();
				URI scmUrl = description.getUri();
				String version = getTag(scmUrl);
				String host = getServer(scmUrl);
				styledString.append(project);
				if (version != null) {
					styledString.append(' ');
					styledString.append(version, StyledString.DECORATIONS_STYLER);
				}
				styledString.append(' ');
				styledString.append('[', StyledString.DECORATIONS_STYLER);
				styledString.append(host, StyledString.DECORATIONS_STYLER);
				styledString.append(']', StyledString.DECORATIONS_STYLER);
				return styledString;
			}
			styledString.append(element.toString());
			return styledString;
		}
	}

	/**
	 * Constructs the page.
	 */
	public CVSScmUrlImportWizardPage() {
		super("cvs", CVSUIMessages.CVSScmUrlImportWizardPage_0, null); //$NON-NLS-1$
		setDescription(CVSUIMessages.CVSScmUrlImportWizardPage_1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTUtils.createHVFillComposite(parent, SWTUtils.MARGINS_NONE, 1);
		Composite group = SWTUtils.createHFillComposite(comp, SWTUtils.MARGINS_NONE, 1);

		Button versions = SWTUtils.createRadioButton(group, CVSUIMessages.CVSScmUrlImportWizardPage_3);
		useHead = SWTUtils.createRadioButton(group, CVSUIMessages.CVSScmUrlImportWizardPage_2);
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				bundlesViewer.refresh(true);
			}
		};
		versions.addSelectionListener(listener);
		useHead.addSelectionListener(listener);

		Table table = new Table(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 225;
		table.setLayoutData(gd);

		bundlesViewer = new TableViewer(table);
		bundlesViewer.setLabelProvider(new CVSLabelProvider());
		bundlesViewer.setContentProvider(new ArrayContentProvider());
		bundlesViewer.setComparator(new ViewerComparator());
		counterLabel = new Label(comp, SWT.NONE);
		counterLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setControl(comp);
		setPageComplete(true);

		// Initialize versions versus HEAD
		IDialogSettings settings = getWizard().getDialogSettings();
		boolean useHEAD= settings != null && settings.getBoolean(CVS_PAGE_USE_HEAD);
		useHead.setSelection(useHEAD);
		versions.setSelection(!useHEAD);

		if (descriptions != null) {
			bundlesViewer.setInput(descriptions);
			updateCount();
		}

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.CVS_SCM_URL_IMPORT_PAGE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.IScmUrlImportWizardPage#finish()
	 */
	public boolean finish() {
		boolean head = false;
		if (getControl() != null) {
			head = useHead.getSelection();
			// store settings
			IDialogSettings settings = getWizard().getDialogSettings();
			if (settings != null) {
				settings.put(CVS_PAGE_USE_HEAD, head);
			}
		} else {
			// use whatever was used last time
			IDialogSettings settings = getWizard().getDialogSettings();
			if (settings != null) {
				head = settings.getBoolean(CVS_PAGE_USE_HEAD);
			}
		}

		if (head) {
			// modify tags on bundle import descriptions
			for (int i = 0; i < descriptions.length; i++) {
				URI scmUri = descriptions[i].getUri();
				descriptions[i].setUrl(removeTag(scmUri));
			}
		}
		
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.IScmUrlImportWizardPage#getSelection()
	 */
	public ScmUrlImportDescription[] getSelection() {
		return descriptions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.IScmUrlImportWizardPage#getSelection()
	 */
	public void setSelection(ScmUrlImportDescription[] descriptions) {
		this.descriptions = descriptions;
		// fill viewer
		if (bundlesViewer != null) {
			bundlesViewer.setInput(descriptions);
			updateCount();
		}
	}

	/**
	 * Updates the count of bundles that will be imported
	 */
	private void updateCount() {
		counterLabel.setText(NLS.bind(CVSUIMessages.CVSScmUrlImportWizardPage_4, new Integer(descriptions.length)));
		counterLabel.getParent().layout();
	}

	private static String getTag(URI scmUri) {
		return CVSURI.fromUri(scmUri).getTag().getName();
	}
	
	/**
	 * Remove tag attributes from the given URI reference. Results in the URI
	 * pointing to HEAD.
	 * 
	 * @param scmUri
	 *            a SCM URI reference to modify
	 * @return Returns the content of the stripped URI as a string.
	 */
	private static String removeTag(URI scmUri) {
		StringBuffer sb = new StringBuffer();
		sb.append(scmUri.getScheme()).append(':');
		String ssp = scmUri.getSchemeSpecificPart();
		int j = ssp.indexOf(';');
		if (j != -1) {
			sb.append(ssp.substring(0, j));
			String[] params = ssp.substring(j).split(";"); //$NON-NLS-1$
			for (int k = 0; k < params.length; k++) {
				// PDE way of providing tags
				if (params[k].startsWith("tag=")) { //$NON-NLS-1$
					// ignore
				} else if (params[k].startsWith("version=")) { //$NON-NLS-1$
					// ignore
				} else {
					sb.append(params[k]);
				}
			}
		} else {
			sb.append(ssp);
		}
		return sb.toString();
	}
	
	private static String getServer(URI scmUri) {
		return CVSURI.fromUri(scmUri).getRepository().getHost();
	}

	public void setProvider(RepositoryProviderType provider) {
		this.provider = provider;
	}

	public RepositoryProviderType getProvider() {
		return provider;
	}
	
}

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
package org.eclipse.update.internal.ui.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.core.ISite;
import org.eclipse.update.internal.operations.UpdateUtils;
import org.eclipse.update.internal.ui.UpdateLabelProvider;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.UpdateUIMessages;
import org.eclipse.update.internal.ui.parts.DefaultContentProvider;
import org.eclipse.update.internal.ui.parts.SWTUtil;
import org.eclipse.update.operations.IInstallFeatureOperation;

public class TargetSiteDialog extends Dialog {
	protected static final String MOST_RECEANTLY_USED_SITE_URL = "mostReceantlyUsedSiteURL"; //$NON-NLS-1$
	private TableViewer siteViewer;
	private IInstallConfiguration config;
	private Button addButton;
	private Button deleteButton;
    private IInstallFeatureOperation[] jobs;
    private WorkingCopy workingCopy;
    
    class WorkingCopy extends Observable {
    	private ArrayList sites=new ArrayList();
    	private ArrayList added=new ArrayList();
    	private IConfiguredSite targetSite;

    	public WorkingCopy() {
    		Object [] initial = config.getConfiguredSites();
    		for (int i=0; i<initial.length; i++)
    			sites.add(initial[i]);
    		for (int i=0; i<jobs.length; i++) {
    			IConfiguredSite jsite = jobs[i].getTargetSite();
    			if (targetSite==null)
    				targetSite = jsite;
    			else
    				if (!targetSite.equals(jsite))
    					targetSite = null;
    		}
    	}
    	
    	public void addSite(IConfiguredSite site) {
    		sites.add(site);
    		added.add(site);
    		setChanged();
    		notifyObservers(site);
    		clearChanged();
    	}
    	
    	public void removeSite(IConfiguredSite site) {
    		sites.remove(site);
    		added.remove(site);
    		setChanged();
    		notifyObservers(site);
    		clearChanged();
    	}
    	
    	public boolean isNewlyAdded(IConfiguredSite site) {
    		return added.contains(site);
    	}
    	
    	public void commit() {
    		// add new sites to the config
    		for (int i=0; i<added.size(); i++) {
    			config.addConfiguredSite((IConfiguredSite)added.get(i));
    		}
    		// set selected site to the job
    		for (int i=0; i<jobs.length; i++) {
    			jobs[i].setTargetSite(targetSite);
    		}
    	}
    	
    	public IConfiguredSite [] getSites() {
    		return (IConfiguredSite[])sites.toArray(new IConfiguredSite[sites.size()]);
    	}
    	
    	public IConfiguredSite [] getAddedSites() {
    		return (IConfiguredSite[])added.toArray(new IConfiguredSite[added.size()]);
    	}    	
    	
    	public IConfiguredSite getTargetSite() {
    		return targetSite;
    	}
    	
    	public void setTargetSite(IConfiguredSite site) {
    		this.targetSite = site;
    	}
    }
    
    class SitesContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return workingCopy.getSites();
		}
	}
	
	class SitesLabelProvider extends LabelProvider implements ITableLabelProvider {
		
		public Image getColumnImage(Object obj, int col) {
			UpdateLabelProvider provider = UpdateUI.getDefault().getLabelProvider();
			return provider.getLocalSiteImage((IConfiguredSite) obj);
		}
	
		public String getColumnText(Object obj, int col) {
			if (col == 0) {
				ISite site = ((IConfiguredSite) obj).getSite();
				return new File(site.getURL().getFile()).toString();
			}
			return null;
		}
	}


	/**
	 * Constructor for ReviewPage2
	 */
	public TargetSiteDialog(Shell parentShell, IInstallConfiguration config, IInstallFeatureOperation[] jobs) {
        super(parentShell);
		this.config = config;
		UpdateUI.getDefault().getLabelProvider().connect(this);
		this.jobs = jobs;
		workingCopy = new WorkingCopy();
	}

	public boolean close() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

    public Control createDialogArea(Composite parent) {
    	Composite client = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 10;
		client.setLayout(layout);
		client.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite leftPanel = new Composite(client, SWT.NULL);
		GridLayout centerLayout = new GridLayout();
		centerLayout.numColumns = 1;
		centerLayout.marginWidth = centerLayout.marginHeight = 0;
		leftPanel.setLayout(centerLayout);
		leftPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        
		Label label = new Label(leftPanel, SWT.NULL);
		label.setText(UpdateUIMessages.InstallWizard_TargetPage_siteLabel); 
		createSiteViewer(leftPanel);

		Composite rightPanel = new Composite(client, SWT.NULL);
		GridLayout rightLayout = new GridLayout();
		rightLayout.numColumns = 1;
		rightLayout.marginWidth = rightLayout.marginHeight = 0;
		rightPanel.setLayout(rightLayout);
		rightPanel.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		new Label(rightPanel, SWT.NULL);
		Composite buttonContainer = new Composite(rightPanel, SWT.NULL);
		GridLayout blayout = new GridLayout();
		blayout.marginWidth = blayout.marginHeight = 0;
		buttonContainer.setLayout(blayout);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		addButton = new Button(buttonContainer, SWT.PUSH);
		addButton.setText(UpdateUIMessages.InstallWizard_TargetPage_new); 
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addTargetLocation();
			}
		});

		addButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(addButton);
		
		deleteButton = new Button(buttonContainer, SWT.PUSH);
		deleteButton.setText(UpdateUIMessages.InstallWizard_TargetPage_delete); 
		deleteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					removeSelection();
				}
				catch (CoreException ex) {
					UpdateUI.logException(ex);
				}
			}
		});
		deleteButton.setEnabled(false);
		deleteButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(deleteButton);		

        PlatformUI.getWorkbench().getHelpSystem().setHelp(client, "org.eclipse.update.ui.MultiTargetPage2"); //$NON-NLS-1$
		Dialog.applyDialogFont(parent);
        
        siteViewer.setInput(jobs[0]);
        IConfiguredSite affinitySite = UpdateUtils.getDefaultTargetSite(config, jobs[0], true);
        if (jobs[0].getTargetSite() != null) 
        	siteViewer.setSelection(new StructuredSelection(jobs[0].getTargetSite()));
        addButton.setEnabled(affinitySite == null);
        
        return client;
	}

	private void createSiteViewer(Composite parent) {
		siteViewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		siteViewer.getTable().setLayoutData(gd);
		siteViewer.setContentProvider(new SitesContentProvider());
		siteViewer.setLabelProvider(new SitesLabelProvider());
		siteViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object obj) {
				IInstallFeatureOperation job = (IInstallFeatureOperation) siteViewer.getInput();
				return getSiteVisibility((IConfiguredSite) obj, job);
			}
		});
		siteViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ssel = (IStructuredSelection) event.getSelection();
				selectTargetSite(ssel);
				updateButtons(ssel);
			}
		});
		workingCopy.addObserver(new Observer() {
			public void update(Observable arg0, Object arg1) {
				siteViewer.refresh();
			}
		});
	}
	
    protected void okPressed() {
    	workingCopy.commit();
        super.okPressed();
    }
    
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		updateButtons((IStructuredSelection)siteViewer.getSelection());
	}

	private void updateButtons(IStructuredSelection selection) {
		deleteButton.setEnabled(canDelete(selection));
		Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton!=null)
			okButton.setEnabled(!selection.isEmpty());
	}
	
	private boolean canDelete(IStructuredSelection selection) {
		if (selection.isEmpty()) return false;
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			IConfiguredSite site = (IConfiguredSite)iter.next();
			if (!workingCopy.isNewlyAdded(site))
				return false;
		}
		return true;
	}

	private void selectTargetSite(IStructuredSelection selection) {
		IConfiguredSite site = (IConfiguredSite) selection.getFirstElement();
		if (site!=null) {
			IDialogSettings master = UpdateUI.getDefault().getDialogSettings();
			IDialogSettings section = master.getSection(MOST_RECEANTLY_USED_SITE_URL);
			if (section==null)
				section = master.addNewSection(MOST_RECEANTLY_USED_SITE_URL);
			section.put(MOST_RECEANTLY_USED_SITE_URL, site.getSite().getURL().toExternalForm());
			workingCopy.setTargetSite(site);
		}
	}

	private void addTargetLocation() {
		DirectoryDialog dd = new DirectoryDialog(getShell());
		dd.setMessage(UpdateUIMessages.InstallWizard_TargetPage_location_message); 
		String path = dd.open();
		if (path != null) {
			addConfiguredSite(getShell(), config, new File(path));
		}
	}
	
	private void removeSelection() throws CoreException {
		IStructuredSelection selection = (IStructuredSelection) siteViewer.getSelection();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			IConfiguredSite targetSite = (IConfiguredSite) iter.next();
			workingCopy.removeSite(targetSite);
		}
		siteViewer.refresh();
	}

	private IConfiguredSite addConfiguredSite(Shell shell, IInstallConfiguration config, File file) {
		try {
			IConfiguredSite csite = config.createConfiguredSite(file);
			IStatus status = csite.verifyUpdatableStatus();
			if (status.isOK())
				workingCopy.addSite(csite);
			else 
				throw new CoreException(status);
            siteViewer.setSelection(new StructuredSelection(csite));
            siteViewer.getControl().setFocus();
			return csite;
		} catch (CoreException e) {
			String title = UpdateUIMessages.InstallWizard_TargetPage_location_error_title; 
			ErrorDialog.openError(shell, title, null, e.getStatus());
			UpdateUI.logException(e,false);
			return null;
		}
	}
	
	private  boolean getSiteVisibility(IConfiguredSite site, IInstallFeatureOperation job) {
		// Do not allow installing into a non-updateable site
		if (!site.isUpdatable())
			return false;
		
		// If affinity site is known, only it should be shown
		IConfiguredSite affinitySite = UpdateUtils.getAffinitySite(config, job.getFeature());
		if (affinitySite != null) {
			// Must compare referenced sites because
			// configured sites themselves may come from 
			// different configurations
			return site.getSite().equals(affinitySite.getSite());
		}
		
		// Co-locate updates with the old feature
		if (job.getOldFeature() != null) {
			IConfiguredSite oldSite = UpdateUtils.getSiteWithFeature(config, job.getOldFeature().getVersionedIdentifier().getIdentifier());
			return (site == oldSite);
		}

		// Allow installing into any site that is updateable and there is no affinity specified
		return true;
	}
	
	public IConfiguredSite [] getAddedSites() {
		return workingCopy.getAddedSites();
	}
}

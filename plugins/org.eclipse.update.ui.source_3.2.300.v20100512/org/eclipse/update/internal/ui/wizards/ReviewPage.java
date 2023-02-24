/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ScrolledFormText;
import org.eclipse.update.core.ICategory;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.IImport;
import org.eclipse.update.core.IIncludedFeatureReference;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.ISiteFeatureReference;
import org.eclipse.update.core.IURLEntry;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.internal.core.ExtendedSite;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.internal.operations.FeatureStatus;
import org.eclipse.update.internal.operations.OperationValidator;
import org.eclipse.update.internal.operations.UpdateUtils;
import org.eclipse.update.internal.operations.OperationValidator.InternalImport;
import org.eclipse.update.internal.operations.OperationValidator.RequiredFeaturesResult;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.UpdateUIImages;
import org.eclipse.update.internal.ui.UpdateUIMessages;
import org.eclipse.update.internal.ui.model.FeatureReferenceAdapter;
import org.eclipse.update.internal.ui.model.SimpleFeatureAdapter;
import org.eclipse.update.internal.ui.model.SiteBookmark;
import org.eclipse.update.internal.ui.model.SiteCategory;
import org.eclipse.update.internal.ui.parts.DefaultContentProvider;
import org.eclipse.update.internal.ui.parts.SWTUtil;
import org.eclipse.update.internal.ui.parts.SharedLabelProvider;
import org.eclipse.update.operations.IInstallFeatureOperation;
import org.eclipse.update.operations.IUpdateModelChangedListener;
import org.eclipse.update.operations.OperationsManager;
import org.eclipse.update.search.IUpdateSearchSite;
import org.eclipse.update.search.UpdateSearchRequest;

public class ReviewPage	extends BannerPage {

	private Label label;
	private ArrayList jobs;
	private Label counterLabel;
	private IStatus validationStatus;
	private Collection problematicFeatures = new HashSet();
	// feature that was recently selected or null
	private IFeature newlySelectedFeature;
	// 
	private FeatureStatus lastDisplayedStatus;
	private PropertyDialogAction propertiesAction;
	private ScrolledFormText descLabel;
	private Button statusButton;
	private Button moreInfoButton;
	private Button propertiesButton;
	private Button selectRequiredFeaturesButton;
	private Button filterCheck;
	private Button filterOlderVersionCheck;
	private ContainmentFilter filter = new ContainmentFilter();
	private LatestVersionFilter olderVersionFilter = new LatestVersionFilter();
	private UpdateSearchRequest searchRequest;
	//private int LABEL_ORDER = 1;
	//private int VERSION_ORDER = 1;
	//private int PROVIDER_ORDER = 1;
    private ContainerCheckedTreeViewer treeViewer;
    private boolean initialized;
    private boolean isUpdateSearch;
  
    
    class TreeContentProvider extends DefaultContentProvider implements
            ITreeContentProvider {

        public Object[] getElements(Object parent) {
            return getSites();
        }

        public Object[] getChildren(final Object parent) {
            if (parent instanceof SiteBookmark) {
                SiteBookmark bookmark = (SiteBookmark) parent;
                bookmark.getSite(null); // triggers catalog creation
                Object[] children = bookmark.getCatalog(true,null);
                ArrayList nonEmptyCategories = new ArrayList(children.length);
                for (int i=0; i<children.length; i++)
                    if (hasChildren(children[i]))
                        nonEmptyCategories.add(children[i]);
                return nonEmptyCategories.toArray();
            } else if (parent instanceof SiteCategory) {
                SiteCategory category = (SiteCategory)parent;
                //return category.getChildren();
                Object[] children = category.getChildren();
                ArrayList list = new ArrayList(children.length);
                for (int i=0; i<children.length; i++) {
                    if (children[i] instanceof FeatureReferenceAdapter) {
                        try {
                            IInstallFeatureOperation job = findJob((FeatureReferenceAdapter)children[i]);
                            if (job != null)
                                list.add(job);
                        } catch (CoreException e) {
                            UpdateCore.log(e.getStatus());
                        }
                    }
                }
                return list.toArray();
            }
            return new Object[0];
        }

        public Object getParent(Object element) {
            if (element instanceof SiteCategory)
                return ((SiteCategory) element).getBookmark();
            if (element instanceof IInstallFeatureOperation) {
                IFeature f = ((IInstallFeatureOperation)element).getFeature();
                ISiteFeatureReference fr = f.getSite().getFeatureReference(f);
                ICategory[] categories = fr.getCategories();
//                if (categories != null && categories.length > 0)
//                    return categories[0];
                SiteBookmark[] sites = (SiteBookmark[])((ITreeContentProvider)treeViewer.getContentProvider()).getElements(null);
                for (int i=0; i<sites.length; i++) {
                	try {
                		if (sites[i].getSite(false, null).getURL() != f.getSite().getSiteContentProvider().getURL()) {
                		    // if the site has mirrors check if this is from the mirror that user selected
                			if (sites[i].getSite(false, null) instanceof ExtendedSite) {
                				ExtendedSite site = (ExtendedSite)sites[i].getSite(false, null);
                				IURLEntry siteMirror = site.getSelectedMirror();
                				if (siteMirror != null && siteMirror.getURL().toExternalForm().equals(f.getSite().getSiteContentProvider().getURL().toExternalForm())) { 
                					// this is the site so proceed with the loop
                				} else {
                					continue;
                				}
                			} else {
                				continue;
                			}
                		}
                	} catch (CoreException ce) {
                		return null;
                	}
                    Object[] children = sites[i].getCatalog(true, null);
                    for (int j = 0; j<children.length; j++) {
                        if (!(children[j] instanceof SiteCategory))
                            continue;
                        for (int c=0; c < categories.length; c++)
                            if (categories[c].getName().equals(((SiteCategory)children[j]).getName()))
                                return children[j];
                    }
                }
            }

            return null;
        }

        public boolean hasChildren(Object element) {
            return (element instanceof SiteBookmark || (
                    element instanceof SiteCategory && getChildren(element).length > 0));
        }

        private SiteBookmark[] getSites() {
            if (searchRequest == null)
                return new SiteBookmark[0];
            else if (searchRequest.getScope().getSearchSites() == null ||
                searchRequest.getScope().getSearchSites().length == 0) {
                // this is an update search, so see if there are any jobs first,
                // and get their sites
                if (jobs != null) {
                    ArrayList sitesList = new ArrayList(jobs.size());
                    for (int i = 0; i < jobs.size(); i++) {
                        IInstallFeatureOperation op = (IInstallFeatureOperation) jobs
                                .get(i);
                        // we need a label for the site, so try to get it from the old
                        // feature update url
                        String label = null;
                        IFeature[] existingFeatures = UpdateUtils
                                .getInstalledFeatures(op.getFeature(), true);
                        if (existingFeatures != null
                                && existingFeatures.length > 0) {
                            IURLEntry entry = op.getFeature()
                                    .getUpdateSiteEntry();
                            label = entry.getAnnotation();
                        }
                        if (label == null)
                            label = op.getFeature().getSite().getURL().toExternalForm();
                                    
                        SiteBookmark bookmark = new SiteBookmark(label,
                                op.getFeature().getSite().getURL(), false);
                        if (sitesList.contains(bookmark))
                            continue;
                        else
                            sitesList.add(bookmark);
                        
                    }
                    if (!sitesList.isEmpty())
                        return (SiteBookmark[]) sitesList
                                .toArray(new SiteBookmark[sitesList.size()]);
                }
                return new SiteBookmark[0];
            } else {
                // search for features
                IUpdateSearchSite[] sites = searchRequest.getScope().getSearchSites();
                SiteBookmark[] siteBookmarks = new SiteBookmark[sites.length];
                for (int i = 0; i < sites.length; i++)
                    siteBookmarks[i] = new SiteBookmark(sites[i].getLabel(),
                            sites[i].getURL(), false);
                return siteBookmarks;
            }
        }
    }

    class TreeLabelProvider extends SharedLabelProvider {

        public Image getImage(Object obj) {
            if (obj instanceof SiteBookmark)
                return UpdateUI.getDefault().getLabelProvider().get(
                        UpdateUIImages.DESC_SITE_OBJ);
            if (obj instanceof SiteCategory)
                return UpdateUI.getDefault().getLabelProvider().get(
                        UpdateUIImages.DESC_CATEGORY_OBJ);
            if (obj instanceof IInstallFeatureOperation) {
                IFeature feature = ((IInstallFeatureOperation) obj).getFeature();
                boolean patch = feature.isPatch();
                
                //boolean problematic=problematicFeatures.contains(feature) && treeViewer.getChecked(obj);
                boolean featureIsProblematic = isFeatureProblematic(feature);
                boolean problematic = treeViewer.getChecked(obj) && featureIsProblematic;
                
                if (!problematic && featureIsProblematic) {
                	Object parent = ((TreeContentProvider)treeViewer.getContentProvider()).getParent(obj);
                	if(parent == null)
                		return super.getImage(obj);
                	problematic = treeViewer.getChecked(parent) && !treeViewer.getGrayed(parent);
                }
                
                if (patch) {
                    return get(UpdateUIImages.DESC_EFIX_OBJ, problematic? F_ERROR : 0);
                } else {
                    return get(UpdateUIImages.DESC_FEATURE_OBJ, problematic? F_ERROR : 0);
                }
            }
            return super.getImage(obj);

        }

        public String getText(Object obj) {
            if (obj instanceof SiteBookmark) 
                return ((SiteBookmark) obj).getLabel();
            /*
            if (obj instanceof SiteCategory)
                return obj.toString();
                */
            if (obj instanceof IInstallFeatureOperation) {
                IInstallFeatureOperation job = (IInstallFeatureOperation) obj;
                IFeature feature = job.getFeature();
                return feature.getLabel() + " " + feature //$NON-NLS-1$
                            .getVersionedIdentifier()
                            .getVersion()
                            .toString();
            }
            return super.getText(obj);
        }
    }

    class ModelListener implements IUpdateModelChangedListener {
        public void objectChanged(Object object, String property) {
            treeViewer.refresh();
            checkItems();
        }

        public void objectsAdded(Object parent, Object[] children) {
            treeViewer.refresh();
            checkItems();
        }

        public void objectsRemoved(Object parent, Object[] children) {
            treeViewer.refresh();
            checkItems();
        }
        
        private void checkItems() {
            TreeItem[] items = treeViewer.getTree().getItems();
            for (int i = 0; i < items.length; i++) {
                SiteBookmark bookmark = (SiteBookmark) items[i].getData();
                treeViewer.setChecked(bookmark, bookmark.isSelected());
                String[] ignoredCats = bookmark.getIgnoredCategories();
                treeViewer.setGrayed(bookmark, ignoredCats.length > 0
                        && bookmark.isSelected());
            }
        }
    }

	class ContainmentFilter extends ViewerFilter {
		
		private IInstallFeatureOperation[] selectedJobs;
		
		public boolean select(Viewer viewer, Object parent, Object element) {
            if (element instanceof IInstallFeatureOperation) {
                return !isContained((IInstallFeatureOperation) element) || isSelected( selectedJobs, (IInstallFeatureOperation)element);
            } else if ( (element instanceof SiteCategory) || (element instanceof SiteBookmark)){
            	Object[] children = ((ITreeContentProvider)((ContainerCheckedTreeViewer)viewer).getContentProvider()).getChildren(element);
            	for ( int i = 0; i < children.length; i++) {
            		if (select(viewer, element, children[i])) {
            			return true;
            		}
            	}
            	return false;
            }
        
            return true;
		}
		
		private boolean isContained(IInstallFeatureOperation job) {
			VersionedIdentifier vid = job.getFeature().getVersionedIdentifier();

			for (int i = 0; i < jobs.size(); i++) {
				IInstallFeatureOperation candidate = (IInstallFeatureOperation) jobs.get(i);
				if (candidate.equals(job))
					continue;
				IFeature feature = candidate.getFeature();
				if (includes(feature, vid,null))
					return true;
			}
			return false;
		}
		private boolean includes(IFeature feature, VersionedIdentifier vid, ArrayList cycleCandidates) {
			try {
				if (cycleCandidates == null)
					cycleCandidates = new ArrayList();
				if (cycleCandidates.contains(feature))
					throw Utilities.newCoreException(NLS.bind(UpdateUIMessages.InstallWizard_ReviewPage_cycle, feature.getVersionedIdentifier().toString()), null);
				else
					cycleCandidates.add(feature);
				IFeatureReference[] irefs =
					feature.getIncludedFeatureReferences();
				for (int i = 0; i < irefs.length; i++) {
					IFeatureReference iref = irefs[i];
					IFeature ifeature = UpdateUtils.getIncludedFeature(feature, iref);
					VersionedIdentifier ivid =
						ifeature.getVersionedIdentifier();
					if (ivid.equals(vid))
						return true;
					if (includes(ifeature, vid, cycleCandidates))
						return true;
				}
				return false;
			} catch (CoreException e) {
				return false;
			} finally {
				// after this feature has been DFS-ed, it is no longer a cycle candidate
				cycleCandidates.remove(feature);
			}
		}


		public IInstallFeatureOperation[] getSelectedJobs() {
			return selectedJobs;
		}
		

		public void setSelectedJobs(IInstallFeatureOperation[] selectedJobs) {
			this.selectedJobs = selectedJobs;
		}

	
	}

	class LatestVersionFilter extends ViewerFilter {
		
		private IInstallFeatureOperation[] selectedJobs;
		
		public boolean select(Viewer viewer, Object parent, Object element) {
						
            if (element instanceof IInstallFeatureOperation) {
                return isLatestVersion((IInstallFeatureOperation) element) || isSelected( selectedJobs, (IInstallFeatureOperation)element);
            } else if ( (element instanceof SiteCategory) || (element instanceof SiteBookmark)){
            	Object[] children = ((ITreeContentProvider)((ContainerCheckedTreeViewer)viewer).getContentProvider()).getChildren(element);
            	for ( int i = 0; i < children.length; i++) {
            		if (select(viewer, element, children[i])) {
            			return true;
            		}
            	}
                return false;
            }
            
            return true;
		}
		
		private boolean isLatestVersion(IInstallFeatureOperation job) {
			IFeature feature = job.getFeature();
			for (int i = 0; i < jobs.size(); i++) {
				IInstallFeatureOperation candidateJob = (IInstallFeatureOperation) jobs.get(i);
				if (candidateJob.equals(job))
					continue;
				IFeature candidate = candidateJob.getFeature();
				if (feature.getSite() != job.getFeature().getSite())
					continue;
				if (!feature.getVersionedIdentifier().getIdentifier().equals(candidate.getVersionedIdentifier().getIdentifier()))
					continue;
				if (!feature.getVersionedIdentifier().getVersion().isGreaterOrEqualTo(candidate.getVersionedIdentifier().getVersion()))
					return false;
			}
			return true;
		}
		
		public IInstallFeatureOperation[] getSelectedJobs() {
			return selectedJobs;
		}
		
		public void setSelectedJobs(IInstallFeatureOperation[] selectedJobs) {
			this.selectedJobs = selectedJobs;
		}
		
		
	}
	
	class FeaturePropertyDialogAction extends PropertyDialogAction {
		private IStructuredSelection selection;

		public FeaturePropertyDialogAction(
			Shell shell,
			ISelectionProvider provider) {
			super(shell, provider);
		}

		public IStructuredSelection getStructuredSelection() {
			return selection;
		}

		public void selectionChanged(IStructuredSelection selection) {
			this.selection = selection;
		}

	}
	/**
	 * Constructor for ReviewPage2
	 */
	public ReviewPage(boolean isUpdateSearch, UpdateSearchRequest searchRequest, ArrayList jobs) {
		super("Review"); //$NON-NLS-1$
        this.isUpdateSearch = isUpdateSearch;
        this.jobs = jobs;
        if (this.jobs==null) this.jobs = new ArrayList();
        this.searchRequest = searchRequest;
        
		setTitle(UpdateUIMessages.InstallWizard_ReviewPage_title); 
		setDescription(UpdateUIMessages.InstallWizard_ReviewPage_desc); 
		UpdateUI.getDefault().getLabelProvider().connect(this);
		setBannerVisible(false);
	}

	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		
		// when searching for updates, only nested patches can be shown.
		// when searching for features, features and patches can be shown
		String filterText = filterCheck.getText();
		String filterFeatures = UpdateUIMessages.InstallWizard_ReviewPage_filterFeatures; 
		String filterPatches = UpdateUIMessages.InstallWizard_ReviewPage_filterPatches; 

		if (isUpdateSearch && filterText.equals(filterFeatures))
			filterCheck.setText(filterPatches);
		else if ( !isUpdateSearch && filterText.equals(filterPatches))
			filterCheck.setText(filterFeatures);
		
		if (visible && !initialized) {
            initialized = true;
//			jobs.clear();

//			setDescription(UpdateUI.getString("InstallWizard.ReviewPage.searching")); //$NON-NLS-1$;
//			label.setText(UpdateUI.getString("")); //$NON-NLS-1$

			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
//					searchRunner.runSearch();
					performPostSearchProcessing();
				}
			});
		}
	}

	private void performPostSearchProcessing() {
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				if (treeViewer != null) {
//                    treeViewer.refresh();
//                    treeViewer.getTree().layout(true);
					if (isUpdateSearch) {
						selectTrueUpdates();
					}
				}
				pageChanged();
				
				int totalCount = jobs != null ? jobs.size(): 0;
				if(totalCount >0) {
					setDescription(UpdateUIMessages.InstallWizard_ReviewPage_desc); 
					label.setText(UpdateUIMessages.InstallWizard_ReviewPage_label); 
				} else {
					if (isUpdateSearch)
						setDescription(UpdateUIMessages.InstallWizard_ReviewPage_zeroUpdates); 
					else
						setDescription(UpdateUIMessages.InstallWizard_ReviewPage_zeroFeatures); 
					label.setText(""); //$NON-NLS-1$
				}
			}
		});
	}
	
	private void selectTrueUpdates() {
		ArrayList trueUpdates = new ArrayList();
		for (int i=0; i<jobs.size(); i++) {
			IInstallFeatureOperation job = (IInstallFeatureOperation)jobs.get(i);
			if (!UpdateUtils.isPatch(job.getFeature()))
				trueUpdates.add(job);
		}
		treeViewer.setCheckedElements(trueUpdates.toArray()); 
		validateSelection(new NullProgressMonitor());
	}	

	/**
	 * @see DialogPage#createControl(Composite)
	 */
	public Control createContents(Composite parent) {
		Composite client = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		client.setLayout(layout);
		label = new Label(client, SWT.NULL);
		label.setText(UpdateUIMessages.InstallWizard_ReviewPage_label); 
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

        createTreeViewer(client);

		Composite comp = new Composite(client, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
				
		Composite buttonContainer = new Composite(comp, SWT.NULL);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0; //30?
		buttonContainer.setLayout(layout);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		Button button = new Button(buttonContainer, SWT.PUSH);
		button.setText(UpdateUIMessages.InstallWizard_ReviewPage_deselectAll); 
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleDeselectAll();
			}
		});

		moreInfoButton = new Button(buttonContainer, SWT.PUSH);
		moreInfoButton.setText(UpdateUIMessages.InstallWizard_ReviewPage_moreInfo); 
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		moreInfoButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(moreInfoButton);
		moreInfoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleMoreInfo();
			}
		});
		moreInfoButton.setEnabled(false);
		
		
		propertiesButton = new Button(buttonContainer, SWT.PUSH);
		propertiesButton.setText(UpdateUIMessages.InstallWizard_ReviewPage_properties); 
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		propertiesButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(propertiesButton);
		propertiesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleProperties();
			}
		});
		propertiesButton.setEnabled(false);

		selectRequiredFeaturesButton = new Button(buttonContainer, SWT.PUSH);
		selectRequiredFeaturesButton.setText(UpdateUIMessages.InstallWizard_ReviewPage_selectRequired);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_BEGINNING);
		selectRequiredFeaturesButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(selectRequiredFeaturesButton);
		selectRequiredFeaturesButton
				.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						BusyIndicator.showWhile(e.display, new Runnable() {
							public void run() {
								Object[] elements = treeViewer.getExpandedElements();
								treeViewer.expandAll();
								treeViewer.setExpandedElements(elements);
								selectRequiredFeatures();
								updateItemCount();
							}
						});
					}
				});

		statusButton = new Button(buttonContainer, SWT.PUSH);
		statusButton.setText(UpdateUIMessages.InstallWizard_ReviewPage_showStatus); 
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		statusButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(statusButton);
		statusButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showStatus();
			}
		});

		//new Label(client, SWT.NULL);

		counterLabel = new Label(client, SWT.NULL);
		gd = new GridData();
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);

		filterOlderVersionCheck = new Button(client, SWT.CHECK);
		filterOlderVersionCheck.setText(UpdateUIMessages.InstallWizard_ReviewPage_filterOlderFeatures); 
		filterOlderVersionCheck.setSelection(true);
		treeViewer.addFilter(olderVersionFilter);
		filterOlderVersionCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				IInstallFeatureOperation[] jobs = getSelectedJobs();
				
				if (filterOlderVersionCheck.getSelection())
					treeViewer.addFilter(olderVersionFilter);
				else 
					treeViewer.removeFilter(olderVersionFilter);
				
				olderVersionFilter.setSelectedJobs(jobs);				
				pageChanged(jobs);
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		filterOlderVersionCheck.setLayoutData(gd);
		
		filterCheck = new Button(client, SWT.CHECK);
		filterCheck.setText(UpdateUIMessages.InstallWizard_ReviewPage_filterFeatures); 
		filterCheck.setSelection(false);
		//tableViewer.addFilter(filter);
		filterCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				
				IInstallFeatureOperation[] jobs = getSelectedJobs();
				
				if (filterCheck.getSelection()) {
					// make sure model is local
					if (downloadIncludedFeatures()) {
						treeViewer.addFilter(filter);
					} else {
						filterCheck.setSelection(false);
					}
				} else {
					treeViewer.removeFilter(filter);
				}
				
				filter.setSelectedJobs(jobs);
				
				pageChanged(jobs);
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		filterCheck.setLayoutData(gd);
		
		pageChanged();

		PlatformUI.getWorkbench().getHelpSystem().setHelp(client, "org.eclipse.update.ui.MultiReviewPage2"); //$NON-NLS-1$

		Dialog.applyDialogFont(parent);

		return client;
	}

    private void createTreeViewer(final Composite parent) {
        SashForm sform = new SashForm(parent, SWT.VERTICAL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 250;
        gd.heightHint =100;
        sform.setLayoutData(gd);
        
        treeViewer = new ContainerCheckedTreeViewer(sform, SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.BORDER);
        treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        treeViewer.setContentProvider(new TreeContentProvider());
        treeViewer.setLabelProvider(new TreeLabelProvider());
        treeViewer.setInput(UpdateUI.getDefault().getUpdateModel());

        treeViewer
            .addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent e) {
                handleSelectionChanged((IStructuredSelection) e.getSelection());
            }
        });
        
        treeViewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
    			/*
				 * validateSelection(); Object site =
				 * getSite(event.getElement()); ArrayList descendants = new
				 * ArrayList(); collectDescendants(site, descendants); Object[]
				 * nodes = new Object[descendants.size()]; for (int i = 0; i <
				 * nodes.length; i++) nodes[i] = descendants.get(i);
				 * treeViewer.update(nodes, null); updateItemCount();
				 */
				try {
					getContainer().run(true, true,
							getCheckStateOperation(event, parent.getDisplay()));
					getContainer().updateButtons();
					updateStatusButton();
				} catch (InvocationTargetException e) {
					UpdateUI.logException(e);
				} catch (InterruptedException e) {
					UpdateUI.logException(e);
				}
            }
        });
        
      descLabel = new ScrolledFormText(sform, true);
      descLabel.setText(""); //$NON-NLS-1$
      descLabel.setBackground(parent.getBackground());
      HyperlinkSettings settings = new HyperlinkSettings(parent.getDisplay());
      descLabel.getFormText().setHyperlinkSettings(settings);
      descLabel.getFormText().addHyperlinkListener(new HyperlinkAdapter() {
    	  public void linkActivated(HyperlinkEvent e) {
    		  Object href = e.getHref();
    		  if (href==null)
    			  return;
    		  try {
    			  URL url = new URL(href.toString());
    			  PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
    		  }
    		  catch (PartInitException ex) {
    			  UpdateUI.logException(ex);
    		  }
    		  catch (MalformedURLException ex) {
    			  UpdateUI.logException(ex);
    		  }
    	  }
      });
      
      gd = new GridData(SWT.FILL, SWT.FILL, true, true);
      gd.horizontalSpan = 1;
      descLabel.setLayoutData(gd);
      
      sform.setWeights(new int[] {10, 2});
    }
    
	private IRunnableWithProgress getCheckStateOperation(
			final CheckStateChangedEvent event, final Display display) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				monitor.beginTask(UpdateUIMessages.ReviewPage_validating,
						IProgressMonitor.UNKNOWN);
				validateSelection(monitor);
				if (monitor.isCanceled()) {
					undoStateChange(event);
					monitor.done();
					return;
				}
				Object site = getSite(event.getElement());
				ArrayList descendants = new ArrayList();
				collectDescendants(site, descendants, monitor);
				final Object[] nodes = new Object[descendants.size()];
				if (monitor.isCanceled()) {
					undoStateChange(event);
					monitor.done();
					return;
				}
				for (int i = 0; i < nodes.length; i++)
					nodes[i] = descendants.get(i);
				display.syncExec(new Runnable() {
					public void run() {
						treeViewer.update(nodes, null);
						updateItemCount();
					}
				});
				monitor.done();
			}
		};
	}
	
	private void undoStateChange(final CheckStateChangedEvent e) {
		treeViewer.getControl().getDisplay().syncExec(new Runnable() {
			public void run() {
				treeViewer.setChecked(e.getElement(), !e.getChecked());
			}
		});
	}
    
    private void handleSelectionChanged(IStructuredSelection ssel) {
        
        Object item = ssel.getFirstElement();
        String description = null;
        if (item instanceof SiteBookmark) {
            description = ((SiteBookmark)item).getDescription();
        } else if (item instanceof SiteCategory) {
            IURLEntry descEntry = ((SiteCategory)item).getCategory().getDescription();
            if (descEntry != null)
                description = descEntry.getAnnotation();
        } else if (item instanceof IInstallFeatureOperation) {
            jobSelected(ssel);
            return;
        }

        if (description == null)
            description = ""; //$NON-NLS-1$
        //descLabel.setText(UpdateManagerUtils.getWritableXMLString(description), false, true);
        updateDescription(description);
        propertiesButton.setEnabled(false);
        moreInfoButton.setEnabled(false);
    }

/*	
	private void fillContextMenu(IMenuManager manager) {
		if (treeViewer.getSelection().isEmpty()) return;
		Action action = new Action(UpdateUIMessages.InstallWizard_ReviewPage_prop) { 
			public void run() {
				handleProperties();
			}
		};
		manager.add(action);
	}
*/

	private void jobSelected(IStructuredSelection selection) {
		IInstallFeatureOperation job = (IInstallFeatureOperation) selection.getFirstElement();
		IFeature feature = job != null ? job.getFeature() : null;
		IURLEntry descEntry = feature != null ? feature.getDescription() : null;
		String desc = null;
		if (descEntry != null)
			desc = descEntry.getAnnotation();
		if (desc == null)
			desc = ""; //$NON-NLS-1$
		//descLabel.setText(UpdateManagerUtils.getWritableXMLString(desc));
		updateDescription(desc);
		propertiesButton.setEnabled(feature != null);
		moreInfoButton.setEnabled(job != null && getMoreInfoURL(job) != null);
	}
	
	private void updateDescription(String text) {
		descLabel.getFormText().setText(UpdateManagerUtils.getWritableXMLString(text), false, true);
		descLabel.reflow(true);
	}
	
	private void pageChanged() {
		pageChanged(this.getSelectedJobs());
	}
	
	private void pageChanged( IInstallFeatureOperation[] jobsSelected) {
	
		if (jobsSelected.length == 0) {
			lastDisplayedStatus = null;
			setErrorMessage(null);
			setPageComplete(false);
			setValidationStatus(null);
			problematicFeatures.clear();
		}
		treeViewer.setCheckedElements(jobsSelected);
		//validateSelection();
        treeViewer.refresh();
        treeViewer.setCheckedElements(jobsSelected);
        updateItemCount();
	}
	
	private void setValidationStatus(IStatus newValidationStatus) {
		this.validationStatus = newValidationStatus;
		updateStatusButton();
	}
	
	private void updateStatusButton() {
		statusButton.getDisplay().syncExec(new Runnable() {
			public void run() {
				boolean newState = validationStatus != null && validationStatus.getSeverity() != IStatus.OK;
				statusButton.setEnabled(newState);
			}
		});
	}
	
	private void updateItemCount() {
		updateItemCount(-1, -1);
	}
	
	private int getSelectedJobsUniqueCount() {
		Object[] checkedElements = getSelectedJobs();
		Set set = new HashSet();
		for (int i=0; i<checkedElements.length; i++) {
			IInstallFeatureOperation job = (IInstallFeatureOperation)checkedElements[i];
			IFeature feature = job.getFeature();
			if (set.contains(feature))
				continue;
			set.add(feature);
		}
		return set.size();
	}

	private void updateItemCount(int checkedCount, int totalCount) {
		if (checkedCount == -1) {
			checkedCount = getSelectedJobsUniqueCount();
		}
		if (totalCount == -1) {
			totalCount = jobs.size();
		}
		String total = "" + totalCount; //$NON-NLS-1$
		String selected = "" + checkedCount; //$NON-NLS-1$
		counterLabel.setText(
			NLS.bind(UpdateUIMessages.InstallWizard_ReviewPage_counter, (new String[] { selected, total })));	
        counterLabel.getParent().layout();
	}

//	private void handleSelectAll(boolean select) {
//		treeViewer.setAllChecked(select);
////		 make sure model is local (download using progress monitor from container)
//		downloadIncludedFeatures(); 
//			
//		treeViewer.getControl().getDisplay().asyncExec(new Runnable() {
//			public void run() {
//				pageChanged();
//			}
//		});
//	}

//  private void handleSelectAll(boolean select) {
//  treeViewer.setAllChecked(select);
////     make sure model is local (download using progress monitor from container)
//  downloadIncludedFeatures(); 
//      
//  treeViewer.getControl().getDisplay().asyncExec(new Runnable() {
//      public void run() {
//          pageChanged();
//      }
//  });
//}

   private void handleDeselectAll() {
        //treeViewer.setCheckedElements(new Object[0]);
         IInstallFeatureOperation[] selectedJobs = getSelectedJobs();
         for( int i = 0; i < selectedJobs.length; i++)
        	 treeViewer.setChecked( selectedJobs[i], false);
        // make sure model is local (download using progress monitor from
        // container)
//        downloadIncludedFeatures();

        treeViewer.getControl().getDisplay().asyncExec(new Runnable() {
            public void run() {
                pageChanged();
            }
        });
}
    
	private void handleProperties() {
		final IStructuredSelection selection =
			(IStructuredSelection) treeViewer.getSelection();

		final IInstallFeatureOperation job =
			(IInstallFeatureOperation) selection.getFirstElement();
		if (propertiesAction == null) {
			propertiesAction =
				new FeaturePropertyDialogAction(getShell(), treeViewer);
		}

		BusyIndicator
			.showWhile(treeViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				SimpleFeatureAdapter adapter =
					new SimpleFeatureAdapter(job.getFeature());
				propertiesAction.selectionChanged(
					new StructuredSelection(adapter));
				propertiesAction.run();
			}
		});
	}

	private String getMoreInfoURL(IInstallFeatureOperation job) {
		IURLEntry desc = job.getFeature().getDescription();
		if (desc != null) {
			URL url = desc.getURL();
			return (url == null) ? null : url.toString();
		}
		return null;
	}

	private void handleMoreInfo() {
		IStructuredSelection selection =
			(IStructuredSelection) treeViewer.getSelection();
		final IInstallFeatureOperation selectedJob =
			(IInstallFeatureOperation) selection.getFirstElement();
		BusyIndicator
			.showWhile(treeViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				String urlName = getMoreInfoURL(selectedJob);
				UpdateUI.showURL(urlName);
			}
		});
	}

	private IStatus selectRequiredFeatures() {
		
		IInstallFeatureOperation[] jobs = getSelectedJobs();
		RequiredFeaturesResult requiredFeaturesResult = ((OperationValidator)OperationsManager
				.getValidator()).getRequiredFeatures(jobs);
		setValidationStatus(requiredFeaturesResult.getStatus());
		Set requiredFeatures = requiredFeaturesResult.getRequiredFeatures();
		problematicFeatures.clear();
		
		Iterator requiredFeaturesIterator = requiredFeatures.iterator();
		ArrayList toBeInstalled = new ArrayList();
		
		while (requiredFeaturesIterator.hasNext()) {
			IImport requiredFeature = ((InternalImport)requiredFeaturesIterator.next()).getImport();

			IInstallFeatureOperation currentFeatureSelected = null;
			TreeItem[] items = treeViewer.getTree().getItems();
			for (int i = 0; i < items.length; i++) {
				TreeItem[] siteRootContent = items[i].getItems();
				for (int j = 0; j < siteRootContent.length; j++) {
					if (siteRootContent[j].getData() instanceof SiteCategory) {
						
						if ( !treeViewer.getChecked(siteRootContent[j].getData())) {
							// this category has not been checked at all so we have to create its features
							treeViewer.createChildren(siteRootContent[j]);
						}
						TreeItem[] features = siteRootContent[j].getItems();
						if ((features.length > 0) && (features[0].getData() == null)) {
							// this category has been checked but not visited yet so restore the features in it
							treeViewer.createChildren(siteRootContent[j]);
							treeViewer.updateChildrenItems(siteRootContent[j]);
							features = siteRootContent[j].getItems();
						}
						
						for (int k = 0; k < features.length; k++) {
							currentFeatureSelected = decideOnFeatureSelection(
									requiredFeature,
									(IInstallFeatureOperation) features[k]
											.getData(), currentFeatureSelected);
						}
					} else if (siteRootContent[j].getData() instanceof IInstallFeatureOperation) {
						currentFeatureSelected = decideOnFeatureSelection(
								requiredFeature,
								(IInstallFeatureOperation) siteRootContent[j]
										.getData(), currentFeatureSelected);
					}
				}
			}

			if (currentFeatureSelected != null)
				toBeInstalled.add(currentFeatureSelected);
		}

		if (!toBeInstalled.isEmpty()) {
			Iterator toBeInstalledIterator = toBeInstalled.iterator();
			while (toBeInstalledIterator.hasNext()) {
				IInstallFeatureOperation current = (IInstallFeatureOperation)toBeInstalledIterator.next();
				treeViewer.setChecked(current, true);			
			}
			return selectRequiredFeatures();
		} else {
			problematicFeatures.clear();
			if (validationStatus != null) {
				IStatus[] status = validationStatus.getChildren();
				for (int i = 0; i < status.length; i++) {
					IStatus singleStatus = status[i];
					if (isSpecificStatus(singleStatus)) {
						IFeature f = ((FeatureStatus) singleStatus)
								.getFeature();
						problematicFeatures.add(f);
					}
				}
			}

			setPageComplete(validationStatus == null
					|| validationStatus.getSeverity() == IStatus.WARNING);

			lastDisplayedStatus = null;
			updateWizardMessage();
			
			treeViewer.update(getSelectedJobs(), null);
			return validationStatus;
		}
	}

	public IInstallFeatureOperation[] getSelectedJobs() {      
        Object[] selected = treeViewer.getCheckedElements();
        ArrayList selectedJobs = new ArrayList(selected.length);
        for (int i=0; i<selected.length; i++)
            if (selected[i] instanceof IInstallFeatureOperation)
                selectedJobs.add(selected[i]);
        return (IInstallFeatureOperation[])selectedJobs.toArray(new IInstallFeatureOperation[selectedJobs.size()]);
	}
	
	public void validateSelection(IProgressMonitor monitor) {
		IInstallFeatureOperation[] jobs;

		final IInstallFeatureOperation[][] bag = new IInstallFeatureOperation[1][];
		treeViewer.getControl().getDisplay().syncExec(new Runnable() {
			public void run() {
				bag[0] = getSelectedJobs();
			}
		});
		if (monitor.isCanceled()) return;
		jobs = bag[0];
		setValidationStatus(OperationsManager.getValidator()
				.validatePendingChanges(jobs));
		problematicFeatures.clear();
		if (monitor.isCanceled()) return;
		if (validationStatus != null) {
			IStatus[] status = validationStatus.getChildren();
			for (int i = 0; i < status.length; i++) {
				IStatus singleStatus = status[i];
				if (isSpecificStatus(singleStatus)) {
					IFeature f = ((FeatureStatus) singleStatus).getFeature();
					problematicFeatures.add(f);
				}
			}
		}
		if (monitor.isCanceled())
				return;
		treeViewer.getControl().getDisplay().syncExec(new Runnable() {
			public void run() {
				setPageComplete(validationStatus == null
						|| validationStatus.getSeverity() == IStatus.WARNING);
/*
				statusButton.setEnabled(validationStatus != null
						&& validationStatus.getSeverity() != IStatus.OK);
						*/

				updateWizardMessage();
			}
		});
	}

	private void showStatus() {
		if (validationStatus != null) {
			new StatusDialog().open();
		}

	}
	/**
	 * Check whether status is relevant to show for
	 * a specific feature or is a other problem
	 * @param status
	 * @return true if status is FeatureStatus with
	 * specified feature and certain error codes
	 */
	private boolean isSpecificStatus(IStatus status){
		if(!(status instanceof FeatureStatus)){
			return false;
		}
		if(status.getSeverity()!=IStatus.ERROR){
			return false;
		}
		FeatureStatus featureStatus = (FeatureStatus) status;
		if(featureStatus.getFeature()==null){
			return false;
		}
		return 0!= (featureStatus.getCode()
				& FeatureStatus.CODE_CYCLE
				+ FeatureStatus.CODE_ENVIRONMENT
				+ FeatureStatus.CODE_EXCLUSIVE
				+ FeatureStatus.CODE_OPTIONAL_CHILD
				+ FeatureStatus.CODE_PREREQ_FEATURE
				+ FeatureStatus.CODE_PREREQ_PLUGIN);
	}
	/**
	 * Update status in the wizard status area
	 */
	private void updateWizardMessage() {
		
		if (validationStatus == null) {
			lastDisplayedStatus=null;
			setErrorMessage(null);
		} else if (validationStatus.getSeverity() == IStatus.WARNING) {
			lastDisplayedStatus=null;
			setErrorMessage(null);
			setMessage(validationStatus.getMessage(), IMessageProvider.WARNING);
		} else {
			// 1.  Feature selected, creating a problem for it, show status for it
			if(newlySelectedFeature !=null){
				IStatus[] status = validationStatus.getChildren();
				for(int s =0; s< status.length; s++){
					if(isSpecificStatus(status[s])){
						FeatureStatus featureStatus = (FeatureStatus)status[s];
						if(newlySelectedFeature.equals(featureStatus.getFeature())){
							lastDisplayedStatus=featureStatus;
							setErrorMessage(featureStatus.getMessage());
							return;
						}
					}
				}
			}
			
			// 2.  show old status if possible (it is still valid)
			if(lastDisplayedStatus !=null){
				IStatus[] status = validationStatus.getChildren();
				for(int i=0; i<status.length; i++){
					if(lastDisplayedStatus.equals(status[i])){
						//lastDisplayedStatus=lastDisplayedStatus;
						//setErrorMessage(status[i].getMessage());
						return;
					}
				}
				lastDisplayedStatus = null;
			}
			
			// 3.  pick the first problem that is specific to some feature
			IStatus[] status = validationStatus.getChildren();
			for(int s =0; s< status.length; s++){
				if(isSpecificStatus(status[s])){
					lastDisplayedStatus = (FeatureStatus)status[s];
					setErrorMessage(status[s].getMessage());
					return;
				}
			}
				
			// 4.  display the first problem (no problems specify a feature)
			if(status.length>0){
				IStatus singleStatus=status[0];
				setErrorMessage(singleStatus.getMessage());
			}else{
			// 5. not multi or empty multi status
				setErrorMessage(UpdateUIMessages.InstallWizard_ReviewPage_invalid_long); 
			}
		}
	}

	class StatusDialog extends ErrorDialog {
//		Button detailsButton;
		public StatusDialog() {
			super(getContainer().getShell(), UpdateUIMessages.InstallWizard_ReviewPage_invalid_short, null, 
					validationStatus, IStatus.OK | IStatus.INFO
							| IStatus.WARNING | IStatus.ERROR);
		}
//		protected Button createButton(
//				Composite parent,
//				int id,
//				String label,
//				boolean defaultButton) {
//			Button b = super.createButton(parent, id, label, defaultButton);
//			if(IDialogConstants.DETAILS_ID == id){
//				detailsButton = b;
//			}
//			return b;
//		}
		public void create() {
			super.create();
			buttonPressed(IDialogConstants.DETAILS_ID);
//			if(detailsButton!=null){
//				detailsButton.dispose();
//			}
		}
	}

	/**
	 * @return true, if completed, false if canceled by the user
	 */
	private boolean downloadIncludedFeatures() {
		try {
			Downloader downloader = new Downloader(jobs);
			getContainer().run(true, true, downloader);
			return !downloader.isCanceled();
		} catch (InvocationTargetException ite) {
		} catch (InterruptedException ie) {
		}
		return true;
	}
	/**
	 * Runnable to resolve included feature references.
	 */
	class Downloader implements IRunnableWithProgress {
		boolean canceled = false;
		/**
		 * List of IInstallFeatureOperation
		 */
		ArrayList operations;
		public Downloader(ArrayList installOperations) {
			operations = installOperations;
		}
		public boolean isCanceled() {
			return canceled;
		}
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			for (int i = 0; i < operations.size(); i++) {
				IInstallFeatureOperation candidate = (IInstallFeatureOperation) operations
						.get(i);
				IFeature feature = candidate.getFeature();
				try {
					IFeatureReference[] irefs = feature
							.getRawIncludedFeatureReferences();
					for (int f = 0; f < irefs.length; f++) {
						if (monitor.isCanceled()) {
							canceled = true;
							return;
						}
						IFeatureReference iref = irefs[f];
						iref.getFeature(monitor);
					}
				} catch (CoreException e) {
				}
			}
			if (monitor.isCanceled()) {
				canceled = true;
			}
		}
	}
    
    private IInstallFeatureOperation findJob(FeatureReferenceAdapter feature)
            throws CoreException {
        if (jobs == null)
            return null;
        for (int i = 0; i < jobs.size(); i++)
            if (((IInstallFeatureOperation) jobs.get(i)).getFeature()
                    .getVersionedIdentifier().equals(feature.getFeatureReference()
                    .getVersionedIdentifier()))
                return (IInstallFeatureOperation) jobs.get(i);

        return null;
    }
    
    private Object getSite(Object object) {
        ITreeContentProvider provider = (ITreeContentProvider)treeViewer.getContentProvider();
        while (object != null && !(object instanceof SiteBookmark)) {
            object = provider.getParent(object);
        }
        return object;
    }
    
	private void collectDescendants(Object root, ArrayList list,
			IProgressMonitor monitor) {
		ITreeContentProvider provider = (ITreeContentProvider) treeViewer
				.getContentProvider();
		Object[] children = provider.getChildren(root);
		if (children != null && children.length > 0)
			for (int i = 0; i < children.length; i++) {
				if (monitor.isCanceled())
					return;
				list.add(children[i]);
				collectDescendants(children[i], list, monitor);
			}
	}

	
	public boolean isFeatureGood(IImport requiredFeature, IFeature feature) {
		return isFeatureGood(requiredFeature, feature, new ArrayList());
	}
	
	public boolean isFeatureGood(IImport prereq, IFeature feature, List visitedFeatures) {

		if (prereq.getKind() == IImport.KIND_FEATURE) { 
			if ((!prereq.getVersionedIdentifier().getIdentifier().equals(
					feature.getVersionedIdentifier().getIdentifier()))) {
				IIncludedFeatureReference[] iifr = null;
				try {
					iifr = feature.getIncludedFeatureReferences();
				} catch (CoreException e) {
					UpdateUI.logException(e);
					//	if we can not get included features then they can not satisfy requirement, so just ignore them
					return false;
				}
				if (iifr == null) {
					return false;
				}
				
				for(int i = 0; i < iifr.length; i++) {
					IFeature current;
					try {
						current = UpdateUtils.getIncludedFeature(feature, iifr[i]);
					} catch (CoreException e) {
						// if we can not get feature then it can not satisfy requirement, so just ignore it
						UpdateUI.logException(e);
						continue;
					}
					if (!visitedFeatures.contains(current)) {
						visitedFeatures.add(current);
						if (isFeatureGood(prereq, current, visitedFeatures)) {
							return true;
						}
					}
				}
				
				return false;
			}

			int rule = (prereq.getRule() != IImport.RULE_NONE) ? prereq.getRule() : IImport.RULE_COMPATIBLE;

			switch (rule) {
			case IImport.RULE_PERFECT: return feature.getVersionedIdentifier().getVersion().isPerfect(
								prereq.getVersionedIdentifier()
								.getVersion());
			case IImport.RULE_EQUIVALENT:
						return feature.getVersionedIdentifier().getVersion()
						.isEquivalentTo(
								prereq.getVersionedIdentifier()
								.getVersion());
			case IImport.RULE_COMPATIBLE:
				return feature.getVersionedIdentifier().getVersion()
						.isCompatibleWith(
								prereq.getVersionedIdentifier()
								.getVersion());
			case IImport.RULE_GREATER_OR_EQUAL:
						return feature.getVersionedIdentifier().getVersion()
						.isGreaterOrEqualTo(
								prereq.getVersionedIdentifier()
								.getVersion());
			}

			return false;
		} else {
			if ((prereq.getKind() == IImport.KIND_PLUGIN)) { 
				return checkIfFeatureHasPlugin( prereq, feature);
			}
			return false;
		}
	}
	
	private boolean checkIfFeatureHasPlugin(IImport requiredFeature, IFeature feature)  {
		
		IPluginEntry[] plugins = feature.getPluginEntries();
		try {			
			List includedPlugins = getPluginEntriesFromIncludedFeatures(feature, new ArrayList(), new ArrayList());
			includedPlugins.addAll(Arrays.asList(plugins));
			plugins = (IPluginEntry[])includedPlugins.toArray( new IPluginEntry[includedPlugins.size()]);
		} catch( CoreException ce) {
			UpdateUI.logException(ce);
			// ignore this plugins can not sutisfy requirement anyways
		}
		if (plugins == null) {
			return false;
		}
		
		for(int i = 0; i < plugins.length; i++) {
			if (isMatch(plugins[i].getVersionedIdentifier(), requiredFeature.getVersionedIdentifier(), requiredFeature.getIdRule())) {
				return true;
			}
		}
		
		return false;
	}

	private List getPluginEntriesFromIncludedFeatures(IFeature feature, List plugins, List visitedFeatures) throws CoreException {
		IIncludedFeatureReference[] iifr = feature.getIncludedFeatureReferences();
		for(int i = 0; i < iifr.length; i++) {
			IFeature current = UpdateUtils.getIncludedFeature( feature, iifr[i]);
			if (!visitedFeatures.contains(current)) {
				IPluginEntry[] pluginEntries = current.getPluginEntries();
				plugins.addAll(Arrays.asList(pluginEntries));
				visitedFeatures.add(current);
				getPluginEntriesFromIncludedFeatures(current, plugins, visitedFeatures);
			}
		}
		
		return plugins;
	}

	// vid1 = feature
	// vid2 = requiredFeature
	private boolean isMatch( VersionedIdentifier vid1, VersionedIdentifier vid2, int rule) {
		
		if (!vid1.getIdentifier().equals(vid2.getIdentifier())) {
			return false;
		}
		if ( vid2.getVersion().getMajorComponent() == 0 && vid2.getVersion().getMinorComponent() == 0 && vid2.getVersion().getServiceComponent() == 0 ) {
			//version is ignored
			return true;
		}
		switch (rule) {
		case IImport.RULE_PERFECT:
			return vid1.getVersion().isPerfect(vid2.getVersion());
		case IImport.RULE_EQUIVALENT:
			return vid1.getVersion().isEquivalentTo(vid2.getVersion());
		case IImport.RULE_COMPATIBLE:
			return vid1.getVersion().isCompatibleWith(vid2.getVersion());
		case IImport.RULE_GREATER_OR_EQUAL:
			return vid1.getVersion().isGreaterOrEqualTo(vid2.getVersion());
		}
		return false;
	}

	public boolean isFeatureBetter(IInstallFeatureOperation feature,
			IInstallFeatureOperation currentFeatureSelected) {

		if (currentFeatureSelected == null)
			return true;
		// If the feature is the same, pick the newer one
		if (currentFeatureSelected.getFeature().getVersionedIdentifier().getIdentifier().equals(
				feature.getFeature().getVersionedIdentifier().getIdentifier())) {
			return !currentFeatureSelected.getFeature().getVersionedIdentifier()
			.getVersion().isGreaterOrEqualTo(
					feature.getFeature().getVersionedIdentifier()
							.getVersion());			
		}
		else {
			// Different features.
			// Pick a feature with smaller number of plug-ins
			NullProgressMonitor monitor = new NullProgressMonitor();
			int currentNumber = getTotalNumberOfPluginEntries(currentFeatureSelected.getFeature(), monitor);
			int newNumber = getTotalNumberOfPluginEntries(feature.getFeature(), monitor);
			return newNumber<currentNumber;
		}
	}
	
	private int getTotalNumberOfPluginEntries(IFeature feature, IProgressMonitor monitor) {
		int count = 0;
		try {
			count = feature.getPluginEntryCount();
			IIncludedFeatureReference [] irefs = feature.getIncludedFeatureReferences();
			for (int i=0; i<irefs.length; i++) {
				IFeature child = irefs[i].getFeature(monitor);
				count += getTotalNumberOfPluginEntries(child, monitor);
			}
		}
		catch (CoreException e) {
		}
		return count;
	}

	public IInstallFeatureOperation decideOnFeatureSelection(
			IImport requiredFeature, IInstallFeatureOperation feature,
			IInstallFeatureOperation currentFeatureSelected) {

		if (isFeatureGood(requiredFeature, feature.getFeature()) && isFeatureBetter(feature, currentFeatureSelected)) {
				return feature;
		} else {
			return currentFeatureSelected;
		}
	}
	
	private boolean isFeatureProblematic(IFeature feature) {
		
		if ( problematicFeatures.contains(feature) )
			return true;
		
		IImport[] iimports = feature.getImports();
		
		for(int i = 0; i < iimports.length; i++) {
			Iterator problematicFeatures = this.problematicFeatures.iterator();
			while(problematicFeatures.hasNext()) {
				if (iimports[i].getVersionedIdentifier().equals( ((IFeature)problematicFeatures.next()).getVersionedIdentifier()) ) {
					return true;
				}
			}
		}
		try {
			Iterator includedFeatures = OperationValidator.computeFeatureSubtree(feature, null, null, false, new ArrayList(), null ).iterator();
			while (includedFeatures.hasNext()) {
				Iterator problematicFeatures = this.problematicFeatures.iterator();		
				VersionedIdentifier currentIncludedFeaturesVI = ((IFeature)includedFeatures.next()).getVersionedIdentifier();
				while (problematicFeatures.hasNext()) {
					Object currentProblematicFeatures = problematicFeatures.next();
					if (currentProblematicFeatures instanceof IFeature) {
						VersionedIdentifier currentProblematicFeaturesVI = ((IFeature)currentProblematicFeatures).getVersionedIdentifier();						
						if (currentProblematicFeaturesVI.equals( currentIncludedFeaturesVI) ) {
							return true;
						}
					}
				}
			}
		} catch (CoreException ce) {
		}
 		return false;
	}
	
	private boolean isSelected( IInstallFeatureOperation[] selectedJobs, IInstallFeatureOperation iInstallFeatureOperation) {
		
		if (selectedJobs == null)
			return false;
		
		for( int i = 0; i < selectedJobs.length; i++) {
			
			if (iInstallFeatureOperation.getFeature().getVersionedIdentifier().equals(selectedJobs[i].getFeature().getVersionedIdentifier()) &&
				iInstallFeatureOperation.getFeature().getSite().getURL().equals(selectedJobs[i].getFeature().getSite().getURL())) {
				return true;
			}
		}
		
		return false;
	}
}

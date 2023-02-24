/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     IBM Corporation - Ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal.wizard;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.importexport.IUDetail;
import org.eclipse.equinox.internal.p2.importexport.VersionIncompatibleException;
import org.eclipse.equinox.internal.p2.importexport.internal.Messages;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.CompoundQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class ImportPage extends AbstractImportPage implements ISelectableIUsPage {

	private class InstallationContentProvider implements IStructuredContentProvider {

		public void dispose() {
			//
		}

		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			//
		}

	}

	private class InstallationLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			IInstallableUnit iu = ((IUDetail) element).getIU();
			switch (columnIndex) {
				case 0 :
					return getIUNameWithDetail(iu);
				case 1 :
					return iu.getVersion().toString();
				case 2 :
					return iu.getId();
				default :
					throw new RuntimeException("Should not happen"); //$NON-NLS-1$
			}

		}

		public Color getForeground(Object element) {
			if (hasInstalled(ProvUI.getAdapter(element, IInstallableUnit.class)))
				return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
			return null;
		}

		public Color getBackground(Object element) {
			return null;
		}
	}

	private List<IUDetail> features;
	private final List<URI> loadRepos = new ArrayList<URI>();
	private final Map<IUDetail, IUDetail[]> newProposedFeature = new HashMap<IUDetail, IUDetail[]>();
	private Button contactAll;
	private Button installLatest;
	private String oldDestination;

	public ImportPage(ProvisioningUI ui, ProvisioningOperationWizard wizard) {
		super("importpage", ui, wizard); //$NON-NLS-1$
		setTitle(Messages.ImportPage_TITLE);
		setDescription(Messages.ImportPage_DESCRIPTION);
	}

	@Override
	protected void createContents(Composite composite) {
		createDestinationGroup(composite);
		createInstallationTable(composite);
		createAdditionOptions(composite);
	}

	private void createAdditionOptions(Composite parent) {
		Composite composite = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		contactAll = new Button(composite, SWT.CHECK);
		contactAll.setText(ProvUIMessages.AvailableIUsPage_ResolveAllCheckbox);

		installLatest = new Button(composite, SWT.CHECK);
		installLatest.setText(Messages.ImportPage_InstallLatestVersion);
		installLatest.setSelection(true);
	}

	@Override
	protected IContentProvider getContentProvider() {
		return new InstallationContentProvider();
	}

	@Override
	protected ITableLabelProvider getLabelProvider() {
		return new InstallationLabelProvider();
	}

	@Override
	protected int getBrowseDialogStyle() {
		return SWT.OPEN;
	}

	@Override
	protected String getDestinationLabel() {
		return Messages.ImportPage_DESTINATION_LABEL;
	}

	@Override
	protected String getDialogTitle() {
		return Messages.ImportPage_FILEDIALOG_TITLE;
	}

	@Override
	protected IUDetail[] getInput() {
		return new IUDetail[0];
	}

	@Override
	protected String getInvalidDestinationMessage() {
		return Messages.ImportPage_DEST_ERROR;
	}

	@Override
	protected void giveFocusToDestination() {
		destinationNameField.setFocus();
	}

	@Override
	protected void updatePageCompletion() {
		super.updatePageCompletion();
		if (isPageComplete())
			getProvisioningWizard().operationSelectionsChanged(this);
	}

	@Override
	protected void handleDestinationChanged(String newDestination) {
		if (validateDestinationGroup()) {
			// p2f file is changed, update the cached data
			if (!newDestination.equals(oldDestination)) {
				loadRepos.clear();
				newProposedFeature.clear();
			}
			InputStream input = null;
			try {
				input = new BufferedInputStream(new FileInputStream(getDestinationValue()));
				features = importexportService.importP2F(input);
				viewer.setInput(features.toArray(new IUDetail[features.size()]));
				input.close();
			} catch (VersionIncompatibleException e) {
				MessageDialog.openWarning(getShell(), Messages.ImportPage_TITLE, e.getMessage());
			} catch (FileNotFoundException e) {
				MessageDialog.openError(getShell(), Messages.ImportPage_TITLE, Messages.ImportPage_FILENOTFOUND);
			} catch (IOException e) {
				MessageDialog.openError(getShell(), Messages.ImportPage_TITLE, e.getLocalizedMessage());
			}
		} else
			viewer.setInput(null);
	}

	@Override
	protected void setDestinationValue(String selectedFileName) {
		oldDestination = getDestinationValue();
		super.setDestinationValue(selectedFileName);
	}

	@Override
	protected boolean validDestination() {
		File target = new File(getDestinationValue());
		return super.validDestination() && target.exists() && target.canRead();
	}

	public Object[] getCheckedIUElements() {
		Object[] checked = viewer.getCheckedElements();
		List<IUDetail> features = new ArrayList<IUDetail>(checked.length);
		for (int i = 0; i < checked.length; i++) {
			IUDetail feature = (IUDetail) checked[i];
			IUDetail[] existingFeatures = newProposedFeature.get(feature);
			if (existingFeatures == null)
				features.add(feature);
			else {
				IUDetail matchPolicy = null;
				for (IUDetail f : existingFeatures) {
					if (matchPolicy == null)
						matchPolicy = f;
					// here use exact match
					else if (matchPolicy.getIU().getVersion().compareTo(f.getIU().getVersion()) < 0) {
						if (installLatest.getSelection())
							matchPolicy = f;
						else
							continue;
					} else
						matchPolicy = f;
				}
				if (matchPolicy != null)
					features.add(matchPolicy);
			}
		}
		return features.toArray(new IUDetail[features.size()]);
	}

	public Object[] getSelectedIUElements() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setCheckedElements(Object[] elements) {
		new UnsupportedOperationException();
	}

	public ProvisioningContext getProvisioningContext() {
		if (agent != null) {
			Object[] checked = viewer.getCheckedElements();
			List<URI> referredRepos = new ArrayList<URI>(checked.length);
			for (Object checkItem : checked) {
				IUDetail feature = (IUDetail) checkItem;
				for (URI uri : feature.getReferencedRepositories()) {
					referredRepos.add(uri);
				}
			}
			ProvisioningContext context = new ProvisioningContext(agent);
			if (!contactAll.getSelection()) {
				context.setArtifactRepositories(referredRepos.toArray(new URI[referredRepos.size()]));
				context.setMetadataRepositories(referredRepos.toArray(new URI[referredRepos.size()]));
			}
			return context;
		}
		return null;
	}

	public boolean hasUnloadedRepo() {
		for (Object checked : viewer.getCheckedElements()) {
			IUDetail feature = (IUDetail) checked;
			for (URI uri : feature.getReferencedRepositories())
				if (!loadRepos.contains(uri))
					return true;
		}
		return false;
	}

	class GetCheckedElement implements Runnable {
		Object[] checkedElements = null;

		public void run() {
			checkedElements = viewer.getCheckedElements();
		}
	}

	public Object[] getChecked() {
		if (Display.findDisplay(Thread.currentThread()) != null)
			return viewer.getCheckedElements();
		GetCheckedElement get = new GetCheckedElement();
		Display.getDefault().syncExec(get);
		return get.checkedElements;
	}

	public void recompute(IProgressMonitor monitor) throws InterruptedException {
		SubMonitor sub = SubMonitor.convert(monitor, Messages.ImportPage_QueryFeaturesJob, 1000);
		if (agent != null) {
			IMetadataRepositoryManager metaManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
			Object[] checked = getChecked();
			sub.setWorkRemaining(100 * checked.length);
			for (Object item : checked) {
				IUDetail feature = (IUDetail) item;
				if (!newProposedFeature.containsKey(feature)) {
					if (sub.isCanceled())
						throw new InterruptedException();
					SubMonitor sub2 = sub.newChild(100, SubMonitor.SUPPRESS_ALL_LABELS);
					sub2.setWorkRemaining(feature.getReferencedRepositories().size() * 500 + 100);
					List<IRepository<IInstallableUnit>> repos = new ArrayList<IRepository<IInstallableUnit>>();
					for (URI uri : feature.getReferencedRepositories()) {
						if (!metaManager.contains(uri)) {
							metaManager.addRepository(uri);
						}
						metaManager.setEnabled(uri, true);
						try {
							repos.add(metaManager.loadRepository(uri, sub2.newChild(500)));
						} catch (ProvisionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (OperationCanceledException e) {
							throw new InterruptedException(e.getLocalizedMessage());
						}
						if (!artifactManager.contains(uri)) {
							artifactManager.addRepository(uri);
						}
						artifactManager.setEnabled(uri, true);
					}
					if (sub2.isCanceled())
						throw new InterruptedException();
					@SuppressWarnings("unchecked")
					Set<IInstallableUnit> result = new CompoundQueryable<IInstallableUnit>(repos.toArray(new IRepository[repos.size()])).query(QueryUtil.createIUQuery(feature.getIU().getId(), new VersionRange(feature.getIU().getVersion(), true, null, false)), sub2.newChild(100)).toSet();
					List<IUDetail> existingFeatures = new ArrayList<IUDetail>(result.size());
					for (IInstallableUnit iu : result) {
						existingFeatures.add(new IUDetail(iu, feature.getReferencedRepositories()));
					}
					newProposedFeature.put(feature, existingFeatures.toArray(new IUDetail[existingFeatures.size()]));
				} else {
					if (sub.isCanceled())
						throw new InterruptedException();
					sub.worked(100);
				}
			}
		}
	}
}
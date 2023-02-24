/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;

public class OrganizeManifestsWizard extends RefactoringWizard {

	private OrganizeManifestsWizardPage fMainPage;

	public OrganizeManifestsWizard(PDERefactor refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEUIMessages.OrganizeManifestsWizard_title);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_ORGANIZE_MANIFESTS);
	}

	public boolean performFinish() {
		fMainPage.performOk();
		return super.performFinish();
	}

	protected void addUserInputPages() {
		setDefaultPageTitle(getRefactoring().getName());
		fMainPage = new OrganizeManifestsWizardPage();
		addPage(fMainPage);
	}
}

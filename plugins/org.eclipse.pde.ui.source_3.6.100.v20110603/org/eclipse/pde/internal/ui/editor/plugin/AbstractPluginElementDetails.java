/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDESection;

/**
 * AbstractPluginElementDetails
 *
 */
public abstract class AbstractPluginElementDetails extends PDEDetails {

	private PDESection fMasterSection;

	/**
	 * @param masterSection
	 */
	public AbstractPluginElementDetails(PDESection masterSection) {
		fMasterSection = masterSection;
	}

	/**
	 * @return
	 */
	public PDESection getMasterSection() {
		return fMasterSection;
	}

}

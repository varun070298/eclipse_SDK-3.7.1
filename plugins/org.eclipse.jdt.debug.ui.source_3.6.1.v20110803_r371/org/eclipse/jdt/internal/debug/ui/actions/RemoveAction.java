/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.ui.actions;


import java.util.List;

import org.eclipse.jdt.internal.debug.ui.launcher.IClasspathViewer;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Removes selected enries in a runtime classpath viewer.
 */
public class RemoveAction extends RuntimeClasspathAction {

	public RemoveAction(IClasspathViewer viewer) {
		super(ActionMessages.RemoveAction__Remove_1, viewer); 
	}
	/**
	 * Removes all selected entries.
	 * 
	 * @see IAction#run()
	 */
	public void run() {
		List targets = getOrderedSelection();
		List list = getEntriesAsList();
		list.removeAll(targets);
		setEntries(list);
	}

	/**
	 * @see SelectionListenerAction#updateSelection(IStructuredSelection)
	 */
	protected boolean updateSelection(IStructuredSelection selection) {
		if (selection.isEmpty()) {
			return false;
		}
		return getViewer().updateSelection(getActionType(), selection);
	}
	
	protected int getActionType() {
		return REMOVE;
	}
}

/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.sourcelookup;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
/**
 * Provides content for a tree viewer that shows only containers.
 * 
 * @since 3.0
 */
public class BasicContainerContentProvider implements ITreeContentProvider {

	private boolean fShowClosedProjects = true;
	/**
	 * Creates a new ResourceContentProvider.
	 */
	public BasicContainerContentProvider() {
	}
	/**
	 * The visual part that is using this content provider is about
	 * to be disposed. Deallocate all allocated SWT resources.
	 */
	public void dispose() {
	}
	
	/**
	 * @see ITreeContentProvider#getChildren
	 */
	public Object[] getChildren(Object element) {
		if (element instanceof IWorkspaceRoot) {
			// check if closed projects should be shown
			IProject[] allProjects = ((IWorkspaceRoot) element).getProjects();
			if (fShowClosedProjects)
				return allProjects;
			
			ArrayList accessibleProjects = new ArrayList();
			for (int i = 0; i < allProjects.length; i++) {
				if (allProjects[i].isOpen()) {
					accessibleProjects.add(allProjects[i]);
				}
			}
			return accessibleProjects.toArray();
		}
		return new Object[0];
	}
	
	/**
	 * @see ITreeContentProvider#getElements
	 */
	public Object[] getElements(Object element) {
		return getChildren(element);
	}
	
	/**
	 * @see ITreeContentProvider#getParent
	 */
	public Object getParent(Object element) {
		if (element instanceof IResource)
			return ((IResource) element).getParent();
		return null;
	}
	
	/**
	 * @see ITreeContentProvider#hasChildren
	 */
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}
	
	/**
	 * @see IContentProvider#inputChanged
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		
	}
}

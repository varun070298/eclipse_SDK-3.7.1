/*******************************************************************************
 * Copyright (c) 2009, 2010 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - ongoing bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStateUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.VirtualTreeModelViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.widgets.Display;

/**
 * Interface of an tree model viewer.  It declares the common methods for the
 * JFace-based {@link TreeModelViewer} and the UI-less 
 * {@link VirtualTreeModelViewer}.
 *
 * @since 3.5
 */
public interface ITreeModelViewer extends ISelectionProvider {

    /**
     * Constant indicating that all levels of the tree should be expanded or
     * collapsed.
     *
     * @see #setAutoExpandLevel(int)
     * @see #getAutoExpandLevel()
     */
    public static final int ALL_LEVELS = -1;

    /**
     * Returns the Display object that this viewer is in.  The
     * display object can be used by clients to access the display thread
     * to call the viewer methods.
     */
    public Display getDisplay();

    /**
     * Returns this viewer's presentation context.
     * 
     * @return presentation context
     */
    public IPresentationContext getPresentationContext();

    /**
     * Returns the current input of this viewer, or <code>null</code>
     * if none. The viewer's input provides the "model" for the viewer's
     * content.
     */
    public Object getInput();

    /**
     * Sets the input of this viewer.  Setting the input resets the 
     * viewer's contents and triggers an update starting at the input
     * element.
     * @param object Input element, or <code>null</code> if none.
     */
    public void setInput(Object object);
    
    /**
     * Returns the current selection in viewer.
     */
    public ISelection getSelection();

    /**
     * Sets a new selection for this viewer and optionally makes it visible.
     * The selection is not set if the model selection policy overrides the
     * attempt to set the selection.
     * 
     * @param selection the new selection
     * @param reveal <code>true</code> if the selection is to be made
     *   visible, and <code>false</code> otherwise
     * @param force <code>true</code> if the selection should override the 
     *   model selection policy
     */
    public void setSelection(ISelection selection, boolean reveal, boolean force);
    
    /**
     * Attempts to set the selection for this viewer and optionally makes it visible.
     * The selection is not set if the model selection policy overrides the
     * attempt to set the selection.
     * 
     * @param selection the new selection
     * @param reveal whether to make the selection visible after successfully setting
     *  the selection
     * @param force whether to force the selection (override the model selection policy)
     * @return <code>true</code> if the selection was set and <code>false</code> if the
     *  model selection policy overrides the selection attempt
     */
    public boolean trySelection(ISelection selection, boolean reveal, boolean force);

    /**
     * Returns the auto-expand level.
     *
     * @return non-negative level, or <code>ALL_LEVELS</code> if all levels of
     *         the tree are expanded automatically
     * @see #setAutoExpandLevel
     */    
    public int getAutoExpandLevel();

    /**
     * Sets the auto-expand level to be used when the input of the viewer is set
     * using {@link #setInput(Object)}. The value 0 means that there is no
     * auto-expand; 1 means that the invisible root element is expanded (since
     * most concrete implementations do not show the root element, there is usually
     * no practical difference between using the values 0 and 1); 2 means that
     * top-level elements are expanded, but not their children; 3 means that
     * top-level elements are expanded, and their children, but not
     * grandchildren; and so on.
     * <p>
     * The value <code>ALL_LEVELS</code> means that all subtrees should be
     * expanded.
     * </p>
     * 
     * @param level
     *            non-negative level, or <code>ALL_LEVELS</code> to expand all
     *            levels of the tree
     */
    public void setAutoExpandLevel(int level);
        
    /**
     * Returns the label data for the given element and for the given column, 
     * Returns <code>null</code> if the given element is not found or is not 
     * materialized in the virtual viewer.  Clients may listen to label update 
     * events to be notified when element labels are updated.
     * 
     * @param path Path of the element.
     * @param columnIdx ID of the column for which to return the label data.
     * @return Label object containing the label information.  Can be 
     * <code>null</code> if the given element is not found or is not 
     * materialized in the virtual viewer.
     */
    public ViewerLabel getElementLabel(TreePath path, String columnId);
    
    /**
     * Registers the specified listener for view update notifications.
     */
    public void addViewerUpdateListener(IViewerUpdateListener listener);
    
    /**
     * Removes the specified listener from update notifications.
     */
    public void removeViewerUpdateListener(IViewerUpdateListener listener);
    
    /**
     * Registers the specified listener for state update notifications.
     * @since 3.6
     */
    public void addStateUpdateListener(IStateUpdateListener listener);
    
    /**
     * Removes the specified listener from state update notifications.
     * @since 3.6
     */
    public void removeStateUpdateListener(IStateUpdateListener listener);
    
    /**
     * Registers the specified listener for view label update notifications.
     */
    public void addLabelUpdateListener(ILabelUpdateListener listener);
    
    /**
     * Removes the specified listener from view label update notifications.
     */
    public void removeLabelUpdateListener(ILabelUpdateListener listener);
    
    /**
     * Registers the given listener for model delta notification.
     * This listener is called immediately after the viewer processes
     * the delta.  
     */
    public void addModelChangedListener(IModelChangedListener listener);
    
    /**
     * Removes the given listener from model delta notification.
     */
    public void removeModelChangedListener(IModelChangedListener listener);
    
    /**
     * Writes state information into a delta for the sub-tree at the given
     * path.  It adds delta nodes and IModelDelta.EXPAND and IModelDelta.SELECT 
     * as it parses the sub-tree.
     * @param path Path where to start saving the state.
     * @param delta The delta where the state is to be saved.
     * @param flagsToSave The flags to preserve during the state save.  The 
     * supported flags are <code>IModelDelta.SELECT</code>, 
     * <code>IModelDelta.EXPAND</code>, <code>IModelDelta.COLLAPSE</code>.
     * @return Returns whether the state was saved for the given path.  Will 
     * return <code>false</code> if an element at the given path cannot 
     * be found.
     *
     * @since 3.6
     */
    public boolean saveElementState(TreePath path, ModelDelta delta, int flagsToSave);
    
    /**
     * Causes the viewer to process the given delta as if it came from a
     * model proxy.  This method is intended to be used to restore state
     * saved using {@link #saveElementState(TreePath, ModelDelta)}.
     * 
     * @param delta Delta to process.
     */
    public void updateViewer(IModelDelta delta);
}

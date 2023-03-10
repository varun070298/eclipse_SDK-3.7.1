/*****************************************************************
 * Copyright (c) 2009 Texas Instruments and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 286310)
 *     IBM Corporation - ongoing enhancements
 *****************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import org.eclipse.jface.viewers.TreePath;


/**
 * Optional extension to a model proxy for models that use a check box tree viewer. Provides
 * notification for check state changes in the tree. 
 * 
 * @since 3.6
 */
public interface ICheckboxModelProxy extends IModelProxy {

    /**
     * Notifies the receiver that the given element has had its 
     * checked state modified in the viewer.
     * <p>
     * This method is called in the UI thread. Clients that execute long running operations or
     * communicate with a potentially unreliable or blocking model should run those operations
     * asynchronously.
     * </p>
     * 
     * @param context Presentation context in which the element was updated.
     * @param viewerInput The root element of the viewer where the check
     * selection took place.
     * @param path Path of the element that had its checked state changed
     * @param checked The new checked state of the element
     * @return false if the check state should not change
     */
    public boolean setChecked(IPresentationContext context, Object viewerInput, TreePath path, boolean checked);

}

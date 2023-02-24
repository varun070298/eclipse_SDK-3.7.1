/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions;

/**
 * Listener for changes in the preferred toggle targets in the toggle
 * breakpoints target manager.  This interface allows toggle breakpoint
 * actions to update their enablement when the user changes the preferred
 * toggle target settings.
 * 
 * @see ToggleBreakpointsTargetManager
 * @see IToggleBreakpointsTargetFactory
 * @since 3.5
 */
public interface IToggleBreakpointsTargetManagerListener {
    
    /**
     * Called when the preferred toggle targets have changed.
     */
    public void preferredTargetsChanged();
}

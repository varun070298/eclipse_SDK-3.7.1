/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;


/**
 * Request to compare an element to a previously created memento.
 * <p>
 * Clients are not intended to implement this interface.
 * </p>
 * @since 3.3
 */
public interface IElementCompareRequest extends IElementMementoRequest {
	
	/**
	 * Sets whether this request's memento represents this requests's element.
	 * 
	 * @param equal whether the memento represents the element 
	 */
	public void setEqual(boolean equal);
}

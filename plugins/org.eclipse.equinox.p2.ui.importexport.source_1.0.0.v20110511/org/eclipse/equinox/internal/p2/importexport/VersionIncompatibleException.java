/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport;

public class VersionIncompatibleException extends RuntimeException {

	public VersionIncompatibleException(String message) {
		super(message);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -7584242899366859010L;

}

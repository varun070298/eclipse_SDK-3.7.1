/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.net.URL;

import org.eclipse.update.core.SiteContentProvider;

/**
 * 
 */
public class SiteURLContentProvider extends SiteContentProvider {
	
	public static final String SITE_TYPE = "org.eclipse.update.core.http"; //$NON-NLS-1$

	/**
	 * Constructor for HTTPSite
	 */
	public SiteURLContentProvider(URL url) {
		super(url);
	}
}



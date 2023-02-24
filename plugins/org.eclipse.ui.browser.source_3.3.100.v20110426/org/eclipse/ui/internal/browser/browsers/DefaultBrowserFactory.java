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
package org.eclipse.ui.internal.browser.browsers;

import org.eclipse.ui.browser.BrowserFactory;
import org.eclipse.ui.browser.IWebBrowser;
/**
 * Produces Custom Browser
 */
public class DefaultBrowserFactory extends BrowserFactory {
	/**
	 * @see org.eclipse.ui.browser.provisional.BrowserFactory#createBrowser()
	 */
	public IWebBrowser createBrowser(String id, String location, String parameters) {
		return new DefaultBrowser(id, location, parameters);
	}
}
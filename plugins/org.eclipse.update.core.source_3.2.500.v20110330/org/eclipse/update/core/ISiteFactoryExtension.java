/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;

import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.model.*;

/**
 * <p>
 * This is an extension to the standard ISiteFactory interface.
 * If a factory implements this interface and is handling
 * URL connections, a progress monitor can be passed to 
 * allow canceling of frozen connections. 
 * </p>
 * <p>Input stream is obtained from the connection on
 * a separate thread. When connection is canceled, 
 * the thread is still active. It is allowed to terminate
 * when the connection times out on its own.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.BaseSiteFactory
 * @since 2.1
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface ISiteFactoryExtension {
	/**
	 * Returns a site defined by the supplied URL. 
	 * <p>
	 * The actual interpretation of the URL is site-type specific.
	 * In most cases the URL will point to some site-specific
	 * file that can be used (directly or indirectly) to construct
	 * the site object.
	 * </p>
	 * @param url URL interpreted by the site
	 * @param monitor a progress monitor that can be canceled
	 * @return site object
	 * @exception CoreException
	 * @exception InvalidSiteTypeException the referenced site type is
	 * not a supported type for this factory 
	 * @since 2.0 
	 */
	public ISite createSite(URL url, IProgressMonitor monitor)
		throws CoreException, InvalidSiteTypeException;
}

/****************************************************************************
 * Copyright (c) 2008 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *****************************************************************************/

package org.eclipse.ecf.provider.filetransfer.browse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.eclipse.ecf.filetransfer.IRemoteFileAttributes;

/**
 *
 */
public class URLRemoteFileAttributes implements IRemoteFileAttributes {

	static String[] fileAttributes = {IRemoteFileAttributes.READ_ATTRIBUTE, IRemoteFileAttributes.WRITE_ATTRIBUTE, IRemoteFileAttributes.HIDDEN_ATTRIBUTE, IRemoteFileAttributes.EXEC_ATTRIBUTE, IRemoteFileAttributes.ARCHIVE_ATTRIBUTE};
	static List attributeKeys = new ArrayList(Arrays.asList(fileAttributes));

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.filetransfer.IRemoteFileAttributes#getAttribute(java.lang.String)
	 */
	public String getAttribute(String key) {
		if (key == null)
			return null;
		if (key.equals(IRemoteFileAttributes.READ_ATTRIBUTE)) {
			return Boolean.TRUE.toString();
		} else if (key.equals(IRemoteFileAttributes.WRITE_ATTRIBUTE)) {
			return Boolean.FALSE.toString();
		} else if (key.equals(IRemoteFileAttributes.HIDDEN_ATTRIBUTE)) {
			return Boolean.FALSE.toString();
		} else if (key.equals(IRemoteFileAttributes.EXEC_ATTRIBUTE)) {
			return Boolean.FALSE.toString();
		} else if (key.equals(IRemoteFileAttributes.ARCHIVE_ATTRIBUTE)) {
			return Boolean.TRUE.toString();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.filetransfer.IRemoteFileAttributes#getAttributeKeys()
	 */
	public Iterator getAttributeKeys() {
		return attributeKeys.iterator();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.filetransfer.IRemoteFileAttributes#setAttribute(java.lang.String, java.lang.String)
	 */
	public void setAttribute(String key, String value) {
		//
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer("URLRemoteFileAttributes["); //$NON-NLS-1$
		for (Iterator i = getAttributeKeys(); i.hasNext();) {
			String key = (String) i.next();
			buf.append(key).append("=").append(getAttribute(key)); //$NON-NLS-1$
			buf.append(i.hasNext() ? ";" : "]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return buf.toString();
	}

}

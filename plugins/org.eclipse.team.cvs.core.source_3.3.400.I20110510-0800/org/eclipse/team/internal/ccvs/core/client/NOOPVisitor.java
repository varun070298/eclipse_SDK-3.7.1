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
package org.eclipse.team.internal.ccvs.core.client;

import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;

/**
 * Send the contents of the CVS/Notify files to the server
 */
public class NOOPVisitor extends AbstractStructureVisitor {

	public NOOPVisitor(Session session, LocalOption[] localOptions) {
		// Only send non-empty folders
		super(session, localOptions, false, false);
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor#visitFile(ICVSFile)
	 */
	public void visitFile(ICVSFile file) throws CVSException {
		sendPendingNotification(file);
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor#visitFolder(ICVSFolder)
	 */
	public void visitFolder(ICVSFolder folder) throws CVSException {
		if (isRecurse() && folder.isCVSFolder()) {
			folder.acceptChildren(this);
		}
	}

}

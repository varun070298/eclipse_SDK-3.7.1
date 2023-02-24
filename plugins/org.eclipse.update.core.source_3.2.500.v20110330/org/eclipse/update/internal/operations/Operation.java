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
package org.eclipse.update.internal.operations;

import org.eclipse.update.operations.*;


public abstract class Operation implements IOperation {
	
	private boolean processed;
	
	
	public Operation() {
	}
	
	public boolean isProcessed() {
		return processed;
	}
	
	public void markProcessed() {
		processed = true;
	}
}

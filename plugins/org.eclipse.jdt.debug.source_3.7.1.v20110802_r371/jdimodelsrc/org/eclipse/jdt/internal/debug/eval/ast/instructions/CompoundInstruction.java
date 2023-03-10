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
package org.eclipse.jdt.internal.debug.eval.ast.instructions;

/**
 * A <code>CompoundInstruction</code> is a container instruction
 * and may have a size greater than one.
 */
public abstract class CompoundInstruction extends Instruction {

	private int fSize;
	
	/**
	 * Constructor for CompoundInstruction.
	 * @param start
	 */
	protected CompoundInstruction(int start) {
		fSize= -start;
	}

	public void setEnd(int end) {
		fSize += end;
	}

	public int getSize() {
		return fSize;
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.sun.jdi;


import java.util.List;
import java.util.Map;

public interface ObjectReference extends com.sun.jdi.Value {
	public static final int INVOKE_SINGLE_THREADED = 1;
	public static final int INVOKE_NONVIRTUAL = 2;
	public void disableCollection();
	public void enableCollection();
	public int entryCount() throws IncompatibleThreadStateException;
	public boolean equals(Object arg1);
	public Value getValue(Field arg1);
	public Map getValues(java.util.List arg1);
	public int hashCode();
	public Value invokeMethod(ThreadReference arg1, Method arg2, List arg3, int arg4) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException;
	public boolean isCollected();
	public ThreadReference owningThread() throws IncompatibleThreadStateException;
	public com.sun.jdi.ReferenceType referenceType();
	public void setValue(Field arg1, Value arg2) throws InvalidTypeException, ClassNotLoadedException;
	public long uniqueID();
	public List waitingThreads() throws IncompatibleThreadStateException;
	public List referringObjects(long arg1);
}

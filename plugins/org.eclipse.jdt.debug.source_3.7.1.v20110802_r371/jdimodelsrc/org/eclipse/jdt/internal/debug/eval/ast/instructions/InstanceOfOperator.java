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
package org.eclipse.jdt.internal.debug.eval.ast.instructions;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.internal.debug.core.model.JDINullValue;

public class InstanceOfOperator extends CompoundInstruction {
	public static final String IS_INSTANCE= "isInstance"; //$NON-NLS-1$
	public static final String IS_INSTANCE_SIGNATURE= "(Ljava/lang/Object;)Z"; //$NON-NLS-1$
	
	public InstanceOfOperator(int start) {
		super(start);
	}
	
	/*
	 * @see Instruction#execute()
	 */
	public void execute() throws CoreException {
		IJavaType type= (IJavaType)pop();
		IJavaValue value= popValue();
		if (value instanceof JDINullValue) {
			pushNewValue(false);
			return;
		}
		IJavaObject object= (IJavaObject)value;

		IJavaObject classObject= getClassObject(type);
		if (classObject == null) {
			throw new CoreException(new Status(IStatus.ERROR, JDIDebugPlugin.getUniqueIdentifier(), IStatus.OK, MessageFormat.format(InstructionsEvaluationMessages.InstanceOfOperator_No_class_object, new String[]{type.getName()}), null)); 
		}
		push(classObject.sendMessage(IS_INSTANCE, IS_INSTANCE_SIGNATURE, new IJavaValue[] {object}, getContext().getThread(), false));
	}
	
	public String toString() {
		return InstructionsEvaluationMessages.InstanceOfOperator__instanceof___operator_3; 
	}

}

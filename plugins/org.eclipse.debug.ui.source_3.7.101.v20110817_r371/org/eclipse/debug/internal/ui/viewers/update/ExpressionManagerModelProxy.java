/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Wind Rvier Systems - added support for columns (bug 235646)
 *******************************************************************************/

package org.eclipse.debug.internal.ui.viewers.update;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.internal.core.IExpressionsListener2;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.Viewer;


/**
 * Model proxy that fires model delta updates for the ExpressionManager.
 * 
 * @see org.eclipse.debug.internal.core.ExpressionManager
 */
public class ExpressionManagerModelProxy extends AbstractModelProxy implements IExpressionsListener2, IPropertyChangeListener {
        
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.viewers.update.EventHandlerModelProxy#init(org.eclipse.debug.internal.ui.viewers.IPresentationContext)
	 */
	public void init(IPresentationContext context) {
		super.init(context);
		getExpressionManager().addExpressionListener(this);
		context.addPropertyChangeListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy#installed(org.eclipse.jface.viewers.Viewer)
	 */
	public void installed(Viewer viewer) {
		updateExpressions(getExpressionManager().getExpressions(), IModelDelta.INSTALL);
	}

	/**
	 * @return the default expression manager from the debug plugin
	 */
	protected IExpressionManager getExpressionManager() {
		return DebugPlugin.getDefault().getExpressionManager();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.viewers.update.EventHandlerModelProxy#dispose()
	 */
	public synchronized void dispose() {
        getPresentationContext().removePropertyChangeListener(this);
		super.dispose();
		getExpressionManager().removeExpressionListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.IExpressionsListener2#expressionsMoved(org.eclipse.debug.core.model.IExpression[], int)
	 */
	public void expressionsMoved(IExpression[] expressions, int index){
		ModelDelta delta = new ModelDelta(getExpressionManager(), IModelDelta.NO_CHANGE);
		int count = getElementsCount();
		for (int i = 0; i < expressions.length; i++) {
			IExpression expression = expressions[i];
			delta.addNode(expression, IModelDelta.REMOVED);
		}
		for (int i = 0; i < expressions.length; i++) {
			IExpression expression = expressions[i];
			delta.addNode(expression, index+i, IModelDelta.ADDED, count);
		}
		fireModelChanged(delta);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.IExpressionsListener2#expressionsInserted(org.eclipse.debug.core.model.IExpression[], int)
	 */
	public void expressionsInserted(IExpression[] expressions, int index){
	    int count = getElementsCount();
		ModelDelta delta = new ModelDelta(getExpressionManager(), IModelDelta.NO_CHANGE);
		for (int i = 0; i < expressions.length; i++) {
			IExpression expression = expressions[i];
			delta.addNode(expression, index+i, IModelDelta.ADDED | IModelDelta.INSTALL, count);
		}
		fireModelChanged(delta);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IExpressionsListener#expressionsAdded(org.eclipse.debug.core.model.IExpression[])
	 */
	public void expressionsAdded(IExpression[] expressions) {
	    int index = getExpressionManager().getExpressions().length - expressions.length;
	    int count = getElementsCount();
        ModelDelta delta = new ModelDelta(getExpressionManager(), IModelDelta.NO_CHANGE);
        for (int i = 0; i < expressions.length; i++) {
            IExpression expression = expressions[i];
            delta.addNode(expression, index+i, IModelDelta.ADDED | IModelDelta.INSTALL, count);
        }
        fireModelChanged(delta);
		updateExpressions(expressions, IModelDelta.ADDED | IModelDelta.INSTALL);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IExpressionsListener#expressionsRemoved(org.eclipse.debug.core.model.IExpression[])
	 */
	public void expressionsRemoved(IExpression[] expressions) {
		updateExpressions(expressions, IModelDelta.REMOVED | IModelDelta.UNINSTALL);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IExpressionsListener#expressionsChanged(org.eclipse.debug.core.model.IExpression[])
	 */
	public void expressionsChanged(IExpression[] expressions) {
		updateExpressions(expressions, IModelDelta.CONTENT | IModelDelta.STATE);		
	}
    
    private void updateExpressions(IExpression[] expressions, int flags) {
		ModelDelta delta = new ModelDelta(getExpressionManager(), IModelDelta.NO_CHANGE);
		for (int i = 0; i < expressions.length; i++) {
			IExpression expression = expressions[i];
			delta.addNode(expression, flags);
		}
		fireModelChanged(delta);
    }

    private int getElementsCount() {
        // Account for the "Add new expression" element only if columns are 
        // displayed.
        return getExpressionManager().getExpressions().length +
            (getPresentationContext().getColumns() != null ? 1 : 0);
    }
    
    public void propertyChange(PropertyChangeEvent event) {
        // If columns are turned on/off, refresh the view to account for the 
        // "Add new expression" element.
        if (IPresentationContext.PROPERTY_COLUMNS.equals(event.getProperty())) {
            fireModelChanged(new ModelDelta(getExpressionManager(), IModelDelta.CONTENT));
       }
    }

    
}

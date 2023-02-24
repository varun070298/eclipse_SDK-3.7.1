/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.help.internal.base.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.help.ICriteria;
import org.eclipse.help.internal.criteria.CriterionResource;

public class CriteriaUtilities {
	
    public static List getCriteriaValues(String rawValues) {
    	List result = new ArrayList();
    	if (rawValues != null) {
    		String[] values = rawValues.split(","); //$NON-NLS-1$
    		for(int j = 0; j < values.length; ++j){
    			String value = values[j].trim();
    			if (value.length() > 0) {
    				result.add(value);
    			}
    		}
    	}
		return result;
    }
    
    public static void addCriteriaToMap(Map map, ICriteria[] criteria) {
    	for (int i = 0; i < criteria.length; ++i) {
			ICriteria criterion = criteria[i];
			String name = criterion.getName();
			List values = CriteriaUtilities.getCriteriaValues(criterion.getValue());
			if (name != null && name.length() > 0 && values.size() > 0) {
				name = name.toLowerCase();
				Set existingValueSet = (Set) map.get(name);
				if (null == existingValueSet) {
					existingValueSet = new HashSet();
				}
				existingValueSet.addAll(values);
				map.put(name, existingValueSet);
			}		
		}
    }
    
    public static void addCriteriaToMap(Map map, CriterionResource[] criteria) {
    	for(int i = 0; i < criteria.length; ++ i){
			CriterionResource criterion = criteria[i];
			String criterionName = criterion.getCriterionName();
			List criterionValues = criterion.getCriterionValues();
			
			Set existedValueSet = (Set)map.get(criterionName);
			if (null == existedValueSet)
				existedValueSet = new HashSet();
			existedValueSet.addAll(criterionValues);
			map.put(criterionName, existedValueSet);
		}
    }
    
}

/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.internal.base.remote;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class RemoteContentLocator {

	private static Map InfoCenterMap = null; // hash table

	public RemoteContentLocator() {
		
	}

	public static void addContentPage(String contributorID, String InfoCenterUrl) {
		
		if (InfoCenterMap == null){
			InfoCenterMap = new HashMap();
			InfoCenterMap = new TreeMap(); // sorted map
		}
		
		InfoCenterMap.put(contributorID, InfoCenterUrl);
	}

	public static String getUrlForContent(String contributorID) {

		if (InfoCenterMap == null)
			return null;

		Object key = InfoCenterMap.get(contributorID);
		return (String)key;
	}

	public static Map getInfoCenterMap() {
		return InfoCenterMap;
	}

}

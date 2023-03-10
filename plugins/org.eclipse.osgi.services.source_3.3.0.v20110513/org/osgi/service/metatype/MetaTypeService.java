/*
 * Copyright (c) OSGi Alliance (2005, 2010). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.service.metatype;

import org.osgi.framework.Bundle;

/**
 * The MetaType Service can be used to obtain meta type information for a
 * bundle. The MetaType Service will examine the specified bundle for meta type
 * documents to create the returned {@code MetaTypeInformation} object.
 * 
 * <p>
 * If the specified bundle does not contain any meta type documents, then a
 * {@code MetaTypeInformation} object will be returned that wrappers any
 * {@code ManagedService} or {@code ManagedServiceFactory}
 * services registered by the specified bundle that implement
 * {@code MetaTypeProvider}. Thus the MetaType Service can be used to
 * retrieve meta type information for bundles which contain a meta type
 * documents or which provide their own {@code MetaTypeProvider} objects.
 * 
 * @noimplement
 * @version $Id: 2324451e73130e2c8dde2aa1595997a05c77ab5b $
 * @since 1.1
 */
public interface MetaTypeService {
	/**
	 * Return the MetaType information for the specified bundle.
	 * 
	 * @param bundle The bundle for which meta type information is requested.
	 * @return A MetaTypeInformation object for the specified bundle.
	 */
	public MetaTypeInformation getMetaTypeInformation(Bundle bundle);

	/**
	 * Location of meta type documents. The MetaType Service will process each
	 * entry in the meta type documents directory.
	 */
	public final static String	METATYPE_DOCUMENTS_LOCATION	= "OSGI-INF/metatype";
}

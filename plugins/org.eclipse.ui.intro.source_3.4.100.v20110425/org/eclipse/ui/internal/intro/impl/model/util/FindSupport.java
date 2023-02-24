/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.intro.impl.model.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

// This class provides implements the find* methods exposed on Platform.
// It does the lookup in bundles and fragments and does the variable replacement.
public class FindSupport {
	private static String[] NL_JAR_VARIANTS = buildNLVariants(Platform.getNL());

	private static String[] buildNLVariants(String nl) {
		ArrayList result = new ArrayList();
		IPath base = new Path("nl"); //$NON-NLS-1$

		IPath path = new Path(nl.replace('_', '/'));
		while (path.segmentCount() > 0) {
			result.add(base.append(path).toString());
			// for backwards compatibility only, don't replace the slashes
			if (path.segmentCount() > 1)
				result.add(base.append(path.toString().replace('/', '_')).toString());
			path = path.removeLastSegments(1);
		}

		return (String[]) result.toArray(new String[result.size()]);
	}

	/**
	 * See doc on @link Platform#find(Bundle, IPath) Platform#find(Bundle, IPath) 
	 */
	public static URL find(Bundle bundle, IPath path) {
		return find(bundle, path, null);
	}

    /**
     * Proposed API for Platform in Eclispe 3.2. 
     * Same as @link #find(Bundle, IPath) except multiple entries can be
     * returned if more than one entry matches the path in the host and 
     * any of its fragments.
     * 
     * @param bundle
     * @param path
     * @return an array of entries which match the given path.  An empty 
     * array is returned if no matches are found.
     */
    public static URL[] findEntries(Bundle bundle, IPath path) {
        return findEntries(bundle, path, null);
    }

    /**
     * Proposed API for Platform in Eclispe 3.2. 
     * Same as @link #find(Bundle, IPath) except multiple entries can be
     * returned if more than one entry matches the path in the host and 
     * any of its fragments.
     * 
     * @param bundle
     * @param path
     * @param override
     * @returnan array of entries which match the given path.  An empty 
     * array is returned if no matches are found.
     */
    public static URL[] findEntries(Bundle bundle, IPath path, Map override) {
        ArrayList results = new ArrayList(1);
        find(bundle, path, override, results);
        return (URL[]) results.toArray(new URL[results.size()]);
    }

	/**
	 * See doc on @link Platform#find(Bundle, IPath, Map) Platform#find(Bundle, IPath, Map) 
	 */
	public static URL find(Bundle b, IPath path, Map override) {
        return find(b, path, override, null);
    }
 
    private static URL find(Bundle b, IPath path, Map override, ArrayList multiple) {
		if (path == null)
			return null;

		URL result = null;

		// Check for the empty or root case first
		if (path.isEmpty() || path.isRoot()) {
			// Watch for the root case.  It will produce a new
			// URL which is only the root directory (and not the
			// root of this plugin).	
			result = findInPlugin(b, Path.EMPTY, multiple);
			if (result == null || multiple != null)
				result = findInFragments(b, Path.EMPTY, multiple);
            return result;
		}

		// Now check for paths without variable substitution
		String first = path.segment(0);
		if (first.charAt(0) != '$') {
			result = findInPlugin(b, path, multiple);
			if (result == null || multiple != null)
				result = findInFragments(b, path, multiple);
            return result;
		}

		// Worry about variable substitution
		IPath rest = path.removeFirstSegments(1);
		if (first.equalsIgnoreCase("$nl$")) //$NON-NLS-1$
			return findNL(b, rest, override, multiple);
		if (first.equalsIgnoreCase("$os$")) //$NON-NLS-1$
			return findOS(b, rest, override, multiple);
		if (first.equalsIgnoreCase("$ws$")) //$NON-NLS-1$
			return findWS(b, rest, override, multiple);
		if (first.equalsIgnoreCase("$files$")) //$NON-NLS-1$
			return null;

		return null;
	}

	private static URL findOS(Bundle b, IPath path, Map override, ArrayList multiple) {
		String os = null;
		if (override != null)
			try {
				// check for override
				os = (String) override.get("$os$"); //$NON-NLS-1$
			} catch (ClassCastException e) {
				// just in case
			}
		if (os == null)
			// use default
			os = Platform.getOS();
		if (os.length() == 0)
			return null;

		// Now do the same for osarch
		String osArch = null;
		if (override != null)
			try {
				// check for override
				osArch = (String) override.get("$arch$"); //$NON-NLS-1$
			} catch (ClassCastException e) {
				// just in case
			}
		if (osArch == null)
			// use default
			osArch = Platform.getOSArch();
		if (osArch.length() == 0)
			return null;

		URL result = null;
		IPath base = new Path("os").append(os).append(osArch); //$NON-NLS-1$
		// Keep doing this until all you have left is "os" as a path
		while (base.segmentCount() != 1) {
			IPath filePath = base.append(path);
			result = findInPlugin(b, filePath, multiple);
			if (result != null && multiple == null)
				return result;
			result = findInFragments(b, filePath, multiple);
			if (result != null && multiple == null)
				return result;
			base = base.removeLastSegments(1);
		}
		// If we get to this point, we haven't found it yet.
		// Look in the plugin and fragment root directories
		result = findInPlugin(b, path, multiple);
		if (result != null && multiple == null)
			return result;
		return findInFragments(b, path, multiple);
	}

	private static URL findWS(Bundle b, IPath path, Map override, ArrayList multiple) {
		String ws = null;
		if (override != null)
			try {
				// check for override
				ws = (String) override.get("$ws$"); //$NON-NLS-1$
			} catch (ClassCastException e) {
				// just in case
			}
		if (ws == null)
			// use default
			ws = Platform.getWS();
		IPath filePath = new Path("ws").append(ws).append(path); //$NON-NLS-1$
		// We know that there is only one segment to the ws path
		// e.g. ws/win32	
		URL result = findInPlugin(b, filePath, multiple);
		if (result != null && multiple == null)
			return result;
		result = findInFragments(b, filePath, multiple);
		if (result != null && multiple == null)
			return result;
		// If we get to this point, we haven't found it yet.
		// Look in the plugin and fragment root directories
		result = findInPlugin(b, path, multiple);
		if (result != null && multiple == null)
			return result;
		return findInFragments(b, path, multiple);
	}

	private static URL findNL(Bundle b, IPath path, Map override, ArrayList multiple) {
		String nl = null;
		String[] nlVariants = null;
		if (override != null)
			try {
				// check for override
				nl = (String) override.get("$nl$"); //$NON-NLS-1$
			} catch (ClassCastException e) {
				// just in case
			}
		nlVariants = nl == null ? NL_JAR_VARIANTS : buildNLVariants(nl);
		if (nl != null && nl.length() == 0)
			return null;

		URL result = null;
		for (int i = 0; i < nlVariants.length; i++) {
			IPath filePath = new Path(nlVariants[i]).append(path);
			result = findInPlugin(b, filePath, multiple);
			if (result != null && multiple == null)
				return result;
			result = findInFragments(b, filePath, multiple);
			if (result != null && multiple == null)
				return result;
		}
		// If we get to this point, we haven't found it yet.
		// Look in the plugin and fragment root directories
		result = findInPlugin(b, path, multiple);
		if (result != null && multiple == null)
			return result;
		return findInFragments(b, path, multiple);
	}

	private static URL findInPlugin(Bundle b, IPath filePath, ArrayList multiple) {
		URL result = b.getEntry(filePath.toString());
        if (result != null && multiple != null)
            multiple.add(result);
        return result;
	}

	private static URL findInFragments(Bundle b, IPath filePath, ArrayList multiple) {
		Bundle[] fragments = Platform.getFragments(b);
		if (fragments == null)
			return null;

        multiple.ensureCapacity(fragments.length + 1);
		URL fileURL = null;
		int i = 0;
		while (i < fragments.length && fileURL == null) {
			fileURL = fragments[i].getEntry(filePath.toString());
            if (fileURL != null && multiple != null) {
                multiple.add(fileURL);
                fileURL = null;
            }
			i++;
		}
		return fileURL;
	}

	/**
	 * See doc on @link Platform#openStream(Bundle, IPath, boolean) Platform#Platform#openStream(Bundle, IPath, boolean) 
	 */
	public static final InputStream openStream(Bundle bundle, IPath file, boolean localized) throws IOException {
		URL url = null;
		if (!localized) {
			url = findInPlugin(bundle, file, null);
			if (url == null)
				url = findInFragments(bundle, file, null);
		} else {
			url = FindSupport.find(bundle, file);
		}
		if (url != null)
			return url.openStream();
		throw new IOException("Cannot find " + file.toString()); //$NON-NLS-1$
	}

}

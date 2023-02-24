/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Michael Allman - Bug 211648, Bug 156343 - Standard VM not supported on MacOS
 *******************************************************************************/
package org.eclipse.jdt.internal.launching;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.AbstractVMInstallType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.osgi.service.environment.Constants;

import com.ibm.icu.text.MessageFormat;

/**
 * A VM install type for VMs the conform to the standard
 * JDK installation layout.
 */
public class StandardVMType extends AbstractVMInstallType {
	
	/**
	 * Constants for common {@link String}s
	 * @since 3.7 
	 */
	private static final String RT_JAR = "rt.jar"; //$NON-NLS-1$
	private static final String SRC = "src"; //$NON-NLS-1$
	private static final String SRC_ZIP = "src.zip"; //$NON-NLS-1$
	private static final String SRC_JAR = "src.jar"; //$NON-NLS-1$
	private static final String JRE = "jre"; //$NON-NLS-1$
	private static final String LIB = "lib"; //$NON-NLS-1$
	private static final String BAR = "|"; //$NON-NLS-1$

	public static final String ID_STANDARD_VM_TYPE = "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType"; //$NON-NLS-1$
	
	/**
	 * The root path for the attached source
	 */
	private String fDefaultRootPath = ""; //$NON-NLS-1$
	
	/**
	 * Map of the install path for which we were unable to generate
	 * the library info during this session.
	 */
	private static Map fgFailedInstallPath = new HashMap();

	/**
	 * Cache for default library locations. See {@link #getDefaultLibraryLocations(File)}
	 * <br><br>
	 * Map&lt;{@link String}, {@link LibraryLocation}&gt;
	 * 
	 * @since 3.7
	 */
	private static Map fgDefaultLibLocs = new HashMap();
	
	/**
	 * The list of locations in which to look for the java executable in candidate
	 * VM install locations, relative to the VM install location.
	 */
	private static final String[] fgCandidateJavaFiles = {"javaw", "javaw.exe", "java", "java.exe", "j9w", "j9w.exe", "j9", "j9.exe"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
	private static final String[] fgCandidateJavaLocations = {"bin" + File.separatorChar, JRE + File.separatorChar + "bin" + File.separatorChar}; //$NON-NLS-1$ //$NON-NLS-2$ 
	
	/**
	 * Starting in the specified VM install location, attempt to find the 'java' executable
	 * file.  If found, return the corresponding <code>File</code> object, otherwise return
	 * <code>null</code>.
	 * @param vmInstallLocation the {@link File} location to look in
	 * @return the {@link File} for the Java executable or <code>null</code>
	 */
	public static File findJavaExecutable(File vmInstallLocation) {
		// Try each candidate in order.  The first one found wins.  Thus, the order
		// of fgCandidateJavaLocations and fgCandidateJavaFiles is significant.
		for (int i = 0; i < fgCandidateJavaFiles.length; i++) {
			for (int j = 0; j < fgCandidateJavaLocations.length; j++) {
				File javaFile = new File(vmInstallLocation, fgCandidateJavaLocations[j] + fgCandidateJavaFiles[i]);
				if (javaFile.isFile()) {
					return javaFile;
				}				
			}
		}		
		return null;							
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstallType#getName()
	 */
	public String getName() {
		return LaunchingMessages.StandardVMType_Standard_VM_3; 
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractVMInstallType#doCreateVMInstall(java.lang.String)
	 */
	protected IVMInstall doCreateVMInstall(String id) {
		return new StandardVM(this, id);
	}
	
	/**
	 * Return library information corresponding to the specified install
	 * location. If the information does not exist, create it using the given Java
	 * executable.
	 * @param javaHome the Java home folder
	 * @param javaExecutable the Java executable file
	 * @return the {@link LibraryInfo} for the home and executable path or an empty object, never <code>null</code>
	 */
	protected synchronized LibraryInfo getLibraryInfo(File javaHome, File javaExecutable) {
		String installPath = javaHome.getAbsolutePath();
		LibraryInfo info = LaunchingPlugin.getLibraryInfo(installPath);
		if (info == null || LaunchingPlugin.timeStampChanged(installPath)) {
			info = (LibraryInfo)fgFailedInstallPath.get(installPath);
			if (info == null) {
				info = generateLibraryInfo(javaHome, javaExecutable);
				if (info == null) {
					info = getDefaultLibraryInfo(javaHome);
					fgFailedInstallPath.put(installPath, info);
				} else {
				    // only persist if we were able to generate information - see bug 70011
				    LaunchingPlugin.setLibraryInfo(installPath, info);
				}
			}
		} 
		return info;
	}	
	
	/**
	 * Return <code>true</code> if the appropriate system libraries can be found for the
	 * specified java executable, <code>false</code> otherwise.
	 * @param javaHome the Java home folder
	 * @param javaExecutable the Java executable file
	 * @return <code>true</code> if the default system libraries can be detected for the given install location
	 * <code>false</code> otherwise
	 */
	protected boolean canDetectDefaultSystemLibraries(File javaHome, File javaExecutable) {
		LibraryLocation[] locations = getDefaultLibraryLocations(javaHome);
		String version = getVMVersion(javaHome, javaExecutable);
		return locations.length > 0 && !version.startsWith("1.1"); //$NON-NLS-1$
	}
	
	/**
	 * Returns the version of the VM at the given location, with the given
	 * executable.
	 * 
	 * @param javaHome the Java home folder
	 * @param javaExecutable the Java executable file
	 * @return String
	 */
	protected String getVMVersion(File javaHome, File javaExecutable) {
		LibraryInfo info = getLibraryInfo(javaHome, javaExecutable);
		return info.getVersion();
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstallType#detectInstallLocation()
	 */
	public File detectInstallLocation() {
		// do not detect on the Mac OS
		if (Platform.getOS().equals(Constants.OS_MACOSX)) {
			return null;
		}		
		
		// Retrieve the 'java.home' system property.  If that directory doesn't exist, 
		// return null.
		File javaHome; 
		try {
			javaHome= new File (System.getProperty("java.home")).getCanonicalFile(); //$NON-NLS-1$
		} catch (IOException e) {
			LaunchingPlugin.log(e);
			return null;
		}
		if (!javaHome.exists()) {
			return null;
		}

		// Find the 'java' executable file under the java home directory.  If it can't be
		// found, return null.
		File javaExecutable = findJavaExecutable(javaHome);
		if (javaExecutable == null) {
			return null;
		}
		
		// If the reported java home directory terminates with 'jre', first see if 
		// the parent directory contains the required libraries
		boolean foundLibraries = false;
		if (javaHome.getName().equalsIgnoreCase(JRE)) { 
			File parent= new File(javaHome.getParent());			
			if (canDetectDefaultSystemLibraries(parent, javaExecutable)) {
				javaHome = parent;
				foundLibraries = true;
			}
		}	
		
		// If we haven't already found the libraries, look in the reported java home dir
		if (!foundLibraries) {
			if (!canDetectDefaultSystemLibraries(javaHome, javaExecutable)) {
				return null;
			}			
		}
		
		return javaHome;
	}

	/**
	 * Return an <code>IPath</code> corresponding to the single library file containing the
	 * standard Java classes for most VMs version 1.2 and above.
	 * 
	 * @param javaHome the Java home folder
	 * @return the {@link IPath} to the <code>rt.jar</code> file
	 */
	protected IPath getDefaultSystemLibrary(File javaHome) {
		IPath jreLibPath= new Path(javaHome.getPath()).append(LIB).append(RT_JAR); 
		if (jreLibPath.toFile().isFile()) {
			return jreLibPath;
		}
		return new Path(javaHome.getPath()).append(JRE).append(LIB).append(RT_JAR); 
	}
	
	/**
	 * Returns a path to the source attachment for the given library, or
	 * an empty path if none.
	 * 
	 * @param libLocation the {@link File} location of the library to find the source for
	 * @return a path to the source attachment for the given library, or
	 *  an empty path if none
	 */
	protected IPath getDefaultSystemLibrarySource(File libLocation) {
		File parent= libLocation.getParentFile();
		while (parent != null) {
			File parentsrc= new File(parent, SRC_JAR);
			if (parentsrc.isFile()) {
				setDefaultRootPath(SRC);
				return new Path(parentsrc.getPath());
			}
			parentsrc= new File(parent, SRC_ZIP);
			if (parentsrc.isFile()) {
				setDefaultRootPath(""); //$NON-NLS-1$
				return new Path(parentsrc.getPath());
			}
			parent = parent.getParentFile();
		}
		// if we didn't find any of the normal source files, look for J9 source
		IPath result = checkForJ9LibrarySource(libLocation);
		if (result != null)
			return result;
		// check for <lib>-src.jar pattern
		IPath libName = new Path(libLocation.getName());
		String extension = libName.getFileExtension();
		String prefix = libName.removeFileExtension().lastSegment();
		if (extension != null) {
			IPath srcPath = new Path(libLocation.getPath());
			srcPath = srcPath.removeLastSegments(1);
			StringBuffer buf = new StringBuffer();
			buf.append(prefix);
			buf.append("-src."); //$NON-NLS-1$
			buf.append(extension);
			srcPath = srcPath.append(buf.toString());
			if (srcPath.toFile().exists()) {
				return srcPath;
			}
		}
		setDefaultRootPath(""); //$NON-NLS-1$
		return Path.EMPTY; 
	}

	// J9 has a known/fixed structure for its libraries and source locations.  Here just
	// look for the source associated with each lib.
	private IPath checkForJ9LibrarySource(File libLocation) {
		File parent= libLocation.getParentFile();
		String name = libLocation.getName();
		if (name.equalsIgnoreCase("classes.zip")) { //$NON-NLS-1$
			File source = new File(parent, "source/source.zip"); //$NON-NLS-1$
			return source.isFile() ? new Path(source.getPath()) : Path.EMPTY;
		}
		if (name.equalsIgnoreCase("locale.zip")) { //$NON-NLS-1$
			File source = new File(parent, "source/locale-src.zip"); //$NON-NLS-1$
			return source.isFile() ? new Path(source.getPath()) : Path.EMPTY;
		}
		if (name.equalsIgnoreCase("charconv.zip")) { //$NON-NLS-1$
			File source = new File(parent, "charconv-src.zip"); //$NON-NLS-1$
			return source.isFile() ? new Path(source.getPath()) : Path.EMPTY;
		}
		return null;
	}

	/**
	 * Returns the package root path
	 * 
	 * @return the package root path
	 */
	protected IPath getDefaultPackageRootPath() {
		return new Path(getDefaultRootPath());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstallType#getDefaultLibraryLocations(java.io.File)
	 */
	public LibraryLocation[] getDefaultLibraryLocations(File installLocation) {
		//NOTE: We do not add libraries from the "endorsed" directory explicitly, as
		//the bootpath contains these entries already (if they exist).
		// Determine the java executable that corresponds to the specified install location
		// and use this to generate library information.  If no java executable was found, 
		// the 'standard' libraries will be returned.
		List allLibs = (List) fgDefaultLibLocs.get(installLocation.getAbsolutePath());
		if(allLibs == null) {
			File javaExecutable = findJavaExecutable(installLocation);
			LibraryInfo libInfo;
			if (javaExecutable == null) {
				libInfo = getDefaultLibraryInfo(installLocation);
			} else {
				libInfo = getLibraryInfo(installLocation, javaExecutable);
			}
			
			// Add all endorsed libraries - they are first, as they replace
			allLibs = new ArrayList(gatherAllLibraries(libInfo.getEndorsedDirs()));
			
			// next is the boot path libraries
			String[] bootpath = libInfo.getBootpath();
			List boot = new ArrayList(bootpath.length);
			URL url = getDefaultJavadocLocation(installLocation);
			for (int i = 0; i < bootpath.length; i++) {
				IPath path = new Path(bootpath[i]);
				File lib = path.toFile(); 
				if (lib.exists() && lib.isFile()) {
					LibraryLocation libraryLocation = new LibraryLocation(path,
									getDefaultSystemLibrarySource(lib),
									getDefaultPackageRootPath(),
									url);
					boot.add(libraryLocation);
				}
			}
			allLibs.addAll(boot);
					
			// Add all extension libraries
			allLibs.addAll(gatherAllLibraries(libInfo.getExtensionDirs()));
			
			//remove duplicates
			HashSet set = new HashSet();
			LibraryLocation lib = null;
			for(ListIterator liter = allLibs.listIterator(); liter.hasNext();) {
				lib = (LibraryLocation) liter.next();
				IPath systemLibraryPath = lib.getSystemLibraryPath();
				String device = systemLibraryPath.getDevice();
				if (device != null) {
					// @see Bug 197866 - Installed JRE Wizard creates duplicate system libraries when drive letter is lower case
					systemLibraryPath = systemLibraryPath.setDevice(device.toUpperCase());
				}
				if(!set.add(systemLibraryPath.toOSString())) {
					//did not add it, duplicate
					liter.remove();
				}
			}
			fgDefaultLibLocs.put(installLocation.getAbsolutePath(), allLibs);
		}
		return (LibraryLocation[])allLibs.toArray(new LibraryLocation[allLibs.size()]);
	}
	
	/**
	 * Returns default library information for the given install location.
	 * 
	 * @param installLocation the VM install location
	 * @return LibraryInfo
	 */
	protected LibraryInfo getDefaultLibraryInfo(File installLocation) {
		IPath rtjar = getDefaultSystemLibrary(installLocation);
		File extDir = getDefaultExtensionDirectory(installLocation);
		File endDir = getDefaultEndorsedDirectory(installLocation);
		String[] dirs = null;
		if (extDir == null) {
			dirs = new String[0];
		} else {
			dirs = new String[] {extDir.getAbsolutePath()};
		}
		String[] endDirs = null;
		if (endDir == null) {
			endDirs = new String[0]; 
		} else {
			endDirs = new String[] {endDir.getAbsolutePath()};
		}
		return new LibraryInfo("???", new String[] {rtjar.toOSString()}, dirs, endDirs);		 //$NON-NLS-1$
	}
	
	/**
	 * Returns a list of all zip's and jars contained in the given directories.
	 * 
	 * @param dirPaths a list of absolute paths of directories to search
	 * @return List of all zip's and jars
	 */
	public static List gatherAllLibraries(String[] dirPaths) {
		List libraries = new ArrayList();
		for (int i = 0; i < dirPaths.length; i++) {
			File extDir = new File(dirPaths[i]);
			if (extDir.exists() && extDir.isDirectory()) {
				String[] names = extDir.list();
				if (names != null) {
					for (int j = 0; j < names.length; j++) {
						String name = names[j];
						File jar = new File(extDir, name);
						if (jar.isFile()) {
							int length = name.length();
							if (length > 4) {
								String suffix = name.substring(length - 4);
								if (suffix.equalsIgnoreCase(".zip") || suffix.equalsIgnoreCase(".jar")) { //$NON-NLS-1$ //$NON-NLS-2$
									try {
										IPath libPath = new Path(jar.getCanonicalPath());
										LibraryLocation library = new LibraryLocation(libPath, Path.EMPTY, Path.EMPTY, null);
										libraries.add(library);
									} catch (IOException e) {
										LaunchingPlugin.log(e);
									}
								}
							}
						}
					}
				}
			}			
		}
		return libraries;
	}
		
	/**
	 * Returns the default location of the extension directory, based on the given
	 * install location. The resulting file may not exist, or be <code>null</code>
	 * if an extension directory is not supported.
	 * 
	 * @param installLocation the VM install location
	 * @return default extension directory or <code>null</code>
	 */
	protected File getDefaultExtensionDirectory(File installLocation) {
		File jre = null;
		if (installLocation.getName().equalsIgnoreCase(JRE)) { 
			jre = installLocation;
		} else {
			jre = new File(installLocation, JRE); 
		}
		File lib = new File(jre, LIB); 
		File ext = new File(lib, "ext"); //$NON-NLS-1$
		return ext;
	}

	/**
	 * Returns the default location of the endorsed directory, based on the
	 * given install location. The resulting file may not exist, or be
	 * <code>null</code> if an endorsed directory is not supported.
	 * 
	 * @param installLocation the VM install location
	 * @return default endorsed directory or <code>null</code>
	 */
	protected File getDefaultEndorsedDirectory(File installLocation) {
		File lib = new File(installLocation, LIB); 
		File ext = new File(lib, "endorsed"); //$NON-NLS-1$
		return ext;
	}

	protected String getDefaultRootPath() {
		return fDefaultRootPath;
	}

	protected void setDefaultRootPath(String defaultRootPath) {
		fDefaultRootPath = defaultRootPath;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstallType#validateInstallLocation(java.io.File)
	 */
	public IStatus validateInstallLocation(File javaHome) {
		IStatus status = null;
		File javaExecutable = findJavaExecutable(javaHome);
		if (javaExecutable == null) {
			status = new Status(IStatus.ERROR, LaunchingPlugin.getUniqueIdentifier(), 0, LaunchingMessages.StandardVMType_Not_a_JDK_Root__Java_executable_was_not_found_1, null); //			
		} else {
			if (canDetectDefaultSystemLibraries(javaHome, javaExecutable)) {
				status = new Status(IStatus.OK, LaunchingPlugin.getUniqueIdentifier(), 0, LaunchingMessages.StandardVMType_ok_2, null); 
			} else {
				status = new Status(IStatus.ERROR, LaunchingPlugin.getUniqueIdentifier(), 0, LaunchingMessages.StandardVMType_Not_a_JDK_root__System_library_was_not_found__1, null); 
			}
		}
		return status;		
	}

	/**
	 * Generates library information for the given java executable. A main
	 * program is run (<code>org.eclipse.jdt.internal.launching.support.
	 * LibraryDetector</code>), that dumps the system properties for bootpath
	 * and extension directories. This output is then parsed and cached for
	 * future reference.
	 * 
	 * @param javaHome the Java home folder
	 * @param javaExecutable the Java executable file
	 * 
	 * @return library info or <code>null</code> if none
	 */	
	protected LibraryInfo generateLibraryInfo(File javaHome, File javaExecutable) {
		LibraryInfo info = null;
		
		// if this is 1.1.X, the properties will not exist		
		IPath classesZip = new Path(javaHome.getAbsolutePath()).append(LIB).append("classes.zip"); //$NON-NLS-1$ 
		if (classesZip.toFile().exists()) {
			return new LibraryInfo("1.1.x", new String[] {classesZip.toOSString()}, new String[0], new String[0]); //$NON-NLS-1$
		}
		//locate the launching support jar - it contains the main program to run
		File file = LaunchingPlugin.getFileInPlugin(new Path("lib/launchingsupport.jar")); //$NON-NLS-1$
		if (file != null && file.exists()) {
			String javaExecutablePath = javaExecutable.getAbsolutePath();
			String[] cmdLine = new String[] {javaExecutablePath, "-classpath", file.getAbsolutePath(), "org.eclipse.jdt.internal.launching.support.LibraryDetector"};  //$NON-NLS-1$ //$NON-NLS-2$
			Process p = null;
			try {
				String envp[] = null;
				if (Platform.OS_MACOSX.equals(Platform.getOS())) {
					Map map = DebugPlugin.getDefault().getLaunchManager().getNativeEnvironmentCasePreserved();
					if (map.remove(StandardVMDebugger.JAVA_JVM_VERSION) != null) {
						envp = new String[map.size()];
						Iterator iterator = map.entrySet().iterator();
						int i = 0;
						while (iterator.hasNext()) {
							Entry entry = (Entry) iterator.next();
							envp[i] = (String)entry.getKey() + "=" + (String)entry.getValue(); //$NON-NLS-1$
							i++;
						}
					}
				}
				p = DebugPlugin.exec(cmdLine, null, envp);
				IProcess process = DebugPlugin.newProcess(new Launch(null, ILaunchManager.RUN_MODE, null), p, "Library Detection"); //$NON-NLS-1$
				for (int i= 0; i < 600; i++) {
					// Wait no more than 30 seconds (600 * 50 milliseconds)
					if (process.isTerminated()) {
						break;
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
				}
				info = parseLibraryInfo(process);
			} catch (CoreException ioe) {
				LaunchingPlugin.log(ioe);
			} finally {
				if (p != null) {
					p.destroy();
				}
			}
		}
		if (info == null) {
		    // log error that we were unable to generate library information - see bug 70011
		    LaunchingPlugin.log(MessageFormat.format("Failed to retrieve default libraries for {0}", new String[]{javaHome.getAbsolutePath()})); //$NON-NLS-1$
		}
		return info;
	}
	
	/**
	 * Parses the output from 'LibraryDetector'.
	 * 
	 * @param process the backing {@link IProcess} that was run
	 * @return the new {@link LibraryInfo} object or <code>null</code>
	 */
	protected LibraryInfo parseLibraryInfo(IProcess process) {
		IStreamsProxy streamsProxy = process.getStreamsProxy();
		String text = null;
		if (streamsProxy != null) {
			text = streamsProxy.getOutputStreamMonitor().getContents();
		}
		if (text != null && text.length() > 0) {
			int index = text.indexOf(BAR); 
			if (index > 0) { 
				String version = text.substring(0, index);
				text = text.substring(index + 1);
				index = text.indexOf(BAR); 
				if (index > 0) {
					String bootPaths = text.substring(0, index);
					String[] bootPath = parsePaths(bootPaths);
					 
					text = text.substring(index + 1);
					index = text.indexOf(BAR); 
					
					if (index > 0) {
						String extDirPaths = text.substring(0, index);
						String endorsedDirsPath = text.substring(index + 1);
						String[] extDirs = parsePaths(extDirPaths);
						String[] endDirs = parsePaths(endorsedDirsPath);
						return new LibraryInfo(version, bootPath, extDirs, endDirs);
					} 
				}
			}
		} 
		return null;
	}
	
	protected String[] parsePaths(String paths) {
		List list = new ArrayList();
		int pos = 0;
		int index = paths.indexOf(File.pathSeparatorChar, pos);
		while (index > 0) {
			String path = paths.substring(pos, index);
			list.add(path);
			pos = index + 1;	
			index = paths.indexOf(File.pathSeparatorChar, pos);
		}
		String path = paths.substring(pos);
		if (!path.equals("null")) { //$NON-NLS-1$
			list.add(path);
		}
		return (String[])list.toArray(new String[list.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstallType#disposeVMInstall(java.lang.String)
	 */
	public void disposeVMInstall(String id) {
		IVMInstall vm = findVMInstall(id);
		if (vm != null) {
			String path = vm.getInstallLocation().getAbsolutePath();
            LaunchingPlugin.setLibraryInfo(path, null);
            fgFailedInstallPath.remove(path);
            fgDefaultLibLocs.remove(path);
		}		
		super.disposeVMInstall(id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractVMInstallType#getDefaultJavadocLocation(java.io.File)
	 */
	public URL getDefaultJavadocLocation(File installLocation) {
		File javaExecutable = findJavaExecutable(installLocation);
		if (javaExecutable != null) {
			LibraryInfo libInfo = getLibraryInfo(installLocation, javaExecutable);
			if (libInfo != null) {
				String version = libInfo.getVersion();
				return getDefaultJavadocLocation(version);
			}
		}
		return null;
	}

	/**
	 * Returns a default Javadoc location for a language version, or <code>null</code>.
	 * 
	 * @param version language version such as "1.4"
	 * @return URL to default Javadoc location, or <code>null</code>
	 */
	public static URL getDefaultJavadocLocation(String version) {
		try {
			if (version.startsWith(JavaCore.VERSION_1_7)) {
				return new URL("http://download.oracle.com/javase/7/docs/api/"); //$NON-NLS-1$
			} else if (version.startsWith(JavaCore.VERSION_1_6)) {
				return new URL("http://download.oracle.com/javase/6/docs/api/"); //$NON-NLS-1$
			} else if (version.startsWith(JavaCore.VERSION_1_5)) {
				return new URL("http://download.oracle.com/javase/1.5.0/docs/api/"); //$NON-NLS-1$
			} else if (version.startsWith(JavaCore.VERSION_1_4)) {
				return new URL("http://download.oracle.com/javase/1.4.2/docs/api/"); //$NON-NLS-1$
			} else if (version.startsWith(JavaCore.VERSION_1_3)) {
				return new URL("http://download.oracle.com/javase/1.3/docs/api/"); //$NON-NLS-1$
			}
		} catch (MalformedURLException e) {
		}
		return null;
	}

}

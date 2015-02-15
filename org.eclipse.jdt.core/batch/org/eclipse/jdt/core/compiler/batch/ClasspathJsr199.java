/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.batch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ClasspathJsr199 extends ClasspathLocation {
	private static final Set<JavaFileObject.Kind> fileTypes = new HashSet<JavaFileObject.Kind>();

	static {
		fileTypes.add(JavaFileObject.Kind.CLASS);
	}

	private JavaFileManager fileManager;
	private JavaFileManager.Location location;
	private boolean mUsePegaFileManagerOptimizations;

	public ClasspathJsr199(JavaFileManager file, JavaFileManager.Location location) {
		super(null, null);
		this.fileManager = file;
		this.location = location;
	}

	public List fetchLinkedJars(FileSystem.ClasspathSectionProblemReporter problemReporter) {
		// System.err.println("fetchLinkedJars");
		// Assume no linked jars
		return null;
	}

	public NameEnvironmentAnswer findClass(char[] typeName, String qualifiedPackageName, String qualifiedBinaryFileName) {
		return findClass(typeName, qualifiedPackageName, qualifiedBinaryFileName, false);
	}

	public NameEnvironmentAnswer findClass(char[] typeName, String qualifiedPackageName, String aQualifiedBinaryFileName,
			boolean asBinaryOnly) {

		String qualifiedBinaryFileName = File.separatorChar == '/'
				? aQualifiedBinaryFileName
				: aQualifiedBinaryFileName.replace(File.separatorChar, '/');

		try {
			int lastDot = qualifiedBinaryFileName.lastIndexOf('.');
			String className = lastDot < 0 ? qualifiedBinaryFileName : qualifiedBinaryFileName.substring(0, lastDot);
			JavaFileObject jfo;
			try {
				jfo = fileManager.getJavaFileForInput(location, className, JavaFileObject.Kind.CLASS);
			} catch (IOException e1) {
				e1.printStackTrace();
				return null;
			}

			if (jfo == null)
				return null; // most common case
	
			ClassFileReader reader = ClassFileReader.read(jfo.openInputStream(), qualifiedBinaryFileName);
			if (reader != null) {
				return new NameEnvironmentAnswer(reader, fetchAccessRestriction(qualifiedBinaryFileName));
			}
		} catch (ClassFormatException e) {
			// treat as if class file is missing
		} catch (IOException e) {
			// treat as if class file is missing
		}
		return null;
	}

	public char[][][] findTypeNames(String aQualifiedPackageName) {
		String qualifiedPackageName = File.separatorChar == '/' ? aQualifiedPackageName : aQualifiedPackageName.replace(
				File.separatorChar, '/');
		//System.err.println("findTypeNames: " + qualifiedPackageName);

		Iterable<JavaFileObject> files;
		try {
			files = fileManager.list(location, qualifiedPackageName, fileTypes, false);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		ArrayList answers = new ArrayList();
		char[][] packageName = CharOperation.splitOn(File.separatorChar, qualifiedPackageName.toCharArray());

		for (JavaFileObject file : files) {
			String fileName = file.toUri().getPath();

			int last = fileName.lastIndexOf('/');
			if (last > 0) {
				int indexOfDot = fileName.lastIndexOf('.');
				if (indexOfDot != -1) {
					String typeName = fileName.substring(last + 1, indexOfDot);
					answers.add(CharOperation.arrayConcat(packageName, typeName.toCharArray()));
				}
			}
		}
		int size = answers.size();
		if (size != 0) {
			char[][][] result = new char[size][][];
			answers.toArray(result);
			return result;
		}
		return null;
	}

	public void initialize() throws IOException {
	}

	public boolean isPackage(String aQualifiedPackageName) {
		String qualifiedPackageName = File.separatorChar == '/' ? aQualifiedPackageName : aQualifiedPackageName.replace(
				File.separatorChar, '/');

		boolean result = false;
		try {
			Iterable<JavaFileObject> files = fileManager.list(location, qualifiedPackageName, fileTypes, false);
			Iterator f = files.iterator();
			// if there is any content, assume a package
			if (f.hasNext()) {
				result = true;
			} else {
				// I hate to do this since it is expensive and will throw off garbage
				// but can't think of an alternative now
				files = fileManager.list(location, qualifiedPackageName, fileTypes, true);
				f = files.iterator();
				// if there is any content, assume a package
				if (f.hasNext()) {
					result = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public void reset() {
		// should this reset the JavaFileManger?
		try {
			fileManager.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String toString() {
		return "Classpath for Jsr 199 JavaFileManager: " + location; //$NON-NLS-1$
	}

	public char[] normalizedPath() {
		// System.err.println("normalizedPath");
		if (this.normalizedPath == null) {
			this.normalizedPath = this.path.toCharArray();
		}
		return this.normalizedPath;
	}

	public String getPath() {
		// System.err.println("getPath");
		if (this.path == null) {
			this.path = location.getName();
		}
		return this.path;
	}

	public int getMode() {
		return BINARY;
	}
}

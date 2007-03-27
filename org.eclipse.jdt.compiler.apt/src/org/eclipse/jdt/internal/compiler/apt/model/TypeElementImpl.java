/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.apt.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;

public class TypeElementImpl extends ElementImpl implements TypeElement {
	
	/* package */ TypeElementImpl(ReferenceBinding binding) {
		super(binding);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public <R, P> R accept(ElementVisitor<R, P> v, P p)
	{
		return v.visitType(this, p);
	}

	@Override
	public List<? extends AnnotationMirror> getAnnotationMirrors() {
		AnnotationBinding[] annotations = ((ReferenceBinding)_binding).getAnnotations();
		return getAnnotationMirrors(annotations);
	}

	@Override
	public List<? extends Element> getEnclosedElements() {
		ReferenceBinding binding = (ReferenceBinding)_binding;
		List<Element> enclosed = new ArrayList<Element>(binding.fieldCount() + binding.methods().length);
		for (MethodBinding method : binding.methods()) {
			ExecutableElement executable = new ExecutableElementImpl(method);
			enclosed.add(executable);
		}
		for (FieldBinding field : binding.fields()) {
			 VariableElement variable = new VariableElementImpl(field);
			 enclosed.add(variable);
		}
		for (ReferenceBinding memberType : binding.memberTypes()) {
			TypeElement type = new TypeElementImpl(memberType);
			enclosed.add(type);
		}
		return Collections.unmodifiableList(enclosed);
	}

	@Override
	public Element getEnclosingElement() {
		ReferenceBinding binding = (ReferenceBinding)_binding;
		ReferenceBinding enclosingType = binding.enclosingType();
		if (null == enclosingType) {
			// this is a top level type; get its package
			return new PackageElementImpl(binding.fPackage);
		}
		else {
			return Factory.newElement(binding.enclosingType());
		}
	}

	@Override
	public String getFileName() {
		char[] name = ((ReferenceBinding)_binding).getFileName();
		if (name == null)
			return null;
		return new String(name);
	}
	
	@Override
	public List<? extends TypeMirror> getInterfaces() {
		ReferenceBinding binding = (ReferenceBinding)_binding;
		if (null == binding.superInterfaces() || binding.superInterfaces().length == 0) {
			return Collections.emptyList();
		}
		List<TypeMirror> interfaces = new ArrayList<TypeMirror>(binding.superInterfaces().length);
		for (ReferenceBinding interfaceBinding : binding.superInterfaces()) {
			TypeMirror interfaceType = Factory.newTypeMirror(interfaceBinding);
			interfaces.add(interfaceType);
		}
		return Collections.unmodifiableList(interfaces);
	}

	@Override
	public ElementKind getKind() {
		ReferenceBinding refBinding = (ReferenceBinding)_binding;
		// The order of these comparisons is important: e.g., enum is subset of class
		if (refBinding.isEnum()) {
			return ElementKind.ENUM;
		}
		else if (refBinding.isAnnotationType()) {
			return ElementKind.ANNOTATION_TYPE;
		}
		else if (refBinding.isInterface()) {
			return ElementKind.INTERFACE;
		}
		else if (refBinding.isClass()) {
			return ElementKind.CLASS;
		}
		else {
			throw new IllegalArgumentException("TypeElement " + new String(refBinding.shortReadableName()) +  //$NON-NLS-1$
					" has unexpected attributes " + refBinding.modifiers); //$NON-NLS-1$
		}
	}

	@Override
	public Set<Modifier> getModifiers()
	{
		ReferenceBinding refBinding = (ReferenceBinding)_binding;
		return Factory.getModifiers(refBinding.modifiers);
	}

	@Override
	public NestingKind getNestingKind() {
		ReferenceBinding refBinding = (ReferenceBinding)_binding;
		if (refBinding.isAnonymousType()) {
			return NestingKind.ANONYMOUS;
		} else if (refBinding.isLocalType()) {
			return NestingKind.LOCAL;
		} else if (refBinding.isMemberType()) {
			return NestingKind.MEMBER;
		}
		return NestingKind.TOP_LEVEL;
	}

	@Override
	public Name getQualifiedName() {
		ReferenceBinding binding = (ReferenceBinding)_binding;
		//TODO: what is the right way to get this (including member types, parameterized types, ...?
		return new NameImpl(CharOperation.concatWith(binding.compoundName, '.'));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.apt.model.ElementImpl#getSimpleName()
	 * @return last segment of name, e.g. for pa.pb.X.Y return Y.
	 */
	@Override
	public Name getSimpleName()
	{
		ReferenceBinding binding = (ReferenceBinding)_binding;
		return new NameImpl(binding.sourceName());
	}

	@Override
	public TypeMirror getSuperclass() {
		ReferenceBinding binding = (ReferenceBinding)_binding;
		ReferenceBinding superBinding = binding.superclass();
		if (null == superBinding) {
			return null;
		}
		// superclass of a type must be a DeclaredType
		return Factory.newDeclaredType(superBinding);
	}
	
	@Override
	public List<? extends TypeParameterElement> getTypeParameters() {
		ReferenceBinding binding = (ReferenceBinding)_binding;
		TypeVariableBinding[] variables = binding.typeVariables();
		if (variables.length == 0) {
			return Collections.emptyList();
		}
		List<TypeParameterElement> params = new ArrayList<TypeParameterElement>(variables.length); 
		for (TypeVariableBinding variable : variables) {
			params.add(Factory.newTypeParameterElement(variable, this));
		}
		return Collections.unmodifiableList(params);
	}

	@Override
	public String toString() {
		return new String(_binding.readableName());
	}

}

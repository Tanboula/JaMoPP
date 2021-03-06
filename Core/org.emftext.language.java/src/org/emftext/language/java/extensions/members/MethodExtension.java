/*******************************************************************************
 * Copyright (c) 2006-2014
 * Software Technology Group, Dresden University of Technology
 * DevBoost GmbH, Berlin, Amtsgericht Charlottenburg, HRB 140026
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Software Technology Group - TU Dresden, Germany;
 *   DevBoost GmbH - Berlin, Germany
 *      - initial API and implementation
 ******************************************************************************/
package org.emftext.language.java.extensions.members;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.emftext.language.java.classifiers.ConcreteClassifier;
import org.emftext.language.java.expressions.Expression;
import org.emftext.language.java.members.Method;
import org.emftext.language.java.parameters.Parameter;
import org.emftext.language.java.parameters.VariableLengthParameter;
import org.emftext.language.java.references.MethodCall;
import org.emftext.language.java.types.Type;

public class MethodExtension {
	
	/**
	 * Decides if the given method matches the given call. 
	 * 
	 * @param methodCall
	 * @return
	 */
	public static boolean isSomeMethodForCall(Method me, MethodCall methodCall) {
		return me.isMethodForCall(methodCall, false);
	}

	/**
	 * Only returns true if the given Method is a better match for the given calls than the
	 * otherMethod given.
	 * 
	 * @param otherMethod
	 * @param methodCall
	 * @return
	 */
	public static boolean isBetterMethodForCall(Method me, Method otherMethod, MethodCall methodCall) {
		if (!me.isMethodForCall(methodCall, false)) {
			return false;
		}
		if (otherMethod.isMethodForCall(methodCall, true)) {
			if (me.isMethodForCall(methodCall, true)) {
				//we both match perfectly; lets compare our return types
				Type target = me.getTypeReference().getTarget();
				if (target instanceof ConcreteClassifier) {
					if (((ConcreteClassifier) target).getAllSuperClassifiers().contains(
							otherMethod.getTypeReference().getTarget())) {
						// I am the more concrete type
						return true;
					}
				}
			}
			//the other already matches perfectly; I am not better
			return false;
		}
		if (!otherMethod.isMethodForCall(methodCall, false)) {
			//I match, but the other does not
			return true;
		}
		//we both match, I am only better if I match perfectly <- 
		//TODO #763: this is not enough: we need to check for "nearest subtype" here
		return me.isMethodForCall(methodCall, true);
	}

	public static boolean isMethodForCall(Method me, MethodCall methodCall, boolean needsPerfectMatch) {
		EList<Type> argumentTypeList = methodCall.getArgumentTypes();
		EList<Parameter> parameterList = new BasicEList<Parameter>(me.getParameters());
		
		EList<Type> parameterTypeList = new BasicEList<Type>();
		for(Parameter parameter : parameterList)  {
			//determine types before messing with the parameters
			parameterTypeList.add(
					parameter.getTypeReference().getBoundTarget(methodCall));
		}

		if (!parameterList.isEmpty()) {
			Parameter lastParameter = parameterList.get(parameterList.size() - 1);
			Type lastParameterType  = parameterTypeList.get(parameterTypeList.size() - 1);;
			if (lastParameter instanceof VariableLengthParameter) {
				//in case of variable length add/remove some parameters
				while(parameterList.size() < argumentTypeList.size()) {
					if (needsPerfectMatch) return false;
					parameterList.add(lastParameter);
					parameterTypeList.add(lastParameterType);
				}
				if(parameterList.size() > argumentTypeList.size()) {
					if (needsPerfectMatch) return false;
					parameterList.remove(lastParameter);
					parameterTypeList.remove(parameterTypeList.size() - 1);
				}
				
			}
		}
		
		if (parameterList.size() == argumentTypeList.size()) { 
			boolean parametersMatch = true;
			for (int i = 0; i < argumentTypeList.size(); i++) {
				Parameter  parameter = parameterList.get(i);
				Expression argument = methodCall.getArguments().get(i);

				Type parameterType = parameterTypeList.get(i);
				Type argumentType  = argumentTypeList.get(i);
				
				if (argumentType == null || parameterType == null) {
					return false;
				}
				
				if (parameterType != null && argumentType != null) {
					if (!parameterType.eIsProxy() || !argumentType.eIsProxy()) {
						if (needsPerfectMatch) {
							parametersMatch = parametersMatch
								&& argumentType.equalsType(argument.getArrayDimension(),
										parameterType, parameter.getArrayDimension());
						}
						else {
							parametersMatch = parametersMatch 
								&& argumentType.isSuperType(argument.getArrayDimension(),
										parameterType, parameter);
						}
					}
					else {
						return false;
					}
				}
				else {
					return false;
				}
			}
			return parametersMatch; 
		} 
		return false;		
	}

	public static long getArrayDimension(Method me) {
		long size = me.getArrayDimensionsBefore().size() + me.getArrayDimensionsAfter().size();
		if (me instanceof VariableLengthParameter) {
			size++;
		}
		return size;
	}
}

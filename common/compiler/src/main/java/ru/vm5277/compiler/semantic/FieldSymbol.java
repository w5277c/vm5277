/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.vm5277.compiler.semantic;

import ru.vm5277.common.VarType;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.FieldNode;

public class FieldSymbol extends AstHolder {
	private	final	CIScope			scope;
	private			AstNode			astNode;
	private			boolean			isPrivate;
	private			int[]			arrayDimensions	= null;
	
	public FieldSymbol(String name, VarType returnType, boolean isFinal, boolean isStatic, boolean isPrivate, CIScope scope, FieldNode astNode) {
		super(name, returnType, isFinal, isStatic, false);
		
		
		this.scope = scope;
		this.astNode = astNode;
		this.isPrivate = isPrivate;
	}

	public CIScope getScope() {
		return scope;
	}
	
	@Override
	public void setNode(AstNode astNode) {
		this.astNode = astNode;
	}
	@Override
	public AstNode getNode() {
		return astNode;
	}
	
	public boolean isPrivate() {
		return isPrivate;
	}
	
	@Override
	public String toString() {
		return	(isNative ? "native " : "") + (isFinal ? "final " : "") + (isStatic ? "static " : "") + type + " " + scope.getName() + "." + name;
	}
}
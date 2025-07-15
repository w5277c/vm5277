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

import ru.vm5277.common.compiler.VarType;
import ru.vm5277.compiler.nodes.AstNode;

public class VarSymbol extends Symbol implements AstHolder {
	private	final	Scope	scope;
	private			AstNode	astNode;
	
	public VarSymbol(String name, VarType returnType, boolean isFinal, boolean isStatic, Scope scope, AstNode astNode) {
		super(name, returnType, isFinal, isStatic);
		
		this.scope = scope;
		this.astNode = astNode;
	}

	public Scope getScope() {
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

	@Override
	public String toString() {
		String parentName = "";
		if(scope.getParent() instanceof ClassScope) parentName = ((ClassScope)scope.getParent()).getName();
		if(scope.getParent() instanceof MethodScope) parentName = ((MethodScope)scope.getParent()).getSymbol().getName();
		
		return	(isNative ? "native " : "") + (isFinal ? "final " : "") + (isStatic ? "static " : "") + type + " " + parentName + "." + name;
	}
}
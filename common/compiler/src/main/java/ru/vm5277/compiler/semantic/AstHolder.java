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

public abstract class AstHolder extends Symbol {
	protected	int	accessCntr	= 0;
	
	public AstHolder(String name, VarType type, boolean isFinal, boolean isStatic, boolean isNative) {
		super(name, type, isFinal, isStatic, isNative);
	}
	
	public abstract void setNode(AstNode node);
	public abstract AstNode getNode();
	
	public void markUsed() {
		accessCntr++;
	}
	public int getAccessCntr() {
		return accessCntr;
	}
}

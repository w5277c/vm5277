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

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.nodes.commands.CommandNode;

public class LabelSymbol extends Symbol {
	private	Scope				scope;
	private	CGLabelScope		cgLabelScope;
	private List<CommandNode>	references	= new ArrayList<>();

	public LabelSymbol(String name, Scope scope) {
		super(name);
		this.scope = scope;
	}
	
	public void setCGScopes(CGScope cgScope, CGLabelScope cgLabelScope) {
		this.cgScope = cgScope;
		this.cgLabelScope = cgLabelScope;
	}
	
	public CGLabelScope getCGLabelScope() {
		return cgLabelScope;
	}
	
	public Scope getScope() {
		return scope;
	}
	
	public void addReference(CommandNode node) {
		references.add(node);
	}

	public List<CommandNode> getReferences() {
		return references;
	}

	public boolean isUsed() {
		return !references.isEmpty();
	}
}
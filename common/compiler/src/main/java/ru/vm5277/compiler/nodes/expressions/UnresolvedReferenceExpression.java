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

package ru.vm5277.compiler.nodes.expressions;

import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;

public class UnresolvedReferenceExpression extends ExpressionNode {
	private	String	id;

	public UnresolvedReferenceExpression(TokenBuffer tb, MessageContainer mc, String id) {
		super(tb, mc);
		
		this.id = id;
	}
	
	@Override
	public VarType getType(Scope scope) throws CompileException {
		return null;
	}
	
	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
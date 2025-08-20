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

import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.ClassSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class ThisExpression extends ExpressionNode {
	private	ClassScope	scope;
	
	public ThisExpression(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
	}
	
	@Override
	public boolean preAnalyze() {
		return true;
	}
	
	@Override
	public VarType getType(Scope scope) {
		return VarType.fromClassName(scope.getThis().getName());
	}
	
	@Override
	public boolean declare(Scope scope) {
		this.scope = scope.getThis();
		symbol = new ClassSymbol(this.scope.getName(), VarType.fromClassName(this.scope.getName()), true, false, this.scope);
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}

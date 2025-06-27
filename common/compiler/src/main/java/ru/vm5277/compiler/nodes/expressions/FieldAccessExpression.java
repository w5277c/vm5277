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

import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class FieldAccessExpression extends ExpressionNode {
	private	final	ExpressionNode	target;
	private	final	String			fieldName;
	
	private			String			className;
	private			Symbol			fieldSymbol;
	
	public FieldAccessExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode target, String fieldName) {
		super(tb, mc);
		this.target = target;
		this.fieldName = fieldName;
	}

	public ExpressionNode getTarget() {
		return target;
	}

	public String getClassName() {
		return className;
	}
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public String getNodeType() {
		return "member access";
	}

	@Override
	public VarType getType(Scope scope) throws SemanticException {
		resolve(scope);
		return null == fieldSymbol ? null : fieldSymbol.getType();
	}

	public Symbol getSymbol() {
		return fieldSymbol;
	}
	
	public Object getValue() {
		return null == fieldSymbol ? null : fieldSymbol;
	}
	
	// Реализация остальных методов (getType, analyze и т.д.)
	
	// TODO поидее этот код метода declare
	private void resolve(Scope scope) throws SemanticException {
		if(null == fieldSymbol) {
			if(target instanceof VariableExpression) {
				VariableExpression ve = (VariableExpression)target;
				className = ve.getValue();
				ClassScope classScope = scope.getThis().resolveClass(ve.getValue());
				if(null != classScope) {
					fieldSymbol = classScope.resolve(fieldName);
				}
				else {
					throw new SemanticException("Can't resolve class scope:" + ve.getValue());
				}
			}
			else {
				throw new SemanticException("Not supported expression:" + target.toString() + ", type:"+ target.getNodeType());
			}
		}
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
	}
}
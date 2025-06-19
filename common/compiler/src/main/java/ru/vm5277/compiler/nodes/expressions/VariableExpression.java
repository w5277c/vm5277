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

import ru.vm5277.common.compiler.CodeGenerator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class VariableExpression extends ExpressionNode {
    private final	String	value;
    private			Symbol	symbol;
	
    public VariableExpression(TokenBuffer tb, MessageContainer mc, String value) {
        super(tb, mc);
        
		this.value = value;
    }
    
	public String getValue() {
		return value;
	}
	
	@Override
	public VarType getType(Scope scope) {
		if (symbol == null) {
			symbol = scope.resolve(value);
			if(null == symbol) {
				ClassScope classScope = scope.getThis().resolveClass(value);
				if(null != classScope) {
					symbol = new Symbol(value, VarType.fromClassName(value), false, false);
				}
			}
        }
        return null == symbol ? null : symbol.getType();
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + value;
	}
	
	@Override
	public boolean preAnalyze() {
		if (null == value || value.isEmpty()) {
			markError("Variable name cannot be empty");
			return false;
		}
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		if (symbol == null) {
            symbol = scope.resolve(value);
			if(null == symbol) {
				ClassScope classScope = scope.getThis().resolveClass(value);
				if(null != classScope) {
					symbol = new Symbol(value, VarType.fromClassName(value), false, false);
				}
			}
        }
		if (null == symbol) {
			markError("Variable '" + value + "' is not declared");
			return false;
		}
		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) {
		cg.loadAcc(symbol.getRuntimeId());
	}
	
	public Symbol getSymbol() {
		return symbol;
	}
}
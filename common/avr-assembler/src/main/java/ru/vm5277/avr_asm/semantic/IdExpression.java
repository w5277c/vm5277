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
package ru.vm5277.avr_asm.semantic;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.scope.VariableSymbol;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;

public class IdExpression extends Expression {
    private	Scope	scope;
	private	String	name;
	private	Long	value;
	
    public IdExpression(TokenBuffer tb, Scope scope, MessageContainer mc, String name) throws CompileException {
        this.scope = scope;
		this.name = name;
    }
    
	public Long getNumericValue() throws CompileException {
		if(null==value) {
			VariableSymbol symbol = scope.resolveVariable(name);
			if(null!=symbol) {
				value = symbol.getValue();
			}
			else {
				Integer addr = scope.resolveLabel(name);
				if(null!=addr) {
					value = (long)addr;
				}
			}
		}
		return value;
	}
	
	public String getId() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
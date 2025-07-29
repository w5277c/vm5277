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
package ru.vm5277.avr_asm.nodes;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.scope.VariableSymbol;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.Operator;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;

public class EquNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws CompileException {
		String name = ((String)Node.consumeToken(tb, TokenType.ID).getValue()).toLowerCase();
		Node.consumeToken(tb, Operator.ASSIGN);
		SourcePosition sp = tb.getSP();
		Expression expr = Expression.parse(tb, scope, mc);
		Long value = Expression.getLong(expr, tb.getSP());
		if(null == value) {
			tb.skipLine();
			throw new CompileException("Unable to resolve constant: '" + expr + "'", sp);
		}
		scope.setVariable(new VariableSymbol(name, value, true), tb.getSP());
		
		scope.list(".EQU " + name + " = " + value);
		
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}

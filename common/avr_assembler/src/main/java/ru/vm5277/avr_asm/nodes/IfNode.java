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
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class IfNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws CompileException, CriticalParseException {
		Long value = 0x00l;
		SourcePosition sp = tb.getSP();
		
		Expression expr = Expression.parse(tb, scope, mc);
		if(!scope.getIncludeSymbol().isBlockSkip()) {
			Long _value = Expression.getLong(expr, sp);
			if(null == _value) {
				mc.add(new ErrorMessage("Could not resolve condition: '" + expr + "'", sp));
			}
			else {
				value = _value;
			}
		}
		scope.getIncludeSymbol().blockStart(0x01!=value, sp);
		
		scope.list(".IF " + " # " + (0 != value));
		
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}

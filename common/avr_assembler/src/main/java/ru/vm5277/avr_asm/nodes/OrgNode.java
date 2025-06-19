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
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.avr_asm.scope.CodeSegment;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;

public class OrgNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		SourcePosition sp = tb.getSP();
		CodeSegment cSeg = scope.getCSeg();
		Expression expr = Expression.parse(tb, scope, mc);
		Long value = Expression.getLong(expr, sp);
		if(null == value) {
			tb.skipLine();
			throw new ParseException("Cannot resolve constant '" + expr + "'", sp);
		}
		if(0>value || value>cSeg.getWSize()) throw new ParseException("Address 0x" + Long.toHexString(value) + " exceeds flash memory size", sp);
		
		scope.getCSeg().setPC(value.intValue());
		
		scope.list(".ORG " + value.intValue());
		
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}

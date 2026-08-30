/*
 * Copyright 2026 konstantin@5277.ru
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

import java.util.Random;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.CodeSegment;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.messages.MessageContainer;

public class RndDataNode extends Node {
	
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws CompileException {
		SourcePosition sp = tb.getSP();

		CodeSegment cSeg = scope.getCSeg();
		Expression expr = Expression.parse(tb, scope, mc, null);
		Long value = Expression.getLong(expr, sp);
		if(null==value) {
			tb.skipLine();
			throw new CompileException("Cannot resolve constant '" + expr + "'", sp);
		}
		if(0>value) {
			throw new CompileException("Value can't be negative", sp);
		}
		if(value>cSeg.getWSize()) {
			throw new CompileException("Value exceeds flash memory size", sp);
		}

		if(0!=(value.intValue()&0x01)) value = value+1;
		
		byte[] rndBytes = new byte[value.intValue()];
		new Random().nextBytes(rndBytes);
		
		if(scope.isListEnabled()) {
			scope.list(".RNDB " + StrUtils.toString(rndBytes));
		}
		
		int wSize = rndBytes.length/2;
		scope.getCSeg().getCurrentBlock().write(rndBytes, wSize);
		scope.getCSeg().movePC(wSize);
		
		Node.consumeToken(tb, TokenType.NEWLINE);
	}

	public void secondPass()  {
	}
}

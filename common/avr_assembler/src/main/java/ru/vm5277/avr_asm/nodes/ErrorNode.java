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
import ru.vm5277.common.SourcePosition;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class ErrorNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		SourcePosition sp = tb.getSP();
		String text = (String)Node.consumeToken(tb, TokenType.STRING).getValue();
		mc.add(new ErrorMessage(text, sp));

		scope.list(".ERROR " + text);

		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}

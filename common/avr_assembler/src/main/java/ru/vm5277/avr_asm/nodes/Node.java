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
import ru.vm5277.avr_asm.Delimiter;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.Operator;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.avr_asm.tokens.Token;

public class Node {
	protected			TokenBuffer				tb;
	protected			MessageContainer		mc;
	protected			Scope					scope;
	protected			SourcePosition			sp;
	private				ErrorMessage			error;
	
	protected Node() {
	}
	
	protected Node(TokenBuffer tb, Scope scope, MessageContainer mc) {
        this.tb = tb;
		this.sp = null == tb ? null : tb.current().getSP();
		this.scope = scope;
		this.mc = mc;
    }
	
	
	public static Token consumeToken(TokenBuffer tb, TokenType expectedType) throws ParseException {
		if (tb.current().getType() == expectedType) return tb.consume();
		else throw new ParseException("Expected " + expectedType + ", but got " + tb.current().getType(), tb.getSP());
    }
	public static Token consumeToken(TokenBuffer tb, Delimiter delimiter) throws ParseException {
		if (TokenType.DELIMITER == tb.current().getType()) {
            if(delimiter == tb.current().getValue()) return tb.consume();
			else throw new ParseException("Expected delimiter " + delimiter + ", but got " + tb.current().getValue(), tb.getSP());
        }
		else throw new ParseException("Expected " + TokenType.DELIMITER + ", but got " + tb.current().getType(), tb.getSP());
    }
	public static Token consumeToken(TokenBuffer tb, Operator operator) throws ParseException {
		if (TokenType.OPERATOR == tb.current().getType()) {
            if(operator == tb.current().getValue()) return tb.consume();
			else throw new ParseException("Expected operator " + operator + ", but got " + tb.current().getValue(), tb.getSP());
        }
		else throw new ParseException("Expected " + TokenType.OPERATOR + ", but got " + tb.current().getType(), tb.getSP());
    }

	public WarningMessage markWarning(String text) {
		WarningMessage message = new WarningMessage(text, sp);
		mc.add(message);
		return message;
	}
	public WarningMessage markWarning(String text, SourcePosition sp) {
		WarningMessage message = new WarningMessage(text, sp);
		mc.add(message);
		return message;
	}

	public ErrorMessage markError(ParseException e) {
		ErrorMessage message = new ErrorMessage(e.getMessage(), sp);
		if(null == error) error = message;
		mc.add(message);
		return message;
	}
	public ErrorMessage markError(String text) {
		ErrorMessage message = new ErrorMessage(text, sp);
		if(null == error) error = message;
		mc.add(message);
		return message;
	}

	public Scope getScope() {
		return scope;
	}

	public SourcePosition getSP() {
		return sp;
	}
}

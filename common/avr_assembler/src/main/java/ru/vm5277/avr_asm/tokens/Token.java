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
package ru.vm5277.avr_asm.tokens;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.SourceBuffer;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.avr_asm.Keyword;
import ru.vm5277.avr_asm.Lexer;
import ru.vm5277.common.DatatypeConverter;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class Token {
	private		final	static	DecimalFormat	df	= new DecimalFormat("0.####################", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	protected					TokenType		type;
	protected					Object			value;
	protected					SourceBuffer	sb;
	protected					SourcePosition	sp;
	private						ErrorMessage	error;

	public Token(SourceBuffer sb) {
		this.sb = sb;
		this.sp = sb.snapSP();
	}
	public Token(SourceBuffer sb, SourcePosition sp) {
		this.sb = sb;
		this.sp = sp;
	}
	
	public Token(SourceBuffer sb, TokenType type, Object value) {
		this.sb = sb;
		this.sp = null == sb ? null : sb.snapSP();
		this.type = type;
		this.value = value;
	}

	public Token(SourceBuffer sb, SourcePosition sp, TokenType type, Object value) {
		this.sb = sb;
		this.sp = sp;
		this.type = type;
		this.value = value;
	}

	public Object getValue() {
		if(TokenType.LITERAL == type) {
			Keyword kw = (Keyword)value;
			if(Keyword.TRUE == kw) return true;
			if(Keyword.FALSE == kw) return false;
			if(Keyword.NULL == kw) return null;
		}
		return value;
	}
	
	public String getStringValue() {
		return toStringValue(value);
	}
	
	public static String toStringValue(Object value) {
		if(value instanceof Double) return Token.df.format((Double)value);
		if(value instanceof Number) return ((Number)value).toString();
		if(value instanceof Boolean) return ((Boolean)value).toString();
		if(value instanceof byte[]) return "0x" + DatatypeConverter.printHexBinary((byte[])value);
		return (String)value;
	}

	public TokenType getType() {
		return type;
	}
	
	public SourcePosition getSP() {
		return sp;
	}
	
	public ErrorMessage getError() {
		return error;
	}


	public void setErrorAndSkipToken(String text, MessageContainer mc) {
		setError(text, mc);
		skipToken(sb, mc);
	}
	public void setError(String text, MessageContainer mc) {
		error = new ErrorMessage(text, sb.snapSP());
		mc.add(error);
	}
	public static void skipToken(SourceBuffer sb, MessageContainer mc) {
		while(sb.hasNext() && ';'!=sb.getChar()) {
			if(Lexer.skipWhiteSpaces(sb)) continue;
			if(Lexer.skipComment(sb, mc)) continue;
			sb.next();
		}
	}
	
	@Override
	public String toString() {
		return type + "(" + value + ")" + sb;
	}
}

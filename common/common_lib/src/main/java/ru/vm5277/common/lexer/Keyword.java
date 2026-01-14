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

package ru.vm5277.common.lexer;

import java.util.HashMap;
import java.util.Map;

public class Keyword {
	protected	static	final	Map<String, Keyword>	KEYWORDS	= new HashMap<>();	

	static {
		Keyword AS = J8BKeyword.AS;
		Keyword BYTE = ASMKeyword.BYTE;
	}
	
	// Литералы
	public		static	final	Keyword					TRUE		= new Keyword("true", TokenType.LITERAL);
	public		static	final	Keyword					FALSE		= new Keyword("false", TokenType.LITERAL);
	public		static	final	Keyword					NULL		= new Keyword("null", TokenType.LITERAL);
	
	private	String		name;
	private	TokenType	tokenType;
	private	LexerType	lexerType	= LexerType.ALL;
	
	public Keyword(String name, TokenType tokenType) {
		this.name = name;
		this.tokenType = tokenType;

		KEYWORDS.put(name, this);
	}
	
	public Keyword(String name, TokenType tokenType, LexerType lexerType) {
		this.name = name;
		this.tokenType = tokenType;
		this.lexerType = lexerType;

		KEYWORDS.put(name, this);
	}

	public String getName() {
		return name;
	}
	
	public TokenType getTokenType() {
		return tokenType;
	}
	
	public LexerType getLexerType() {
		return lexerType;
	}
	
	public static Keyword fromString(String name, LexerType lexerType) {
		Keyword result = KEYWORDS.get(name);
		if(null==result) return null;
		return (result.getLexerType()==lexerType || LexerType.ALL==result.getLexerType() ? result : null);
	}
	
	public static int getSize() {
		return KEYWORDS.size();
	}
	
	@Override
	public String toString() {
		return name;
	}
}

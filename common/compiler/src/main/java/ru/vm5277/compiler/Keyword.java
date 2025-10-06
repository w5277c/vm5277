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
package ru.vm5277.compiler;

import java.util.HashMap;
import java.util.Map;

public class Keyword {
	private	static	final	Map<String, Keyword>	KEYWORDS	= new HashMap<>();	
	// Литералы
	public	static	final	Keyword					TRUE		= new Keyword("true", TokenType.LITERAL);
	public	static	final	Keyword					FALSE		= new Keyword("false", TokenType.LITERAL);
	public	static	final	Keyword					NULL		= new Keyword("null", TokenType.LITERAL);
	// Типы примитив
	public	static	final	Keyword					VOID		= new Keyword("void", TokenType.TYPE);
	public	static	final	Keyword					BOOL		= new Keyword("bool", TokenType.TYPE);
	public	static	final	Keyword					BYTE		= new Keyword("byte", TokenType.TYPE);
	public	static	final	Keyword					SHORT		= new Keyword("short", TokenType.TYPE);
	public	static	final	Keyword					INT			= new Keyword("int", TokenType.TYPE);
	public	static	final	Keyword					FIXED		= new Keyword("fixed", TokenType.TYPE);
	public	static	final	Keyword					CSTR		= new Keyword("cstr", TokenType.TYPE);
	// Команды
	public	static	final	Keyword					IF			= new Keyword("if", TokenType.COMMAND);
	public	static	final	Keyword					DO			= new Keyword("do", TokenType.COMMAND);
	public	static	final	Keyword					WHILE		= new Keyword("while", TokenType.COMMAND);
	public	static	final	Keyword					FOR			= new Keyword("for", TokenType.COMMAND);
	public	static	final	Keyword					RETURN		= new Keyword("return", TokenType.COMMAND);
	public	static	final	Keyword					CONTINUE	= new Keyword("continue", TokenType.COMMAND);
	public	static	final	Keyword					BREAK		= new Keyword("break", TokenType.COMMAND);
	public	static	final	Keyword					SWITCH		= new Keyword("switch", TokenType.COMMAND);
	// Модификаторы
	public	static	final	Keyword					STATIC		= new Keyword("static", TokenType.MODIFIER);
	public	static	final	Keyword					FINAL		= new Keyword("final", TokenType.MODIFIER);
	public	static	final	Keyword					PRIVATE		= new Keyword("private", TokenType.MODIFIER);
	public	static	final	Keyword					PUBLIC		= new Keyword("public", TokenType.MODIFIER);
	public	static	final	Keyword					NATIVE		= new Keyword("native", TokenType.MODIFIER);
	public	static	final	Keyword					ATOMIC		= new Keyword("atomic", TokenType.MODIFIER);
	// Ключевые слова ООП
	public	static	final	Keyword					CLASS		= new Keyword("class", TokenType.OOP);
	public	static	final	Keyword					INTERFACE	= new Keyword("interface", TokenType.OOP);
	public	static	final	Keyword					IMPLEMENTS	= new Keyword("implements", TokenType.OOP);
	public	static	final	Keyword					THIS		= new Keyword("this", TokenType.OOP);
	//Исключения
	public	static	final	Keyword					TRY			= new Keyword("try", TokenType.COMMAND);
	public	static	final	Keyword					CATCH		= new Keyword("catch", TokenType.KEYWORD);
	public	static	final	Keyword					THROW		= new Keyword("throw", TokenType.COMMAND);
	public	static	final	Keyword					THROWS		= new Keyword("throws", TokenType.OOP);
	//Остальное
	public	static	final	Keyword					IMPORT		= new Keyword("import", TokenType.KEYWORD);
	public	static	final	Keyword					ELSE		= new Keyword("else", TokenType.KEYWORD);
	public	static	final	Keyword					AS			= new Keyword("as", TokenType.KEYWORD);
	public	static	final	Keyword					CASE		= new Keyword("case", TokenType.KEYWORD);
	public	static	final	Keyword					DEFAULT		= new Keyword("default", TokenType.KEYWORD);
	public	static	final	Keyword					NEW			= new Keyword("new", TokenType.KEYWORD);

	private	String		name;
	private	TokenType	tokenType;
	
	protected Keyword(String name, TokenType tokenType) {
		this.name = name;
		this.tokenType = tokenType;
		
		KEYWORDS.put(name, this);
	}
	
	public String getName() {
		return name;
	}
	
	public TokenType getTokenType() {
		return tokenType;
	}
	
	public static Keyword fromString(String name) {
		return KEYWORDS.get(name);
	}
	
	public static int getSize() {
		return KEYWORDS.size();
	}
	
	@Override
	public String toString() {
		return name;
	}
}

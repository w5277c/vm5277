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

public class J8BKeyword extends Keyword {
	// Типы примитив
	public	static	final	Keyword					VOID		= new Keyword("void", TokenType.TYPE, LexerType.J8B);
	public	static	final	Keyword					BOOL		= new Keyword("bool", TokenType.TYPE, LexerType.J8B);
	public	static	final	Keyword					BYTE		= new Keyword("byte", TokenType.TYPE, LexerType.J8B);
	public	static	final	Keyword					CHAR		= new Keyword("char", TokenType.TYPE, LexerType.J8B);
	public	static	final	Keyword					SHORT		= new Keyword("short", TokenType.TYPE, LexerType.J8B);
	public	static	final	Keyword					INT			= new Keyword("int", TokenType.TYPE, LexerType.J8B);
	public	static	final	Keyword					FIXED		= new Keyword("fixed", TokenType.TYPE, LexerType.J8B);
	public	static	final	Keyword					CSTR		= new Keyword("cstr", TokenType.TYPE, LexerType.J8B);
	// Команды
	public	static	final	Keyword					IF			= new Keyword("if", TokenType.COMMAND, LexerType.J8B);
	public	static	final	Keyword					DO			= new Keyword("do", TokenType.COMMAND, LexerType.J8B);
	public	static	final	Keyword					WHILE		= new Keyword("while", TokenType.COMMAND, LexerType.J8B);
	public	static	final	Keyword					FOR			= new Keyword("for", TokenType.COMMAND, LexerType.J8B);
	public	static	final	Keyword					RETURN		= new Keyword("return", TokenType.COMMAND, LexerType.J8B);
	public	static	final	Keyword					CONTINUE	= new Keyword("continue", TokenType.COMMAND, LexerType.J8B);
	public	static	final	Keyword					BREAK		= new Keyword("break", TokenType.COMMAND, LexerType.J8B);
	public	static	final	Keyword					SWITCH		= new Keyword("switch", TokenType.COMMAND, LexerType.J8B);
	// Модификаторы
	public	static	final	Keyword					STATIC		= new Keyword("static", TokenType.MODIFIER, LexerType.J8B);
	public	static	final	Keyword					FINAL		= new Keyword("final", TokenType.MODIFIER, LexerType.J8B);
	public	static	final	Keyword					PRIVATE		= new Keyword("private", TokenType.MODIFIER, LexerType.J8B);
	public	static	final	Keyword					PUBLIC		= new Keyword("public", TokenType.MODIFIER, LexerType.J8B);
	public	static	final	Keyword					NATIVE		= new Keyword("native", TokenType.MODIFIER, LexerType.J8B);
	public	static	final	Keyword					ATOMIC		= new Keyword("atomic", TokenType.MODIFIER, LexerType.J8B);
	// Ключевые слова ООП
	public	static	final	Keyword					CLASS		= new Keyword("class", TokenType.OOP, LexerType.J8B);
	public	static	final	Keyword					INTERFACE	= new Keyword("interface", TokenType.OOP, LexerType.J8B);
	public	static	final	Keyword					EXCEPTION	= new Keyword("exception", TokenType.OOP, LexerType.J8B);
	public	static	final	Keyword					IMPLEMENTS	= new Keyword("implements", TokenType.OOP, LexerType.J8B);
	public	static	final	Keyword					EXTENDS		= new Keyword("extends", TokenType.OOP, LexerType.J8B);
	public	static	final	Keyword					THIS		= new Keyword("this", TokenType.OOP, LexerType.J8B);
	public	static	final	Keyword					ENUM		= new Keyword("enum", TokenType.OOP, LexerType.J8B);
	//Исключения
	public	static	final	Keyword					TRY			= new Keyword("try", TokenType.COMMAND, LexerType.J8B);
	public	static	final	Keyword					CATCH		= new Keyword("catch", TokenType.KEYWORD, LexerType.J8B);
	public	static	final	Keyword					THROWS		= new Keyword("throws", TokenType.OOP, LexerType.J8B);
	//Остальное
	public	static	final	Keyword					IMPORT		= new Keyword("import", TokenType.KEYWORD, LexerType.J8B);
	public	static	final	Keyword					ELSE		= new Keyword("else", TokenType.KEYWORD, LexerType.J8B);
	public	static	final	Keyword					AS			= new Keyword("as", TokenType.KEYWORD, LexerType.J8B);
	public	static	final	Keyword					CASE		= new Keyword("case", TokenType.KEYWORD, LexerType.J8B);
	public	static	final	Keyword					DEFAULT		= new Keyword("default", TokenType.KEYWORD, LexerType.J8B);
	public	static	final	Keyword					NEW			= new Keyword("new", TokenType.KEYWORD, LexerType.J8B);

	public J8BKeyword(String name, TokenType tokenType) {
		super(name, tokenType, LexerType.J8B);
	}

	public static Keyword fromString(String name) {
		return Keyword.fromString(name, LexerType.J8B);
	}
}

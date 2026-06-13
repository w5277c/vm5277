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

public class ASMKeyword extends Keyword {
	private	static	final	Map<String, Keyword>	KEYWORDS	= new HashMap<>();	

	// Константы
	public	static	final	Keyword					EQU		= new Keyword(".equ", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					SET		= new Keyword(".set", TokenType.DIRECTIVE, LexerType.ASM);
	// Установка адреса размещения кода
	public	static	final	Keyword					ORG		= new Keyword(".org", TokenType.DIRECTIVE, LexerType.ASM);
	// Подключение внешнего файла
	public	static	final	Keyword					INCLUDE	= new Keyword(".include", TokenType.DIRECTIVE, LexerType.ASM);
	// Указание целевого МК
	public	static	final	Keyword					DEVICE	= new Keyword(".device", TokenType.DIRECTIVE, LexerType.ASM);
	// Условия
	public	static	final	Keyword					IFDEF	= new Keyword(".ifdef", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					IFNDEF	= new Keyword(".ifndef", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					ENDIF	= new Keyword(".endif", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					IF		= new Keyword(".if", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					ELSE	= new Keyword(".else", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					ELSEIF	= new Keyword(".elseif", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					ELIF	= new Keyword(".elif", TokenType.DIRECTIVE, LexerType.ASM);
	// Диагностические сообщения
	public	static	final	Keyword					MESSAGE	= new Keyword(".message", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					WARNING	= new Keyword(".warning", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					ERROR	= new Keyword(".error", TokenType.DIRECTIVE, LexerType.ASM);
	// Операции с определениями
	public	static	final	Keyword					DEF		= new Keyword(".def", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					UNDEF	= new Keyword(".undef", TokenType.DIRECTIVE, LexerType.ASM);
	// Макросы
	public	static	final	Keyword					MACRO	= new Keyword(".macro", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					ENDM	= new Keyword(".endm", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					ENDMACRO= new Keyword(".endmacro", TokenType.DIRECTIVE, LexerType.ASM);
	// Данные
	public	static	final	Keyword					DB		= new Keyword(".db", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					DW		= new Keyword(".dw", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					DD		= new Keyword(".dd", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					DQ		= new Keyword(".dq", TokenType.DIRECTIVE, LexerType.ASM);
	// Блок случайных данных
	public	static	final	Keyword					RNDB	= new Keyword(".rndb", TokenType.DIRECTIVE, LexerType.ASM);
	// Листинг
	public	static	final	Keyword					EXIT	= new Keyword(".exit", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					LIST	= new Keyword(".list", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					NOLIST	= new Keyword(".nolist", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					LISTMAC	= new Keyword(".listmac", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					NOLISTMAC= new Keyword(".nolistmac", TokenType.DIRECTIVE, LexerType.ASM);
	// Перекрытие адресов
	public	static	final	Keyword					OVERLAP	= new Keyword(".overlap", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					NOOVERLAP= new Keyword(".noverlap", TokenType.DIRECTIVE, LexerType.ASM);
	// Резервирование блока во FLASH
	public	static	final	Keyword					BYTE	= new Keyword(".byte", TokenType.DIRECTIVE, LexerType.ASM);
	// Типы сегментов
	public	static	final	Keyword					CSEG	= new Keyword(".cseg", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					DSEG	= new Keyword(".dseg", TokenType.DIRECTIVE, LexerType.ASM);
	public	static	final	Keyword					ESEG	= new Keyword(".eseg", TokenType.DIRECTIVE, LexerType.ASM);
	// Функции
	public	static	final	Keyword					LOW		= new Keyword("low", TokenType.COMMAND, LexerType.ASM);
	public	static	final	Keyword					HIGH	= new Keyword("high", TokenType.COMMAND, LexerType.ASM);
	public	static	final	Keyword					BYTE1	= new Keyword("byte1", TokenType.COMMAND, LexerType.ASM);
	public	static	final	Keyword					BYTE2	= new Keyword("byte2", TokenType.COMMAND, LexerType.ASM);
	public	static	final	Keyword					BYTE3	= new Keyword("byte3", TokenType.COMMAND, LexerType.ASM);
	public	static	final	Keyword					BYTE4	= new Keyword("byte4", TokenType.COMMAND, LexerType.ASM);
	public	static	final	Keyword					LWRD	= new Keyword("lwrd", TokenType.COMMAND, LexerType.ASM);
	public	static	final	Keyword					HWRD	= new Keyword("hwrd", TokenType.COMMAND, LexerType.ASM);
	public	static	final	Keyword					PAGE	= new Keyword("page", TokenType.COMMAND, LexerType.ASM);
	public	static	final	Keyword					EXP2	= new Keyword("exp2", TokenType.COMMAND, LexerType.ASM);
	public	static	final	Keyword					LOG		= new Keyword("log", TokenType.COMMAND, LexerType.ASM);

	public ASMKeyword(String name, TokenType tokenType) {
		super(name, tokenType, LexerType.ASM);
	}

	public static Keyword fromString(String name) {
		return Keyword.fromString(name, LexerType.ASM);
	}
}

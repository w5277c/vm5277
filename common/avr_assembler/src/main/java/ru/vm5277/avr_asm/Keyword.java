/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
29.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm;

import java.util.HashMap;
import java.util.Map;

public class Keyword {
	private	static	final	Map<String, Keyword>	KEYWORDS	= new HashMap<>();	

	// Литералы
	public	static	final	Keyword					TRUE	= new Keyword("true", TokenType.LITERAL);
	public	static	final	Keyword					FALSE	= new Keyword("false", TokenType.LITERAL);
	public	static	final	Keyword					NULL	= new Keyword("null", TokenType.LITERAL);
	// Константы
	public	static	final	Keyword					EQU		= new Keyword(".equ", TokenType.DIRECTIVE);
	public	static	final	Keyword					SET		= new Keyword(".set", TokenType.DIRECTIVE);
	// Установка адреса размещения кода
	public	static	final	Keyword					ORG		= new Keyword(".org", TokenType.DIRECTIVE);
	// Подключение внешнего файла
	public	static	final	Keyword					INCLUDE	= new Keyword(".include", TokenType.DIRECTIVE);
	// Указание целевого МК
	public	static	final	Keyword					DEVICE	= new Keyword(".device", TokenType.DIRECTIVE);
	// Условия
	public	static	final	Keyword					IFDEF	= new Keyword(".ifdef", TokenType.DIRECTIVE);
	public	static	final	Keyword					IFNDEF	= new Keyword(".ifndef", TokenType.DIRECTIVE);
	public	static	final	Keyword					ENDIF	= new Keyword(".endif", TokenType.DIRECTIVE);
	public	static	final	Keyword					IF		= new Keyword(".if", TokenType.DIRECTIVE);
	public	static	final	Keyword					ELSE	= new Keyword(".else", TokenType.DIRECTIVE);
	public	static	final	Keyword					ELSEIF	= new Keyword(".elseif", TokenType.DIRECTIVE);
	public	static	final	Keyword					ELIF	= new Keyword(".elif", TokenType.DIRECTIVE);
	// Диагностические сообщения
	public	static	final	Keyword					MESSAGE	= new Keyword(".message", TokenType.DIRECTIVE);
	public	static	final	Keyword					WARNING	= new Keyword(".warning", TokenType.DIRECTIVE);
	public	static	final	Keyword					ERROR	= new Keyword(".error", TokenType.DIRECTIVE);
	// Операции с определениями
	public	static	final	Keyword					DEF		= new Keyword(".def", TokenType.DIRECTIVE);
	public	static	final	Keyword					UNDEF	= new Keyword(".undef", TokenType.DIRECTIVE);
	// Макросы
	public	static	final	Keyword					MACRO	= new Keyword(".macro", TokenType.DIRECTIVE);
	public	static	final	Keyword					ENDM	= new Keyword(".endm", TokenType.DIRECTIVE);
	public	static	final	Keyword					ENDMACRO= new Keyword(".endmacro", TokenType.DIRECTIVE);
	// Данные
	public	static	final	Keyword					DB		= new Keyword(".db", TokenType.DIRECTIVE);
	public	static	final	Keyword					DW		= new Keyword(".dw", TokenType.DIRECTIVE);
	public	static	final	Keyword					DD		= new Keyword(".dd", TokenType.DIRECTIVE);
	public	static	final	Keyword					DQ		= new Keyword(".dq", TokenType.DIRECTIVE);
	// Листинг
	public	static	final	Keyword					EXIT	= new Keyword(".exit", TokenType.DIRECTIVE);
	public	static	final	Keyword					LIST	= new Keyword(".list", TokenType.DIRECTIVE);
	public	static	final	Keyword					NOLIST	= new Keyword(".nolist", TokenType.DIRECTIVE);
	public	static	final	Keyword					LISTMAC	= new Keyword(".listmac", TokenType.DIRECTIVE);
	// Перекрытие адресов
	public	static	final	Keyword					OVERLAP	= new Keyword(".overlap", TokenType.DIRECTIVE);
	public	static	final	Keyword					NOOVERLAP= new Keyword(".noverlap", TokenType.DIRECTIVE);
	// Резервирование блока во FLASH
	public	static	final	Keyword					BYTE	= new Keyword(".byte", TokenType.DIRECTIVE);
	// Типы сегментов
	public	static	final	Keyword					CSEG	= new Keyword(".cseg", TokenType.DIRECTIVE);
	public	static	final	Keyword					DSEG	= new Keyword(".dseg", TokenType.DIRECTIVE);
	public	static	final	Keyword					ESEG	= new Keyword(".eseg", TokenType.DIRECTIVE);
	// Функции
	public	static	final	Keyword					LOW		= new Keyword("low", TokenType.COMMAND);
	public	static	final	Keyword					HIGH	= new Keyword("high", TokenType.COMMAND);
	public	static	final	Keyword					BYTE1	= new Keyword("byte1", TokenType.COMMAND);
	public	static	final	Keyword					BYTE2	= new Keyword("byte2", TokenType.COMMAND);
	public	static	final	Keyword					BYTE3	= new Keyword("byte3", TokenType.COMMAND);
	public	static	final	Keyword					BYTE4	= new Keyword("byte4", TokenType.COMMAND);
	public	static	final	Keyword					LWRD	= new Keyword("lwrd", TokenType.COMMAND);
	public	static	final	Keyword					HWRD	= new Keyword("hwrd", TokenType.COMMAND);
	public	static	final	Keyword					PAGE	= new Keyword("page", TokenType.COMMAND);
	public	static	final	Keyword					EXP2	= new Keyword("exp2", TokenType.COMMAND);
	public	static	final	Keyword					LOG		= new Keyword("log", TokenType.COMMAND);
	
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

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

public enum TokenType {
	// Ключевые слова, keywords(разбитые по категориям)
	LITERAL,	//Булевые заняения и null
	TYPE,		//Типы данных
	COMMAND,	//Команды
	MODIFIER,	//Модификаторы
	OOP,		//Ключевые слова ООП
	KEYWORD,	//Остальные ключевые слова
	IDENTIFIER,
	LABEL,
	NUMBER,
	NOTE,		//Тип представление музыки (ноты)
	OPERATOR,
	DELIMITER,
	STRING,
	CHARACTER,

	INVALID,
	COMMENT,
	WHITESPACE,
	NEWLINE,
	EOF,
	
	//ASM only
	DIRECTIVE,	//Директива ассемблера
	MNEMONIC,	//Мнемоника ассемблера
	INDEX_REG,	//Индексные регистры (xl,xh,yl,yh,zl,zg,z,x,y для AVR)
	MACRO,		//Имя макроса, не используется в парсере, но используется в LSP сервере (вероятно нужно убрать после реализации парсинга в LSP)
	MACRO_PARAM, //Параметр в макросе
	
	REGISTER;	//Регистры МК (r0-r31 для AVR)
}

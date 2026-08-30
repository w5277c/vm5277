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

package ru.vm5277.common.lexer.tokens;

import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.lexer.TokenType;

public class TAnnotation extends Token {
	public	static	final	String	OVERRIDE				= "Override"; // Реализация метода на основе контракта из интерфейса
	public	static	final	String	EXPAND_HEAP				= "ExpandHeap"; // Расширить HEAP на указанный размер (для RTOS драйвера)
	public	static	final	String	REGISTER_SOFT_TIMER		= "RegisterSoftTimer"; // Статическая регистрация программного таймера с записью его идентификатора в поле/переменную
	public	static	final	String	MELODY					= "Melody";	// Сформировать байтовое представление мелодии и поместить в поле/переменную-массив
	public	static	final	String	FLASH_DATA				= "FlashData"; // Сформировать блок данных в FLASH и записать адрес в поле
	public	static	final	String	ENTRY_POINT				= "EntryPoint"; // Не исключаемые из кодогенерации методы (только для интерфейсов)
	public	static	final	String	NATIVE					= "Native"; // Генерируем дефайны в ассембеле с адресами static полей RtosDriver наследников
	//TODO реализовать генерацию HEX файла для EEPROM и его прошивку, выполнить доработку для всех компонентов.
	public	static	final	String	EEPROM_DATA				= "EepromData"; // Сформировать блок данных для EEPROM и записать адрес в поле 

	public	static	final	String	RTOS_FEATURE			= "RtosFeature"; // Включить фичу
	public	static	final	String	RTOS_PARAM_EQ			= "RtosParamEq"; // Компилировать блок, если значение параметра соответствует
	public	static	final	String	RTOS_PARAM_NOT_EQ		= "RtosParamNotEq"; // Компилировать блок, если значение параметра не соответствует
	
	public TAnnotation(SourceBuffer sb, SourcePosition sp) {
		super(sb, sp);
		
		this.type = TokenType.ANNOTATION;
		StringBuilder stringBuilder = new StringBuilder();
        while (sb.available() && (Character.isLetterOrDigit(sb.peek()) || '_'==sb.peek())) {
            stringBuilder.append(sb.peek());
			sb.next();
        }
		raw = stringBuilder.toString();
		value = (String)raw;
		length = sb.getPos()-sp.getPos();
	}
}

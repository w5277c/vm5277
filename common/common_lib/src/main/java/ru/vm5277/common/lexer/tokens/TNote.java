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

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.SourceBuffer;

// Нотная запись для полифонии, пример m/8e5-e5-e5-c5-e5-/4g5-g4, т.е. e5 = Ми, c5 = До, g5 = Соль (без диезов-заглавная буква)
public class TNote extends Token {
	StringBuilder	strValue	= new StringBuilder();
	
	// Структура байта:
	// [7] = 0: нота | 1: длительность/пауза
	//
	// Для нот (бит 7=0):
	//   [6:4] - октава (0-7, соответствует MIDI октавам 1-8)
	//   [3:0] - нота (0-11: где регистр буквы определяет диез)
	//
	// Для длительности (бит 7=1):
	//   [6]   - 1=пауза, 0=обычная длительность
	//   [5:0] - значение длительности (0:/1, 1:/4, 2:/8, 3:/16)

	public TNote(SourceBuffer sb) throws CompileException {
		super(sb);
		
		List<Byte> byteBuffer = new ArrayList<>();
		
		type = TokenType.NOTE;

		// Дефолтная длительность (4 = четверть)
		byte currentDuration = 0x04; 

		while(sb.available() && ' '!=sb.peek()) {
			char ch = sb.peek();
			strValue.append(ch);
			
			// Пропуск разделителей
			if('-'==ch) {
				sb.next();
				continue;
			}

			// Обработка длительности (например, "/8")
			if('/'==ch) {
				sb.next();
				ch = sb.peek();
				if(!sb.available() || !Character.isDigit(ch)) {
					skipToken(sb);
					throw new CompileException("Invalid duration");
				}
				strValue.append(ch);
				currentDuration = (byte) (Character.getNumericValue(ch) & 0x3F);
				sb.next();
				byteBuffer.add((byte) (0x80 | currentDuration)); // Бит 7=1 (длительность)
				continue;
			}
			
			// Обработка ноты (регистр определяет альтерацию)
			if(Character.isLetter(ch)) {
				boolean isSharp = Character.isUpperCase(ch);
				char noteLower = Character.toLowerCase(ch);
				int noteValue = "cdefgab".indexOf(noteLower);

				if(-1==noteValue) {
					skipToken(sb);
					throw new CompileException("Invalid note: " + ch);
				}
				if(isSharp) {
					if (noteLower == 'e') noteLower = 'f';
					if (noteLower == 'b') noteLower = 'c';
					isSharp = false;
				}
				// Код ноты: 0-11
				int packedNote = noteValue * 2 + (isSharp ? 1 : 0);
				if (packedNote > 11) packedNote = 11; // Для 'b' (си) нет диеза

				// Октава (по умолчанию 4)
				int octave = 4;
				ch = sb.peek(1);
				if(sb.available(2) && Character.isDigit(ch)) {
					octave = Character.getNumericValue(ch) - 1;
					strValue.append(ch);
					sb.next();
				}

				byteBuffer.add((byte) ((octave << 4) | packedNote));
				sb.next();
				continue;
			}
			sb.next();
			throw new CompileException("Unexpected symbol: " + ch);
		}

		byte[] result = new byte[byteBuffer.size()];
		
		for(int i=0; i< result.length; i++) {
			result[i] = byteBuffer.get(i);
		}
		value = result;
		
		length = sb.getPos()-sp.getPos();
	}
	
	@Override
	public String toString() {
		return type + "(" + strValue + ")" + sb;
	}
}
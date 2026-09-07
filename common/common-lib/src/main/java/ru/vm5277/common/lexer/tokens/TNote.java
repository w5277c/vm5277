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

	// Нотная запись для полифонии, пример /8e5-e5-e5-c5-e5-/4g5-g4
	// Формат ноты: [нота][#?][октава]
	// Нота: c, d, e, f, g, a, b (всегда в нижнем регистре)
	// # - диез (опционально)
	// Октава: 0-7 (по умолчанию 4)
	// Примеры: c4, c#4, e5, g5

	public class TNote extends Token {
		StringBuilder strValue = new StringBuilder();

		// Структура байта:
		// [7] = 0: нота | 1: длительность/пауза
		//
		// Для нот (бит 7=0):
		//	[6:4] - октава (0-7, соответствует MIDI октавам 0-7)
		//	[3:0] - нота (0-11, где 0=C, 1=C#, 2=D, ... 11=B)
		//
		// Для длительности (бит 7=1):
		//	[6]   - 1=пауза, 0=обычная длительность
		//	[5:0] - значение длительности (0:/1, 1:/4, 2:/8, 3:/16)

	private	static	final	byte[]	NOTE_MAP	= {0, 2, 4, 5, 7, 9, 11};
		
	public TNote(SourceBuffer sb) throws CompileException {
		super(sb);
		
		List<Byte> byteBuffer = new ArrayList<>();
		
		type = TokenType.NOTE;

		// Дефолтная длительность (4 = четверть)
		byte currentDuration = 0x04; 

		while(sb.available() && ' '!=sb.peek()) {
			char ch = sb.peek();
			strValue.append(ch);
			sb.next();
			
			// Пропуск разделителей
			if('-'==ch) {
				continue;
			}

			if('p'==ch || 'r'==ch) {
				byteBuffer.add((byte) (0x80|0x40|(currentDuration/2)));
				strValue.append('p');
				continue;
			}

			// Обработка длительности (например, "/8")
			if('/'==ch) {
				ch = sb.peek();
				sb.next();
				if(!sb.available() || !Character.isDigit(ch)) {
					skipToken(sb);
					throw new CompileException("Invalid duration");
				}
				Character ch2 = null;
				if(sb.available()) {
					ch2 = sb.peek();
					if(Character.isDigit(ch2)) {
						currentDuration = (byte)(Character.getNumericValue(ch2) + Character.getNumericValue(ch)*10);
						strValue.append(ch).append(ch2);
						sb.next();
					}
					else {
						currentDuration = (byte)Character.getNumericValue(ch);
						strValue.append(ch);
					}
				}
				else {
					currentDuration = (byte)Character.getNumericValue(ch);
					strValue.append(ch);
				}
				if(currentDuration>=64) currentDuration = 63;
				if(0==currentDuration) currentDuration = 1;
				byteBuffer.add((byte)(0x80|(currentDuration/2))); // Бит 7=1 (длительность)
				continue;
			}
			
			// Обработка ноты (регистр определяет альтерацию)
			if(Character.isLetter(ch)) {
				char noteLower = Character.toLowerCase(ch);

				boolean isSharp = false;
				if(sb.available() && '#'==sb.peek()) {
					isSharp = true;
					strValue.append('#');
					sb.next();
				}
				
				boolean octaveOvf = false;
				if(isSharp) {
					if('e'==noteLower) {
						noteLower = 'f';
						isSharp = false;
					}
					if('b'==noteLower) {
						noteLower = 'c';
						isSharp = false;
						octaveOvf = true;
					}
				}
				
				int noteIndex = "cdefgab".indexOf(noteLower);
				if(-1==noteIndex) {
					skipToken(sb);
					throw new CompileException("Invalid note: " + ch);
				}

				// Код ноты: 0-11
				int packedNote = NOTE_MAP[noteIndex] + (isSharp ? 1 : 0);

				// Октава (по умолчанию 4)
				int octave = 4;
				ch = sb.peek();
				if(sb.available(1) && Character.isDigit(ch)) {
					octave = Character.getNumericValue(ch); 
					strValue.append(ch);
					sb.next();
				}
				if(octaveOvf) octave++;
				if(octave>7) {
					throw new CompileException("Octave out of range (0-7): " + octave);
				}
				byteBuffer.add((byte) ((octave << 4) | packedNote));
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

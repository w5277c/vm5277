/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
29.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.SourceBuffer;
import ru.vm5277.j8b.compiler.messages.MessageContainer;

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

	public TNote(SourceBuffer sb, MessageContainer mc) {
		super(sb);
		
		List<Byte> byteBuffer = new ArrayList<>();
		
		type = TokenType.NOTE;

		// Дефолтная длительность (4 = четверть)
		byte currentDuration = 0x04; 

		while (sb.hasNext() && ' '!=sb.getChar()) {
			char ch = sb.getChar();
			strValue.append(ch);
			
			// Пропуск разделителей
			if ('-'==ch) {
				sb.next();
				continue;
			}

			// Обработка длительности (например, "/8")
			if ('/'==ch) {
				sb.next();
				ch = sb.getChar();
				if (!sb.hasNext() || !Character.isDigit(ch)) {
					setErrorAndSkipToken("Invalid duration", mc);
					return;
				}
				strValue.append(ch);
				currentDuration = (byte) (Character.getNumericValue(ch) & 0x3F);
				sb.next();
				byteBuffer.add((byte) (0x80 | currentDuration)); // Бит 7=1 (длительность)
				continue;
			}
			
			// Обработка ноты (регистр определяет альтерацию)
			if (Character.isLetter(ch)) {
				boolean isSharp = Character.isUpperCase(ch);
				char noteLower = Character.toLowerCase(ch);
				int noteValue = "cdefgab".indexOf(noteLower);

				if (noteValue == -1) {
					setErrorAndSkipToken("Invalid note: " + ch, mc);
					return;
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
				ch = sb.getChar(1);
				if (sb.hasNext(1) && Character.isDigit(ch)) {
					octave = Character.getNumericValue(ch) - 1;
					strValue.append(ch);
					sb.next();
				}

				byteBuffer.add((byte) ((octave << 4) | packedNote));
				sb.next();
				continue;
			}
			setErrorAndSkipToken("Unexpected symbol: " + ch, mc);
			return;
		}

		byte[] result = new byte[byteBuffer.size()];
		
		for(int i=0; i< result.length; i++) {
			result[i] = byteBuffer.get(i);
		}
		value = result;
	}
	
	@Override
	public String toString() {
		return type + "(" + strValue + ")" + sb;
	}
}
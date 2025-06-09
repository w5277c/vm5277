/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.tokens;

import ru.vm5277.common.SourceBuffer;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.j8b_compiler.TokenType;
import ru.vm5277.j8b_compiler.Delimiter;

public class TDelimiter extends Token {
	public TDelimiter(SourceBuffer sb, SourcePosition sp, Delimiter value) {
		super(sb, sp);
		this.type = TokenType.DELIMITER;
		this.value = value;
	}

	public static TDelimiter parse(SourceBuffer sb) {
		SourcePosition sp = sb.snapSP();
		Delimiter delimiter = Delimiter.matchLongestDelimiter(sb);
		if (null!=delimiter) {
			return new TDelimiter(sb, sp, delimiter);
		}
	    return null;
	}
}

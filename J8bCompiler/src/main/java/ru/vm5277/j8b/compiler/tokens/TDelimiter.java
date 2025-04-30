/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.SourceBuffer;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.Delimiter;

public class TDelimiter extends Token {
	public TDelimiter(SourceBuffer sb, Delimiter value) {
		super(sb);
		this.type = TokenType.DELIMITER;
		this.value = value;
	}

	public static TDelimiter parse(SourceBuffer sb) {
		Delimiter delimiter = Delimiter.matchLongestDelimiter(sb);
		if (null!=delimiter) {
			return new TDelimiter(sb, delimiter);
		}
	    return null;
	}
}

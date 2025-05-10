/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.SourceBuffer;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.Operator;

public class TOpearator extends Token {
	private TOpearator(SourceBuffer sb, Operator op) {
		super(sb);
		this.type = TokenType.OPERATOR;
		this.value = op;
	}

	public static TOpearator parse(SourceBuffer sb) {
		
		// Проверяем трехсимвольные операторы (если есть)
        if (sb.hasNext(2)) {
            Operator op = Operator.fromSymbol(sb.getSource().substring(sb.getPos(), sb.getPos()+3));
            if (null != op) {
				TOpearator result = new TOpearator(sb, op);
				sb.next(3);
				return result;
			}
        }
        
        // Проверяем двухсимвольные операторы
        if (sb.hasNext(1)) {
            Operator op = Operator.fromSymbol(sb.getSource().substring(sb.getPos(), sb.getPos()+2));
            if (null != op) {
				TOpearator result = new TOpearator(sb, op);
				sb.next(2);
				return result;
			}
        }
        
		// Проверяем односимвольные операторы
        if (sb.hasNext()) {
            Operator op = Operator.fromSymbol(sb.getSource().substring(sb.getPos(), sb.getPos()+1));
            if (null != op) {
				TOpearator result = new TOpearator(sb, op);
				sb.next();
				return result;
			}
        }
		return null;
	}
}

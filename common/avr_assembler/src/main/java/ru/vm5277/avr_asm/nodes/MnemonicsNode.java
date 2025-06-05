/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
31.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.Delimiter;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;

public class MnemonicsNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		List<String> instructionIds = new ArrayList<>();
		while(true) {
			instructionIds.add((String)Node.consumeToken(tb, TokenType.MNEMONIC).getValue());
			if(tb.match(TokenType.NEWLINE)) break;
			Node.consumeToken(tb, Delimiter.COMMA);
		}
		scope.addMnemonics(instructionIds);
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}

/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
02.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.common.messages;

import ru.vm5277.common.SourcePosition;

public class ErrorMessage extends Message {
	public ErrorMessage(MessageOwner owner, String text, SourcePosition sp) {
		super("ERR", owner, text, sp);
	}

	public ErrorMessage(String text, SourcePosition sp) {
		super("ERR", text, sp);
	}
}

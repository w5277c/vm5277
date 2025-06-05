/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
02.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.common.messages;

import ru.vm5277.common.SourcePosition;

public class WarningMessage extends Message {
	public WarningMessage(MessageOwner owner, String text, SourcePosition sp) {
		super("WRN", owner, text, sp);
	}

	public WarningMessage(String text, SourcePosition sp) {
		super("WRN", text, sp);
	}
}

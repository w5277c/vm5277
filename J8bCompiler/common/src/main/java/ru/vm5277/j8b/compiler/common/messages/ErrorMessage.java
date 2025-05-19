/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
02.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.common.messages;

import ru.vm5277.j8b.compiler.common.SourcePosition;

public class ErrorMessage extends Message {
	public ErrorMessage(MessageOwner owner, String text, SourcePosition sp) {
		super(owner, text, sp);
	}

	public ErrorMessage(String text, SourcePosition sp) {
		super(text, sp);
	}
}

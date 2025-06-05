/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.common.exceptions;

import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.messages.ErrorMessage;

public class ParseException extends Exception {
	private	final	ErrorMessage	message;

	public ParseException(ErrorMessage message) {
		super(message.getText() + (null == message.getSP() ? "" : " at " + message.getSP()));
		this.message = message;
	}

	public ParseException(String text, SourcePosition sp) {
		super(text + (null == sp ? "" : " at " + sp));
		this.message = new ErrorMessage(text, sp);
	}

	public ErrorMessage getErrorMessage() {
		return message;
	}
}

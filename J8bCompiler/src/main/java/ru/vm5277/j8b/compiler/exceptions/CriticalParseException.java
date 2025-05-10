/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.exceptions;

import ru.vm5277.j8b.compiler.messages.ErrorMessage;

public class CriticalParseException extends Exception {
	private	final	ErrorMessage	message;

	public CriticalParseException(ParseException exception) {
		super(exception.getErrorMessage().getText() + " at " + exception.getErrorMessage().getSP());
		this.message = exception.getErrorMessage();
	}

	public ErrorMessage getErrorMessage() {
		return message;
	}
}

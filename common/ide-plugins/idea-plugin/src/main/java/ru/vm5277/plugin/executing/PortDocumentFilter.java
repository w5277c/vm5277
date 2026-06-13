package ru.vm5277.plugin.executing;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class PortDocumentFilter extends PlainDocument {
	@Override
	public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
		if(str==null) return;

		if(str.matches("[a-zA-Z0-9/]+")) {
			super.insertString(offset, str.toUpperCase(), attr);
		}
	}

	public static boolean isValid(String str) {
		if(str.isEmpty()) return true;
		return str.matches("([pP][a-zA-Z]([0-5][0-9]?|6[0-3]?|[7-9]?)|[rR][eE][sS][eE][tT]|[rR][xX]|[tT][xX])(/([pP][a-zA-Z]([0-5][0-9]?|6[0-3]?|[7-9]?)|[rR][eE][sS][eE][tT]|[rR][xX]|[tT][xX]))?");
	}
}
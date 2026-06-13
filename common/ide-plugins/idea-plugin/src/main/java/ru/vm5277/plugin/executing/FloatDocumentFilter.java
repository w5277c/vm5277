package ru.vm5277.plugin.executing;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class FloatDocumentFilter extends PlainDocument {
	@Override
	public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
		if(str==null) return;

		if(str.matches("[0-9/.]+")) {
			super.insertString(offset, str.toUpperCase(), attr);
		}
	}

	public static boolean isValid(String str) {
		return str.matches("(0\\.[0-9]*[1-9][0-9]*)|([1-9][0-9]*(\\.[0-9]*)?)");
	}
}
package ru.vm5277.plugin.executing;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class HexDocumentFilter extends PlainDocument {
	private final int maxLength;

	public HexDocumentFilter(int maxLength) {
		this.maxLength = maxLength;
	}

	@Override
	public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
		if(str == null) return;

		if((getLength() + str.length()<=maxLength) && str.matches("[0-9A-Fa-f]+")) {
			super.insertString(offset, str.toUpperCase(), attr);
		}
	}

	public static boolean isValid(String str) {
		return str.matches("[0-9A-Fa-f]+");
	}
}
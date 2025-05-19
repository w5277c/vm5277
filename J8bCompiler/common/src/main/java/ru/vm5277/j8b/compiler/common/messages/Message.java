/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
02.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.common.messages;

import ru.vm5277.j8b.compiler.common.SourcePosition;

public class Message {
	private			MessageOwner	owner;
	private	final	String			text;
	private	final	SourcePosition	sp;
	
	Message(String text, SourcePosition sp) {
		this.text = text;
		this.sp = sp;
	}

	Message(MessageOwner owner, String text, SourcePosition sp) {
		this.owner = owner;
		this.text = text;
		this.sp = sp;
	}
	
	public void setMessageOwnerIfNull(MessageOwner owner) {
		if(null == this.owner) {
			this.owner = owner;
		}
	}
	
	public String getText() {
		return text;
	}
	
	public SourcePosition getSP() {
		return sp;
	}
	
	public String toStrig() {
		return getClass().getSimpleName() + " " + owner + ":" + text + (null == sp ? "" : " at " + sp);
	}
}

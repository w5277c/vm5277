/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
02.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.common.messages;

import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageOwner;
import ru.vm5277.common.messages.Message;
import ru.vm5277.common.exceptions.MessageContainerIsFullException;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.exceptions.CompilationAbortedException;

public class MessageContainer {
	
	private	final	List<Message>	messages		= new ArrayList<>();
	private			int				maxErrQnt		= 8;
	private			int				errCntr			= 0;
	private			boolean			showImmeditly	= false;
	private			boolean			stopImmeditly	= false;
	private			MessageOwner	currentOwner	= MessageOwner.OTHER;
	
	public MessageContainer() {
	}

	public MessageContainer(int maxErrQnt) {
		this.maxErrQnt = maxErrQnt;
	}

	public MessageContainer(int maxErrQnt, boolean showImmeditly, boolean stopImmeditly) {
		this.maxErrQnt = maxErrQnt;
		this.showImmeditly = showImmeditly;
		this.stopImmeditly = stopImmeditly;
	}

	public void setOwner(MessageOwner owner) {
		this.currentOwner = owner;
	}
	
	public void add(Message message) {
		message.setMessageOwnerIfNull(currentOwner);
		if(showImmeditly) {
			System.out.println(message.toStrig());
		}
		if(message instanceof ErrorMessage) {
			errCntr++;
			if(stopImmeditly) throw new CompilationAbortedException(message.toStrig());
		}
		messages.add(message);
		if(errCntr == maxErrQnt) {
			StringBuilder sb = new StringBuilder();
			if(!showImmeditly) {
				for(Message m : messages) {
					sb.append(m).append("\n");
				}
			}
			throw new MessageContainerIsFullException(sb.toString());
		}
	}
	
	public List<Message> getMessages() {
		return messages;
	}
	
	public List<ErrorMessage> getErrorMessages() {
		List<ErrorMessage> result = new ArrayList<>();
		for(Message message : messages) {
			if(message instanceof ErrorMessage) {
				result.add((ErrorMessage)message);
			}
		}
		return result;
	}

	public boolean hasErrors() {
		for(Message message : messages) {
			if(message instanceof ErrorMessage) {
				return true;
			}
		}
		return false;
	}
}

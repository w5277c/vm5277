/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.vm5277.common.messages;

import ru.vm5277.common.exceptions.MessageContainerIsFullException;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.exceptions.CompilationAbortedException;

public class MessageContainer {
	
	private	final	List<Message>	messages		= new ArrayList<>();
	private			int				maxErrorQnt		= 8;
	private			int				errorCntr		= 0;
	private			int				warningCntr		= 0;
	private			int				lineQnt			= 0;
	private			boolean			showImmeditly	= false;
	private			boolean			stopImmeditly	= false;
	
	public MessageContainer() {
	}

	public MessageContainer(int maxErrQnt) {
		this.maxErrorQnt = maxErrQnt;
	}

	public MessageContainer(int maxErrorQnt, boolean showImmeditly, boolean stopImmeditly) {
		this.maxErrorQnt = maxErrorQnt;
		this.showImmeditly = showImmeditly;
		this.stopImmeditly = stopImmeditly;
	}

	public void add(Message message) {
		if(showImmeditly) {
			System.out.println(message.toStrig());
		}
		if(message instanceof ErrorMessage) {
			errorCntr++;
			if(stopImmeditly) throw new CompilationAbortedException(message.toStrig());
		}
		else if(message instanceof WarningMessage) {
			warningCntr++;
		}
		messages.add(message);
		if(errorCntr == maxErrorQnt) {
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
	
	public int getErrorCntr() {
		return errorCntr;
	}
	
	public int getWarningCntr() {
		return warningCntr;
	}

	public int getMaxErrorQnt() {
		return maxErrorQnt;
	}
	
	public boolean hasErrors() {
		for(Message message : messages) {
			if(message instanceof ErrorMessage) {
				return true;
			}
		}
		return false;
	}

	public void addLineQnt(int lineQnt) {
		this.lineQnt += lineQnt;
	}
	public int getLineQnt() {
		return lineQnt;
	}
}

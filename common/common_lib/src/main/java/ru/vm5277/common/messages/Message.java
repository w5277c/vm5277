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

import java.util.Objects;
import ru.vm5277.common.SourcePosition;

public class Message {
	private			String			type;
	private	final	String			text;
	private	final	SourcePosition	sp;
	
	Message(String type, String text, SourcePosition sp) {
		this.type = type;
		this.text = text;
		this.sp = sp;
	}

	public String getText() {
		return text;
	}
	
	public SourcePosition getSP() {
		return sp;
	}
	
	public String getType() {
		return type;
	}
	
	public String toStrig() {
		return type.toUpperCase() + "|" + (null == sp ? "" : sp) + "\t"  + text;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(sp, type, text);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Message) {
			Message message = (Message)obj;
			if(Objects.equals(sp, message.getSP())) {
				if(Objects.equals(type, message.getType())) {
					return Objects.equals(text, message.getText());
				}
			}
		}
		return false;
	}
}

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
package ru.vm5277.common.exceptions;

import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.messages.ErrorMessage;

public class CompileException extends Exception {
	private	final	ErrorMessage	message;

	public CompileException(ErrorMessage message) {
		super(message.getText() + (null == message.getSP() ? "" : " at " + message.getSP()));
		this.message = message;
	}

	public CompileException(String text, SourcePosition sp) {
		super(text + (null == sp ? "" : " at " + sp));
		this.message = new ErrorMessage(text, sp);
	}

	public CompileException(String text) {
		super(text);
		this.message = new ErrorMessage(text, null);
	}

	public ErrorMessage getErrorMessage() {
		return message;
	}
}

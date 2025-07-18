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
package ru.vm5277.common.cg.items;

public class CGIAsm implements CGItem {
	private	String	text;
	private	int		wSize;
	
	
	public CGIAsm(String text) {
		wSize = 0x01;
		this.text = text;
	}

	public CGIAsm(String text, int wSize) {
		this.text = text;
		this.wSize = wSize;
	}
	
	public int getWSize() {
		return wSize;
	}

	public String getText() {
		return text;
	}
	
	@Override
	public String getSource() {
		return "\t" + text + "\n";
	}
}

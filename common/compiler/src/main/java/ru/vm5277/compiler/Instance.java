/*
 * Copyright 2026 konstantin@5277.ru
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

package ru.vm5277.compiler;

import java.nio.file.Path;
import ru.vm5277.common.messages.MessageContainer;

public class Instance {
	private	Path				toolkitPath;
	private	Path				runtimePath;
	private	MessageContainer	mc;
	private	int					tabSize;
	
	public Instance(Path toolkitPath, Path runtimePath, MessageContainer mc, int tabSize) {
		this.toolkitPath = toolkitPath;
		this.runtimePath = runtimePath;
		this.mc = mc;
		this.tabSize = tabSize;
	}
	
	public Instance(int tabSize) {
		this.tabSize = tabSize;
		this.mc = new MessageContainer(100, true, false);
	}

	public Path getToolkitPath() {
		return toolkitPath;
	}
	
	public Path getRuntimePath() {
		return runtimePath;
	}
	
	public MessageContainer getMessageContainer() {
		return mc;
	}
	
	public int getTabSize() {
		return tabSize;
	}
}

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
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.enums.OptimizationType;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.ObjectTypeNode;

public class Instance {
	private			Path				toolkitPath;
	private			Path				runtimePath;
	private	final	List<String>		importedFiles	= new ArrayList<>();
	private	final	List<Path>			includePaths	= new ArrayList<>();
	private			MessageContainer	mc;
	private			int					tabSize;
	private			StrictLevel			strictLevel			= StrictLevel.LIGHT;
	private			OptimizationType	optimizationType	= OptimizationType.SIZE;
	private			int					softTimerCntr		= 0;	// Это поле должно быть в CodeGenerator'е, но его значением нужно заполнять FieldNode уже в preAnalyze
	private			ObjectTypeNode		primaryObject;
	
	public Instance(Path toolkitPath, Path runtimePath, MessageContainer mc, int tabSize) {
		this.toolkitPath = toolkitPath;
		this.runtimePath = runtimePath;
		this.mc = mc;
		this.tabSize = tabSize;
	}

	public Instance(Path toolkitPath, Path runtimePath, MessageContainer mc, int tabSize, StrictLevel strictLevel, OptimizationType optimizationType) {
		this.toolkitPath = toolkitPath;
		this.runtimePath = runtimePath;
		this.mc = mc;
		this.tabSize = tabSize;
		this.strictLevel = strictLevel;
		this.optimizationType = optimizationType;
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
	
	public StrictLevel getStrictLevel() {
		return strictLevel;
	}
	
	public OptimizationType getOptimizationType() {
		return optimizationType;
	}
	public void setOptimizationType(OptimizationType optimizationType) {
		this.optimizationType = optimizationType;
	}
	
	public int getAndIncSoftTimerCntr() {
		return softTimerCntr++;
	}
	public int getSoftTimerCntr() {
		return softTimerCntr;
	}
	
	public void setPrimaryObject(ObjectTypeNode primaryObject) {
		this.primaryObject = primaryObject;
	}
	public ObjectTypeNode getPrimaryObject() {
		return primaryObject;
	}
	
	public List<String> getImportedFiles() {
		return importedFiles;
	}
	
	public List<Path> getIncludePaths() {
		return includePaths;
	}
}

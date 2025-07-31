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
package ru.vm5277.common;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import ru.vm5277.common.messages.MessageContainer;

public interface AssemblerInterface {
	public	final	static	int										STRICT_STRONG	= 1;
	public	final	static	int										STRICT_LIGHT	= 2;
	public	final	static	int										STRICT_NONE		= 3;

	public boolean exec(MessageContainer mc, String mcu, Path sourcePath, Map<Path, SourceType> sourcePaths, int stirctLevel, String outputFileName,
						File mapFile, BufferedWriter listWriter) throws Exception;
}

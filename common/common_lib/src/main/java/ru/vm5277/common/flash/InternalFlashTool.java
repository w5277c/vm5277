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

package ru.vm5277.common.flash;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.Device;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.Logger;

public class InternalFlashTool implements FlashTool {
	public	final	static	int				MIN_FLASH_SIZE_FOR_BOOTLOADER	= 2048; //2 KBytes
	public	final	static	String			BIN_DIR							= "bin";
	public	final	static	String			FILENAME						= "j8bf";
	public	final	static	String			NAME							= "internal";
	
	private					Logger			log								= null;
	private			final	Path			toolkitPath;
	private			final	Device			device;
	private			final	boolean			supported;
	
	public InternalFlashTool(Logger log, Path toolkitPath, Device device) {
		this.log = log;
		this.toolkitPath = toolkitPath;
		this.device = device;
		
		Long flashSize = device.getDefNum("FLASH_SIZE");
		supported = null!=flashSize && MIN_FLASH_SIZE_FOR_BOOTLOADER<=flashSize;
	}
	
	@Override
	public Path resolveTool(boolean canDownload) {
		if(null==toolkitPath) return null;
		File toolFile = toolkitPath.resolve(BIN_DIR).resolve(FILENAME + (FSUtils.isWindows() ? ".exe" : ".sh")).normalize().toFile();
		if(toolFile.exists()) return Paths.get(toolFile.getAbsolutePath());
		return null;
	}

	@Override
	public List<FlashToolParam> getParams() {
		List<FlashToolParam> result = new ArrayList<>();
		return result;
	}

	public boolean isSupported() {
		return supported;
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public String toString() {
		return NAME;
	}

	@Override
	public String convertDeviceId(String deviceId) {
		return deviceId;
	}
}

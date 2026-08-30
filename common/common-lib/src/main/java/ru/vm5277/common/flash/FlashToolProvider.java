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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import ru.vm5277.common.Device;
import ru.vm5277.common.Logger;
import ru.vm5277.common.enums.PlatformType;

public class FlashToolProvider {
	public static List<FlashTool> getTools(Logger log, Path toolkitPath, PlatformType platformType, String deviceId) {
		List<FlashTool> result = new ArrayList<>();
		
		if(null!=toolkitPath) {
			if(null!=deviceId && !deviceId.isEmpty()) {
				try {
					Device device = new Device(toolkitPath, platformType, deviceId);
					InternalFlashTool internalFlashTool = new InternalFlashTool(log, toolkitPath, device);
					if(internalFlashTool.isSupported()) {
						result.add(internalFlashTool);
					}
				}
				catch(Exception ex) {
				}
			}

			if(PlatformType.AVR==platformType) {
				result.add(new AvrdudeFlashTool(log, toolkitPath));
			}

			Collections.sort(result, new Comparator<FlashTool>() {
				@Override
				public int compare(FlashTool ft1, FlashTool ft2) {
					return ft1.getName().compareTo(ft2.getName());
				}
			});
		}
		return result;
	}
	
	public static FlashTool getExternalTool(Logger log, Path toolkitPath, String flashToolName) {
		if(AvrdudeFlashTool.NAME.equalsIgnoreCase(flashToolName)) {
			return new AvrdudeFlashTool(log, toolkitPath);
		}
		return null;
	}
}

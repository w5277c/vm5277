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

package ru.vm5277.common.enums;

import java.util.HashMap;
import java.util.Map;

public enum InteractiveMode {
	AUTO(0x00),		// Flasher автоматически определяет режим на базе dbg файла
	NONE(0x01),		// Flasher не перенаправляет STDIO(RX,TX) (интеракивный режим выключен)
	OUTPUT(0x02),	// Flasher перенаправляет только STDOUT(TX) в консоль без поддержки управляющих символов
	FULL(0x03);		// Flasher перенаправляет оба потока STDIO(RX,TX) в консоль(из консоли) и поддерживает VT100 режим
	
	private static final Map<Integer, InteractiveMode> ids = new HashMap<>();
	static {
		for (InteractiveMode type : InteractiveMode.values()) {
			ids.put(type.getId(), type);
		}
	}

	private	int	id;

	private InteractiveMode(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static InteractiveMode fromInt(int id) {
		return ids.get(id);
	}
	
	public static InteractiveMode fromName(String name) {
		try {
			return valueOf(name.toUpperCase());
		}
		catch(Exception ex) {
			return null;
		}
	}
}

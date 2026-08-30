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

package ru.vm5277.common.enums;

import java.util.HashMap;
import java.util.Map;

public enum OptimizationType {
	NONE(0x00),
	FRONT(0x01),
	SIZE(0x02),
	SPEED(0x03);
	
	private static final Map<Integer, OptimizationType> ids = new HashMap<>();
	static {
		for (OptimizationType type : OptimizationType.values()) {
			ids.put(type.getId(), type);
		}
	}

	private	int	id;

	private OptimizationType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static OptimizationType fromInt(int id) {
		return ids.get(id);
	}
	
	public static OptimizationType fromName(String name) {
		try {
			return valueOf(name.toUpperCase());
		}
		catch(Exception ex) {
			return null;
		}
	}
}

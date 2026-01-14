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

package ru.vm5277.common.firmware.IntelHex;

import java.util.HashMap;
import java.util.Map;

public enum EIntelRecordType {
	DATA(0x00),
	EOF(0x01),
	EXT_SEGMENT(0x02),
	EXT_LINEAR(0x04),
	START_LINEAR(0x05),

	UNSUPPORTED(-1);

	private static final Map<Integer, EIntelRecordType> ids = new HashMap<Integer, EIntelRecordType>();
	static {
		for (EIntelRecordType type : EIntelRecordType.values()) {
			ids.put(type.getId(), type);
		}
	}

	private	int	id;

	private EIntelRecordType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static EIntelRecordType fromInt(int id) {
		EIntelRecordType resutl = ids.get(id);
		if(null == resutl) {
			resutl = EIntelRecordType.UNSUPPORTED;
		}
		return resutl;
	}
}

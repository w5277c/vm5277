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

package ru.vm5277.common.bldr;

import java.util.HashMap;
import java.util.Map;

public enum BldrResult {
	OK((byte)0x80),
	IDENTICAL((byte)0x81),
	NOT_EQUAL((byte)0x82),
	WRONG_REQUEST((byte)0x8),
	WRONG_PAGE((byte)0x89),
	WRONG_PAGESIZE((byte)0x8a),
	DENIED((byte)0x8b);

	public	static	final	byte	MAGIC	= (byte)0x77;

	private static final Map<Byte, BldrResult> ids = new HashMap<>();
	static {
		for (BldrResult type : BldrResult.values()) {
			ids.put(type.getId(), type);
		}
	}
	private					byte	id;

	private BldrResult(byte _id) {
		id = _id;
	}

	public byte getId() {
		return id;
	}
	
	public static BldrResult fromByte(byte _id) {
		return ids.get(_id);
	}
}

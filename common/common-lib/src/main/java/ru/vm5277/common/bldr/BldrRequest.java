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

public enum BldrRequest {
	INFO((byte)0x00),
	REBOOT((byte)0x01),
	PAGE_ERASE((byte)0x10),
	PAGE_VERIFY((byte)0x11),
	PAGE_WRITE((byte)0x12),
	PAGE_EVERIFY((byte)0x20),
	PAGE_EWRITE((byte)0x21);

	public	static	final	byte	MAGIC	= (byte)0x77;
	private					byte	id;
	
	private BldrRequest(byte _id) {
		id = _id;
	}

	public byte getId() {
		return id;
	}
}

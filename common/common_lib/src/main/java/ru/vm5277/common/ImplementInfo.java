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

import java.util.List;
import ru.vm5277.common.compiler.VarType;

public class ImplementInfo implements Comparable<ImplementInfo> {
	private	VarType			type;
	private	List<String>	signatures;
	
	public ImplementInfo(VarType type, List<String> signatures) {
		this.type = type;
		this.signatures = signatures;
	}
	
	public VarType getType() {
		return type;
	}
	
	public List<String> getSignatures() {
		return signatures;
	}
	
	@Override
	public int compareTo(ImplementInfo pair) {
		return Integer.compare(type.getId(), pair.getType().getId());
	}
}

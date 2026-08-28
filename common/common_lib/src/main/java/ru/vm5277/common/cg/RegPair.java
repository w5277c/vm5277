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
package ru.vm5277.common.cg;

import ru.vm5277.common.exceptions.CompileException;

public class RegPair implements Comparable<RegPair> {
	private	byte	reg;
	private	boolean	free	= true;
	
	public RegPair(byte reg) {
		this.reg = reg;
	}
	
	public RegPair(byte reg, boolean free) {
		this.reg = reg;
		this.free = free;
	}

	public byte getReg() {
		return reg;
	}
	
	public boolean isFree() {
		return free;
	}
	public void setFree(boolean free) throws CompileException {
		if(this.free==free) throw new CompileException("Double " + (free ? "free" : "use") + " for r" + reg);
		this.free = free;
	}
	
	@Override
	public String toString() {
		return Byte.toString(reg) + (free ? "[FREE]" : "[USED]");
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this==obj) return true;
		if(null==obj || getClass()!=obj.getClass()) return false;

		RegPair regPair = (RegPair) obj;
		return this.getReg() == regPair.getReg();
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(getReg());
	}
	
	@Override
	public int compareTo(RegPair pair) {
		return Byte.compare(reg, pair.getReg());
	}
}

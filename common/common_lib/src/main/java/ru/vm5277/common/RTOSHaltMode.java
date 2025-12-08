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

public enum RTOSHaltMode {
	HALT("mcu_halt"),
	BLINK("mcu_blink_forever"),
	RESET("mcu_reset"),
	BLINK_N_RESET("mcu_blink_n_reset");
	
	private	String procname;

	private RTOSHaltMode(String procname) {
		this.procname = procname;
	}
	
	public String getProcName() {
		return procname;
	}
}

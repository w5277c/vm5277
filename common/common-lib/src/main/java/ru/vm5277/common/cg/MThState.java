/*
 * Copyright 2026 konstantin@5277.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0eunary
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.common.cg;

/* Используется при выполнении процедур формирования кода (например, арифметических операций).
   Хранит информацию, необходимую для принятия решений о необходимости блокировки 
   диспетчера на время выполнения данного кода.
*/

public class MThState {
	private boolean enabled			= false;
	private boolean safe			= true;
	private boolean j8bAtomUsed		= false;

	public MThState(boolean enabled) {
		this.enabled = enabled;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public boolean isSafe() {
		return safe;
	}
	
	public boolean getJ8bAtomUsed() {
		return j8bAtomUsed;
	}
	public void setJ8bAtomUsed() {
		j8bAtomUsed = true;
	}
	
	public void setNotSafe() {
		safe = false;
	}
}

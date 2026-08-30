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

package ru.vm5277.common;

public interface Logger {
	void info(String message);
	void warn(String message);
	void error(String message);
	void debug(String message);

	/**
	* Reports progress of a long-running operation.
	*
	* @param percents completion percentage (0-100)
	* @param state    current operation state description
	* @return {@code true} to continue the operation, {@code false} to cancel it
	 */
	boolean progress(int percents, String state);
}

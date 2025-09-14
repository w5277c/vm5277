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

public enum OperandType {
	LITERAL,		//число в виде константы(сразу записывается в регистр)
	LITERAL_FIXED,	//число в виде константы в формате FIXED(сразу записывается в регистр)
	TYPE,			//reference? т.е. заполнен resId?
	FLASH_RES,		//данные размещенные во FLASH области
	LOCAL_RES,		//field или var, заполнен resId.
	ACCUM;			//аккумулятор
}

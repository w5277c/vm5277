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

.IFNDEF J8BPROC_CLASS_TYPEID
;-----------------------------------------------------------
J8BPROC_CLASS_TYPEID:
;-----------------------------------------------------------
;Возвращает идентификатор типа класса
;IN: ACCUM_L/H-адрес HEAP
;OUT: ACCUM_L
;-----------------------------------------------------------
	PUSH_Z
	MOVW ZL,ACCUM_L
	LDD ACCUM_L,Z+_OS_CLASS_HEAP_OFFSET__METADATA_ADDR+0x00	;LE
	LDD ZH,Z+_OS_CLASS_HEAP_OFFSET__METADATA_ADDR+0x01
	MOV ZL,ACCUM_L
	LPM ACCUM_L,Z											;Загружаем ид типа класса
	POP_Z
	RET
.ENDIF

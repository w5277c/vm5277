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

.IFNDEF J8BPROC_INSTANCEOF_NR
;-----------------------------------------------------------
J8BPROC_INSTANCEOF_NR:										;NR-NO_RESTORE - не восстанавливает Z
;-----------------------------------------------------------
;Проверяем реализацию интерфейса(перебор VarType ids)
;IN: Z-адрес HEAP,ACCUM_EH-VarType id
;OUT: flag Z-true(реализует)
;-----------------------------------------------------------
	PUSH TEMP_L
	ADIW ZL,0x03
	LD ACCUM_L,Z+
	LD ZH,Z
	MOV ZL,ACCUM_L
	LPM ACCUM_L,Z+											;Проверяем на ид типа класса
	CP ACCUM_L,ACCUM_EH
	BREQ _J8BPROC_INSTANCEOF_NR__END
	LPM ACCUM_L,Z+											;Получаем количество интерфейсов
_J8BPROC_INSTANCEOF_NR__LOOP:
	LPM TEMP_L,Z+											;Получаем ид типа интерфейса
	CP TEMP_L,ACCUM_EH
	BREQ _J8BPROC_INSTANCEOF_NR__END
	ADIW ZL,0x01											;Пропускаем количество методов интерфейса
	DEC ACCUM_L
	BRNE _J8BPROC_INSTANCEOF_NR__LOOP
	CLZ
_J8BPROC_INSTANCEOF_NR__END:
	POP TEMP_L
	RET
.ENDIF


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
.IFNDEF OS_ROM_READ16
;-----------------------------------------------------------
OS_ROM_READ16_NR:											;NR-NO_RESTORE - не восстанавливает IN регистры.
;-----------------------------------------------------------
;Чтение данных из FLASH и запись их в RAM
;IN: Z-адрес,X-DST адрес,ACCUM_L/H-длина
;-----------------------------------------------------------
	PUSH TEMP_L

_OS_ROM_READ16__LOOP:
	LPM TEMP_L,Z+
	ST X+,TEMP_L
	SUBI ACCUM_L,0x01
	BRNE _OS_ROM_READ16__LOOP
	SBCI ACCUM_H,0x00
	BRNE _OS_ROM_READ16__LOOP

	POP TEMP_L
	RET
.ENDIF

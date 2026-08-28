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

.IFNDEF OS_CRC8_SMBUS_NR
;-----------------------------------------------------------
OS_CRC8_SMBUS_NR:
;-----------------------------------------------------------
;Вычисление crc8 SMBUS из блока данных
;IN: X-адрес на данные, TEMP_L/H-длина данных
;OUT: ACCUM_L-8b результат
;MOD: X,ACCUM_L/H/EL,TEMP_L/H
;-----------------------------------------------------------
	LDI ACCUM_L,0x00
	LDI ACCUM_H,0x07
_OS_CRC8_SMBUS__LOOP_BYTES:
	LD ACCUM_EL,X+
	EOR ACCUM_L,ACCUM_EL

	LDI ACCUM_EL,0x08
_OS_CRC8_SMBUS__LOOP_BITS:
	LSL ACCUM_L
	BRCC PC+0x02
	EOR ACCUM_L,ACCUM_H
	DEC ACCUM_EL
	BRNE _OS_CRC8_SMBUS__LOOP_BITS
	SBIW TEMP_L,0x01
	BRNE _OS_CRC8_SMBUS__LOOP_BYTES

	RET
.ENDIF
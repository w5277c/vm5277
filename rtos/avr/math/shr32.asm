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

.IFNDEF OS_SHR32
;-----------------------------------------------------------
;Сдвиг вправо 32бит числа на 8бит величину сдвига
;IN: ACCUM_EH/EL/H/L-сдвигаемое число, TEMP_L-величина
;сдвига
;OUT: ACCUM_EH/EL/H/L результат
;-----------------------------------------------------------
OS_SHR32:
	CPI TEMP_L,0x20
	BRCC _OS_SHR32__ZERO
_OS_SHR32__LOOP:
	SUBI TEMP_L,0x01
	BRCS _OS_SHR32__END
	LSR ACCUM_EH
	ROR ACCUM_EL
	ROR ACCUM_H
	ROR ACCUM_L
	RJMP _OS_SHR32__LOOP
_OS_SHR32__ZERO:
	LDI ACCUM_EH,0x00
	LDI ACCUM_EL,0x00
	LDI ACCUM_H,0x00
	LDI ACCUM_L,0x00
_OS_SHR32__END:
	RET
.ENDIF

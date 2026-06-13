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

.IFNDEF OS_SHR16
;-----------------------------------------------------------
;Сдвиг вправо 16бит числа на 8бит величину сдвига
;IN: ACCUM_H/L-сдвигаемое число, ACCUM_EL-величина сдвига
;OUT: ACCUM_H/L результат
;-----------------------------------------------------------
OS_SHR16:
	CPI ACCUM_EL,0x10
	BRCC _OS_SHR16__ZERO
_OS_SHR16__LOOP:
	SUBI ACCUM_EL,0x01
	BRCS _OS_SHR16__END
	LSR ACCUM_H
	ROR ACCUM_L
	RJMP _OS_SHR16__LOOP
_OS_SHR16__ZERO:
	LDI ACCUM_H,0x00
	LDI ACCUM_L,0x00
_OS_SHR16__END:
	RET
.ENDIF

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
.IFNDEF PIN_SET
.include "io/pin_set_hi.asm"
.include "io/pin_set_lo.asm"
;-----------------------------------------------------------
PIN_SET:
;-----------------------------------------------------------
;Устанвливаем пину значение из аккумулятора
;IN: ACCUM_L-пин, ACCUM_H-уровень(1=HI)
;-----------------------------------------------------------
	CPI ACCUM_H,0x00
	BRNE _PIN_SET__HI
	MJMP PIN_SET_LO
_PIN_SET__HI:
	MJMP PIN_SET_HI
.ENDIF

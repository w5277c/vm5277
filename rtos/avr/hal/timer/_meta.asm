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

.IFNDEF OS_HAL_TIMER_META
;-----------------------------------------------------------
OS_HAL_TIMER_META:
;-----------------------------------------------------------
;Получаем метаданные таймера
;IN: ACCUM_EL-номер таймера,
;OUT: TEMP_H/L-метеданные
;-----------------------------------------------------------
	PUSH_Z
	PUSH ACCUM_EL

	LDI_Z TIMER_TABLE*2
	LSL ACCUM_EL
	ADD ZL,ACCUM_EL
	ADC ZH,C0x00

	LPM TEMP_L,Z+											;Загружаем информацию о таймере
	LPM TEMP_H,Z											;|bqqqqppp|pppppppp| b=8/16бит, q-максимальное кол-во бит делителя, p-битмаска prescaler
	POP ACCUM_EL

	POP_Z
	RET
.ENDIF
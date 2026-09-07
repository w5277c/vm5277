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

.IFNDEF OS_HAL_TIMER_CHECK_PERIOD
.include "conv/us_to_ticks.asm"
.include "hal/timer/_meta.asm"
;-----------------------------------------------------------
OS_HAL_TIMER_CHECK_PERIOD:
;-----------------------------------------------------------
;Инициализируем таймер
;IN: ACCUM_H/L-максимальный период, ACCUM_EL-номер таймера,
;ACCUM_EH-разрешение(8/16bit)
;MOD: ACCUM_L/H/EL/EH
;-----------------------------------------------------------
	PUSH_T16

.IF OS_FT_TIMER == 0x01
	CPI ACCUM_EL,0x00
	BREQ _OS_HAL_TIMER_CHECK_PERIOD__FAIL
.ENDIF
	CPI ACCUM_EL,OS_TIMERS_QNT
	BRCC _OS_HAL_TIMER_CHECK_PERIOD__FAIL

	CPI ACCUM_EH,0x08										;Поддерживаются только 8 и 16 бит таймеры
	BREQ OS_HAL_TIMER_CHECK_PERIOD__RES_CHECK_DONE
	CPI ACCUM_EH,0x10
	BREQ OS_HAL_TIMER_CHECK_PERIOD__RES_CHECK_DONE
	CPI ACCUM_EH,0xff										;Любой таймер
	BRNE _OS_HAL_TIMER_CHECK_PERIOD__FAIL
OS_HAL_TIMER_CHECK_PERIOD__RES_CHECK_DONE:

	MCALL OS_HAL_TIMER_META									;|bqqqqppp|pppppppp| b=8/16бит, q-максимальное кол-во бит делителя, p-битмаска prescaler

	BST TEMP_H,0x07											;Сохраняем b в флаге T (true=16бит)
	CPI ACCUM_EH,0xff										;Любой таймер
	BREQ OS_HAL_TIMER_CHECK_PERIOD__TIMER_RES_CHECK_SKIP
	CPI ACCUM_EH,0x10
	BRNE PC+0x03
	BRTC _OS_HAL_TIMER_CHECK_PERIOD__FAIL
	RJMP PC+0x02
	BRTS _OS_HAL_TIMER_CHECK_PERIOD__FAIL
OS_HAL_TIMER_CHECK_PERIOD__TIMER_RES_CHECK_SKIP:
	MCALL OS_US_TO_TICKS
	;Проверка на слишком короткий период
	CPI ACCUM_L,0x40
	CPC ACCUM_H,C0x00
	CPC ACCUM_EL,C0x00
	BRCS _OS_HAL_TIMER_CHECK_PERIOD__FAIL

	MOV ACCUM_EH,TEMP_H										;Получаем размер маски в битах
	LSL ACCUM_EH
	SWAP ACCUM_EH
	ANDI ACCUM_EH,0x0f
_OS_HAL_TIMER_CHECK_PERIOD__PRESCALER_LOOP:
	CP ACCUM_EL,C0x00										;Если кол-во тиков помещается в 16 бит и таймер 16 бит - успех
	BRTC PC+0x03
	BREQ _OS_HAL_TIMER_CHECK_PERIOD__SUCCESS
	RJMP _OS_HAL_TIMER_CHECK_PERIOD__PRESCALER_LOOP2
	CP ACCUM_H,C0x00
	CPC ACCUM_EL,C0x00
	BREQ _OS_HAL_TIMER_CHECK_PERIOD__SUCCESS				;Если кол-во тиков помещается в 8 бит - успех
_OS_HAL_TIMER_CHECK_PERIOD__PRESCALER_LOOP2:
	;Сдвигаем биты периода
	LSR ACCUM_EL
	ROR ACCUM_H
	ROR ACCUM_L
	;Сдвигаем биты делителя
	LSR TEMP_H
	ROR TEMP_L
	BRCS PC+0x03
	DEC ACCUM_EH											;Повторяем сдвиг, если текущий делитель не поддерживается
	RJMP  _OS_HAL_TIMER_CHECK_PERIOD__PRESCALER_LOOP2
	;Декремент счетчика бит делителя
	DEC ACCUM_EH
	BRNE _OS_HAL_TIMER_CHECK_PERIOD__PRESCALER_LOOP
	;Цикл закончился, значит период делится на максимальный делитель без остатка
_OS_HAL_TIMER_CHECK_PERIOD__FAIL:
	CLC
	RJMP _OS_HAL_TIMER_CHECK_PERIOD__END
_OS_HAL_TIMER_CHECK_PERIOD__SUCCESS:
	SEC
_OS_HAL_TIMER_CHECK_PERIOD__END:
	POP_T16
	CLT														;Восстанавливаем флаг T
	RET
.ENDIF
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

.IFNDEF OS_HAL_TIMER_ENABLE
;-----------------------------------------------------------
OS_HAL_TIMER_ENABLE:
;-----------------------------------------------------------
;Запускаем таймер
;IN: ACCUM_L-номер таймера
;MOD:ACCUM_L
;-----------------------------------------------------------
.IF OS_FT_TIMER == 0
	CPI ACCUM_L,0x00
	BRNE _OS_HAL_TIMER_ENABLE__CHECK_T1
	LDI ACCUM_L,(1<<OCIE0A)
	STS TIMSK0,ACCUM_L
	LDI ACCUM_L,PRTIM0
	RJMP _OS_HAL_TIMER_ENABLE__POWERON
.ENDIF
_OS_HAL_TIMER_ENABLE__CHECK_T1:
.IF OS_TIMERS_QNT > 1
	CPI ACCUM_L,0x01
	BRNE _OS_HAL_TIMER_ENABLE__CHECK_T2
	LDI ACCUM_L,(1<<OCIE1A)
	STS TIMSK1,ACCUM_L
	LDI ACCUM_L,PRTIM1
	RJMP _OS_HAL_TIMER_ENABLE__POWERON
.ENDIF
_OS_HAL_TIMER_ENABLE__CHECK_T2:
.IF OS_TIMERS_QNT > 2
	CPI ACCUM_L,0x02
	BRNE _OS_HAL_TIMER_ENABLE__CHECK_T3
	LDI ACCUM_L,(1<<OCIE2A)
	STS TIMSK2,ACCUM_L
	LDI ACCUM_L,PRTIM2
	RJMP _OS_HAL_TIMER_ENABLE__POWERON
.ENDIF
_OS_HAL_TIMER_ENABLE__CHECK_T3:
.IF OS_TIMERS_QNT > 3
	CPI ACCUM_L,0x03
	BRNE _OS_HAL_TIMER_ENABLE__CHECK_T4
	LDI ACCUM_L,(1<<OCIE3A)
	STS TIMSK3,ACCUM_L
	LDI ACCUM_L,PRTIM3
	RJMP _OS_HAL_TIMER_ENABLE__POWERON
.ENDIF
_OS_HAL_TIMER_ENABLE__CHECK_T4:
.IF OS_TIMERS_QNT > 4
	CPI ACCUM_L,0x04
	BRNE _OS_HAL_TIMER_ENABLE__CHECK_T5
	LDI ACCUM_L,(1<<OCIE4A)
	STS TIMSK4,ACCUM_L
	LDI ACCUM_L,PRTIM4
	RJMP _OS_HAL_TIMER_ENABLE__POWERON
.ENDIF
_OS_HAL_TIMER_ENABLE__CHECK_T5:
	RET
_OS_HAL_TIMER_ENABLE__POWERON:
	CLI
	LDS ACCUM_H,PRR
	COM ACCUM_L
	AND ACCUM_H,ACCUM_L
	STS PRR,ACCUM_H
	SEI
	RET
.ENDIF

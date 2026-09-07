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

.IFNDEF OS_HAL_TIMER_DISABLE
;-----------------------------------------------------------
OS_HAL_TIMER_DISABLE:
;-----------------------------------------------------------
;Останавливаем таймер
;IN: ACCUM_L-номер таймера
;MOD:ACCUM_L
;-----------------------------------------------------------
.IF OS_FT_TIMER == 0
	CPI ACCUM_L,0x00
	BRNE _OS_HAL_TIMER_DISABLE__CHECK_T1
	STS TIMSK0,C0x00
	LDI ACCUM_L,PRTIM0
	RJMP _OS_HAL_TIMER_DISABLE__POWEROFF
.ENDIF
_OS_HAL_TIMER_DISABLE__CHECK_T1:
.IF OS_TIMERS_QNT > 1
	CPI ACCUM_L,0x01
	BRNE _OS_HAL_TIMER_DISABLE__CHECK_T2
	STS TIMSK1,C0x00
	LDI ACCUM_L,PRTIM1
	RJMP _OS_HAL_TIMER_DISABLE__POWEROFF
.ENDIF
_OS_HAL_TIMER_DISABLE__CHECK_T2:
.IF OS_TIMERS_QNT > 2
	CPI ACCUM_L,0x02
	BRNE _OS_HAL_TIMER_DISABLE__CHECK_T3
	LDI ACCUM_L,PRTIM2
	RJMP _OS_HAL_TIMER_DISABLE__POWEROFF
	RET
.ENDIF
_OS_HAL_TIMER_DISABLE__CHECK_T3:
.IF OS_TIMERS_QNT > 3
	CPI ACCUM_L,0x03
	BRNE _OS_HAL_TIMER_DISABLE__CHECK_T4
	STS TIMSK3,C0x00
	LDI ACCUM_L,PRTIM3
	RJMP _OS_HAL_TIMER_DISABLE__POWEROFF
.ENDIF
_OS_HAL_TIMER_DISABLE__CHECK_T4:
.IF OS_TIMERS_QNT > 4
	CPI ACCUM_L,0x04
	BRNE _OS_HAL_TIMER_DISABLE__CHECK_T5
	STS TIMSK4,C0x00
	LDI ACCUM_L,PRTIM4
	RJMP _OS_HAL_TIMER_DISABLE__POWEROFF
.ENDIF
_OS_HAL_TIMER_DISABLE__CHECK_T5:
	RET
_OS_HAL_TIMER_DISABLE__POWEROFF:
	CLI
	LDS ACCUM_H,PRR
	OR ACCUM_H,ACCUM_L
	STS PRR,ACCUM_H
	SEI
	RET
.ENDIF

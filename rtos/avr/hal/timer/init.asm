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

.IFNDEF OS_HAL_TIMER_INIT
.include "hal/timer/set_period.asm"
.include "j8b/callback_addr.asm"
.include "core/irq_set.asm"
;-----------------------------------------------------------
OS_HAL_TIMER_INIT:
;-----------------------------------------------------------
;Устанавливаем IO регистры аппаратного таймера
;IN: Y-адрес HEAP, ACCUM_H/L-максимальный период,
;ACCUM_EL-номер таймера, ACCUM_EH-res_id
;OUT: флаг C-true-успешно
;MOD: ACCUM_L/H/EL/EH
;-----------------------------------------------------------
	PUSH RESULT
	PUSH FLAGS

	PUSH ACCUM_EH
	MCALL OS_HAL_TIMER_SET_PERIOD
	POP ACCUM_H
	BRCC _OS_HAL_TIMER_INIT__END
	MCALL J8BPROC_CALLBACK_ADDR
	MCALL OS_IRQ_SET
	SEC

_OS_HAL_TIMER_INIT__END:
	POP FLAGS
	POP RESULT
	RET
.ENDIF
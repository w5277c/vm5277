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

.IFNDEF MCU_BLINK_FOREVER
;-----------------------------------------------------------
MCU_BLINK_FOREVER:
;-----------------------------------------------------------
;Бесконечное мигание светодиодом
;-----------------------------------------------------------
	CLI
_MCU_BLINK_FOREVER__LOOP:
	; TODO проверить наличие значения в ACTLED_PORT (не равное 0xff) через IFDEF
	; TODO инвертировать порт светодиода
	; TODO пауза в 100мс?

	RJMP _MCU_BLINK_FOREVER__LOOP
.ENDIF


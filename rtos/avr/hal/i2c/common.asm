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

.IFNDEF _OS_DRV_I2C_FREQ
; Таблица частот I2C для AVR (TWBR)

.IF (CORE_FREQ/10)*9<50
    .warning "I2C driver, CORE_FREQ to low for 10KHz I2C"
    .SET __TWBR_10 = 240
.ELSE
    .SET __TWBR_10 = ((CORE_FREQ/10)-16)/8
.ENDIF

.IF (CORE_FREQ/50)*9<50
    .warning "I2C driver, CORE_FREQ to low for 50KHz I2C"
    .SET __TWBR_50 = 240
.ELSE
    .SET __TWBR_50 = ((CORE_FREQ/50)-16)/8
.ENDIF

.IF (CORE_FREQ/100)*9<50
    .warning "I2C driver, CORE_FREQ to low for 100KHz I2C"
    .SET __TWBR_100 = 240
.ELSE
    .SET __TWBR_100 = ((CORE_FREQ/100)-16)/2
.ENDIF

.IF (CORE_FREQ/400)*9<50
	.warning "I2C driver, CORE_FREQ to low for 400KHz I2C"
    .SET __TWBR_400 = 240
.ELSE
    .SET __TWBR_400 = ((CORE_FREQ/400)-16)/2
.ENDIF

_OS_DRV_I2C_FREQ:
    .db __TWBR_10, 1, __TWBR_50, 1, __TWBR_100, 0, __TWBR_400, 0
.ENDIF
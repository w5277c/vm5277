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

.include "common.asm"

;---DESCRIPTION---------------------------------------------
;Header for ATtiny13a (1KB ROM, 64B EEPROM, 64B SRAM, LE)

	.DEVICE ATtiny13a
	.EQU DEVICE_SIGNATURE							= 0x001E9007

	.EQU FLASH_SIZE									= 0x0400; 1KB(512 words)
	.EQU FLASH_PAGESIZE								= 0x20	; 32B
	.EQU EEPROM_SIZE								= 0x0040; 64B
	.EQU EEPROM_PAGESIZE							= 0x04	; 4B
	.EQU SRAM_START									= 0x0060; 96B
	.EQU SRAM_SIZE									= 0x0040; 64B

	;Самопрограммирование не поддерживается
	;.EQU BOOT_256W_ADDR								= 0x100; Начальный адрес полукилобайтного бутлоадера В СЛОВАХ

	;---SBI/CBI/SBIS/SBIC-port-defines----------------------
	.EQU	PORTA									= 0xff
	.EQU	DDRA									= 0xff
	.EQU	PINA									= 0xff
	.EQU	PORTB									= 0x18
	.EQU	DDRB									= 0x17
	.EQU	PINB									= 0x16
	.EQU	PORTC									= 0xff
	.EQU	DDRC									= 0xff
	.EQU	PINC									= 0xff
	.EQU	PORTD									= 0xff
	.EQU	DDRD									= 0xff
	.EQU	PIND									= 0xff
	.EQU	PORTE									= 0xff
	.EQU	DDRE									= 0xff
	.EQU	PINE									= 0xff
	.EQU	PORTF									= 0xff
	.EQU	DDRF									= 0xff
	.EQU	PINF									= 0xff
	.EQU	PORTG									= 0xff
	.EQU	DDRG									= 0xff
	.EQU	PING									= 0xff

	;---IO-REGISTERS----------------------------------------
	.EQU	SREG									= 0x5f
	.EQU	SPL										= 0x5d
	.EQU	MCUCSR									= 0x54
	.EQU	MCUSR									= MCUCSR
	.EQU		WDRF								= 0x03
	.EQU		BORF								= 0x02
	.EQU		EXTRF								= 0x01
	.EQU		PORF								= 0x00
	.EQU	PRR										= 0x45
	.EQU		PRTIM0								= 0x01
	.EQU		PRADC								= 0x00
	;---TIMER0---
	.EQU	TIMSK0									= 0x59
	.EQU		OCIE0B								= 0x03
	.EQU		OCIE0A								= 0x02
	.EQU		TOIE0								= 0x01
	.EQU	TCCR0A									= 0x4F
	.EQU		COM0A1								= 0x07
	.EQU		COM0A0								= 0x06
	.EQU		COM0B1								= 0x05
	.EQU		COM0B0								= 0x04
	.EQU		WGM01								= 0x01
	.EQU		WGM00								= 0x00
	.EQU	TCCR0B									= 0x53
	.EQU		FOC0A								= 0x07
	.EQU		FOC0B								= 0x06
	.EQU		WGM02								= 0x03
	.EQU		CS02								= 0x02
	.EQU		CS01								= 0x01
	.EQU		CS00								= 0x00
	.EQU	TCNT0									= 0x52
	.EQU	OCR0A									= 0x56
	.EQU	OCR0B									= 0x49
	.EQU	TIFR0									= 0x58
	.EQU		OCF0B								= 0x03
	.EQU		OCF0A								= 0x02
	.EQU		TOV0								= 0x01
	.EQU	GTCCR									= 0x48
	.EQU		TSM								= 0x07
	.EQU		PSRSYNC								= 0x00
	.EQU		PSR0								= PSRSYNC
	;---PCINT---
	.EQU	PCMSK0									= 0x35
	.EQU		PCINT5								= 0x05
	.EQU		PCINT4								= 0x04
	.EQU		PCINT3								= 0x03
	.EQU		PCINT2								= 0x02
	.EQU		PCINT1								= 0x01
	.EQU		PCINT0								= 0x00
	;---INT---
	.EQU	MCUCR									= 0x55
	.EQU		PUD									= 0x06
	.EQU		SE									= 0x05
	.EQU		SM1									= 0x04
	.EQU		SM0									= 0x03
	.EQU		ISC01								= 0x01
	.EQU		ISC00								= 0x00
	.EQU	GIMSK									= 0x5b
	.EQU		INT0								= 0x06
	.EQU		PCIE								= 0x05
	.EQU	GIFR									= 0x5a
	.EQU		INTF0								= 0x06
	.EQU		PCIF								= 0x05
	.EQU	EICRA									= MCUCR
	.EQU	EIMSK									= GIMSK
	.EQU	EIFR									= GIFR
	;---EEPROM---
	.EQU	EEARL									= 0x3E
	.EQU	EEDR									= 0x3D
	.EQU	EECR									= 0x3C
	.EQU		EEPM1								= 0x05
	.EQU		EEPM0								= 0x04
	.EQU		EERIE								= 0x03
	.EQU		EEMPE								= 0x02
	.EQU		EEMWE								= EEMPE
	.EQU		EEPE								= 0x01
	.EQU		EEWE								= EEPE
	.EQU		EERE								= 0x00
	;---ADC---
	.EQU	ADMUX									= 0x27
	.EQU		REFS0								= 0x06
	.EQU		ADLAR								= 0x05
	.EQU		MUX1								= 0x01
	.EQU		MUX0								= 0x00
	.EQU	ADCSRA									= 0x26
	.EQU		ADEN								= 0x07
	.EQU		ADSC								= 0x06
	.EQU		ADATE								= 0x05
	.EQU		ADIF								= 0x04
	.EQU		ADIE								= 0x03
	.EQU		ADPS2								= 0x02
	.EQU		ADPS1								= 0x01
	.EQU		ADPS0								= 0x00
	.EQU	ADCSRB									= 0x23
	.EQU		ACME								= 0x06
	.EQU		ADTS2								= 0x02
	.EQU		ADTS1								= 0x01
	.EQU		ADTS0								= 0x00
	.EQU	ADCL									= 0x24
	.EQU	ADCH									= 0x25
	.EQU	ADC0									= 0x00
	.EQU	ADC1									= 0x01
	.EQU	ADC2									= 0x02
	.EQU	ADC3									= 0x03
	.EQU	ADC4									= 0x04
	;--SPM---
	.EQU	SPMCSR									= 0x37
	.EQU	SPMCR									= SPMCSR
	.EQU		CTPB								= 0x04
	.EQU		RFLB								= 0x03
	.EQU		PGWRT								= 0x02
	.EQU		PGERS								= 0x01
	.EQU		SELFPR								= 0x00
	;---WATCHDOG---
	.EQU	WDTCR									= 0x41
	.EQU		WDIF								= 0x07
	.EQU		WDTIF								= WDIF
	.EQU		WDIE								= 0x06
	.EQU		WDTIE								= WDIE
	.EQU		WDP3								= 0x05
	.EQU		WDCE								= 0x04
	.EQU		WDE									= 0x03
	.EQU		WDP2								= 0x02
	.EQU		WDP1								= 0x01
	.EQU		WDP0								= 0x00

	;---ADC-VOLTAGE-REFERENCE---
	.EQU	ADC_VREF_AVCC							= (0<<REFS0)
	.EQU	ADC_VREF_1_1							= (1<<REFS0)
	;---ADC-PRESCALLER---
	.EQU	ADC_PRESC_X1							= 0x00
	.EQU	ADC_PRESC_X2							= 0x01
	.EQU	ADC_PRESC_X4							= 0x02
	.EQU	ADC_PRESC_X8							= 0x03
	.EQU	ADC_PRESC_X16							= 0x04
	.EQU	ADC_PRESC_X32							= 0x05
	.EQU	ADC_PRESC_X64							= 0x06
	.EQU	ADC_PRESC_X128							= 0x07

	;---OSCILLATOR-CALIBRATION-REGISTER---
	.EQU	OSCCAL									= 0x51

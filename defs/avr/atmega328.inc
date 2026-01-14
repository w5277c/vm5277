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
.include "common.inc"

;---DESCRIPTION---------------------------------------------
;Header for ATMega328 (32KB ROM, 1KB EEPROM, 2KB SRAM, LE)

	.DEVICE ATmega328P
	.EQU DEVICE_SIGNATURE							= 0x001E9514
	
	.EQU FLASH_SIZE									= 0x8000; 32KB(16K words)
	.EQU FLASH_PAGESIZE								= 0x80	; 128B
	.EQU EEPROM_SIZE								= 0x0400; 1KB
	.EQU EEPROM_PAGESIZE							= 0x4	; 4B
	.EQU SRAM_START									= 0x0100; 256B
	.EQU SRAM_SIZE									= 0x0800; 2KB

	.EQU BOOT_256W_ADDR								= 0x3f00; Начальный адрес полукилобайтного бутлоадера В СЛОВАХ
	.EQU BOOT_512W_ADDR								= 0x3e00; Начальный адрес килобайтного бутлоадера В СЛОВАХ

	;---IO-REGISTERS---------------------------------------
	.EQU	SREG									= 0x5f
	.EQU	SPH										= 0x5e
	.EQU	SPL										= 0x5d
	.EQU	MCUCSR									= 0x54
	.EQU	MCUSR									= MCUCSR
	.EQU		WDRF								= 0x03
	.EQU		BORF								= 0x02
	.EQU		EXTRF								= 0x01
	.EQU		PORF								= 0x00
	.EQU	PRR										= 0x64
	.EQU		PRTWI								= 0x07
	.EQU		PRTIM2								= 0x06
	.EQU		PRTIM0								= 0x05
	.EQU		PRTIM1								= 0x03
	.EQU		PRSPI								= 0x02
	.EQU		PRUSART0							= 0x01
	.EQU		PRADC								= 0x00
	;---TWI---
	.EQU	TWCR									= 0xbc
	.EQU		TWINT								= 0x07
	.EQU		TWEA								= 0x06
	.EQU		TWSTA								= 0x05
	.EQU		TWSTO								= 0x04
	.EQU		TWWC								= 0x03
	.EQU		TWEN								= 0x02
	.EQU		TWIE								= 0x00
	.EQU	TWDR									= 0xbb
	.EQU	TWAR									= 0xba
	.EQU		TWGCE								= 0x00
	.EQU	TWSR									= 0xb9
	.EQU		TWPS1								= 0x01
	.EQU		TWPS0								= 0x00
	.EQU	TWBR									= 0xb8
	;---TIMER0---
	.EQU	TIMSK0									= 0x6e
	.EQU		OCIE0B								= 0x02
	.EQU		OCIE0A								= 0x01
	.EQU		TOIE0								= 0x00
	.EQU	TCCR0A									= 0x44
	.EQU		COM0A1								= 0x07
	.EQU		COM0A0								= 0x06
	.EQU		COM0B1								= 0x05
	.EQU		COM0B0								= 0x04
	.EQU		WGM01								= 0x01
	.EQU		WGM00								= 0x00
	.EQU	TCCR0B									= 0x45
	.EQU		FOC0A								= 0x07
	.EQU		FOC0B								= 0x06
	.EQU		WGM02								= 0x03
	.EQU		CS02								= 0x02
	.EQU		CS01								= 0x01
	.EQU		CS00								= 0x00
	.EQU	TCNT0									= 0x46
	.EQU	OCR0A									= 0x47
	.EQU	OCR0B									= 0x48
	.EQU	TIFR0									= 0x35
	.EQU		OCF0B								= 0x02
	.EQU		OCF0A								= 0x01
	.EQU		TOV0								= 0x00
	;---TIMER1---
	.EQU	TIMSK1									= 0x6f
	.EQU		ICIE1								= 0x05
	.EQU		OCIE1B								= 0x02
	.EQU		OCIE1A								= 0x01
	.EQU		TOIE1								= 0x00
	.EQU	TCCR1A									= 0x80
	.EQU		COM1A1								= 0x07
	.EQU		COM1A0								= 0x06
	.EQU		COM1B1								= 0x05
	.EQU		COM1B0								= 0x04
	.EQU		WGM11								= 0x01
	.EQU		WGM10								= 0x00
	.EQU	TCCR1B									= 0x81
	.EQU		ICNC1								= 0x07
	.EQU		ICES1								= 0x06
	.EQU		WGM13								= 0x04
	.EQU		WGM12								= 0x03
	.EQU		CS12								= 0x02
	.EQU		CS11								= 0x01
	.EQU		CS10								= 0x00
	.EQU	TCCR1C									= 0x82
	.EQU		FOC1A								= 0x07
	.EQU		FOC1B								= 0x06
	.EQU	TCNT1H									= 0x85
	.EQU	TCNT1L									= 0x84
	.EQU	OCR1AH									= 0x89
	.EQU	OCR1AL									= 0x88
	.EQU	OCR1BH									= 0x8b
	.EQU	OCR1BL									= 0x8a
	.EQU	TIFR1									= 0x36
	.EQU		ICF1								= 0x05
	.EQU		OCF1B								= 0x02
	.EQU		OCF1A								= 0x01
	.EQU		TOV1								= 0x00
	;---TIMER2---
	.EQU	TIMSK2									= 0x70
	.EQU		OCIE2B								= 0x02
	.EQU		OCIE2A								= 0x01
	.EQU		TOIE2								= 0x00
	.EQU	TCCR2A									= 0xb0
	.EQU		COM2A1								= 0x07
	.EQU		COM2A0								= 0x06
	.EQU		COM2B1								= 0x05
	.EQU		COM2B0								= 0x04
	.EQU		WGM21								= 0x01
	.EQU		WGM20								= 0x00
	.EQU	TCCR2B									= 0xb1
	.EQU		FOC2A								= 0x07
	.EQU		FOC2B								= 0x06
	.EQU		WGM22								= 0x03
	.EQU		CS22								= 0x02
	.EQU		CS21								= 0x01
	.EQU		CS20								= 0x00
	.EQU	TCNT2									= 0xb2
	.EQU	OCR2A									= 0xb3
	.EQU	OCR2B									= 0xb4
	.EQU	TIFR2									= 0x37
	.EQU		OCF2B								= 0x02
	.EQU		OCF2A								= 0x01
	.EQU		TOV2								= 0x00
	;---UART---
	.EQU	UDR0									= 0xc6
	.EQU	UCSR0A									= 0xc0
	.EQU		RXC0								= 0x07
	.EQU		TXC0								= 0x06
	.EQU		UDRE0								= 0x05
	.EQU		FE0									= 0x04
	.EQU		DOR0								= 0x03
	.EQU		UPE0								= 0x02
	.EQU		U2X0								= 0x01
	.EQU		MPCM0								= 0x00
	.EQU	UCSR0B									= 0xc1
	.EQU		RXCIE0								= 0x07
	.EQU		TXCIE0								= 0x06
	.EQU		UDRIE0								= 0x05
	.EQU		RXEN0								= 0x04
	.EQU		TXEN0								= 0x03
	.EQU		UCSZ02								= 0x02
	.EQU		RXB80								= 0x01
	.EQU		TXB80								= 0x00
	.EQU	UCSR0C									= 0xc2
	.EQU		UMSEL01								= 0x07
	.EQU		UMSEL00								= 0x06
	.EQU		UPM01								= 0x05
	.EQU		UPM00								= 0x04
	.EQU		USBS0								= 0x03
	.EQU		UCSZ01								= 0x02
	.EQU		UDCRD0								= 0x02
	.EQU		UCSZ00								= 0x01
	.EQU		UCPHA0								= 0x01
	.EQU		UCPOL0								= 0x00
	.EQU		URSEL0								= 0x08	; Для совместимости
	.EQU	UBRR0H									= 0xc5
	.EQU	UBRR0L									= 0xc4
	;---PCINT---
	.EQU	PCICR									= 0x68
	.EQU		PCIE2								= 0x02
	.EQU		PCIE1								= 0x01
	.EQU		PCIE0								= 0x00
	.EQU	PCIFR									= 0x3b
	.EQU		PCIF2								= 0x02
	.EQU		PCIF1								= 0x02
	.EQU		PCIF0								= 0x00
	.EQU	PCMSK0									= 0x6b
	.EQU		PCINT7								= 0x07
	.EQU		PCINT6								= 0x06
	.EQU		PCINT5								= 0x05
	.EQU		PCINT4								= 0x04
	.EQU		PCINT3								= 0x03
	.EQU		PCINT2								= 0x02
	.EQU		PCINT1								= 0x01
	.EQU		PCINT0								= 0x00
	.EQU	PCMSK1									= 0x6c
	.EQU		PCINT14								= 0x06
	.EQU		PCINT13								= 0x05
	.EQU		PCINT12								= 0x04
	.EQU		PCINT11								= 0x03
	.EQU		PCINT10								= 0x02
	.EQU		PCINT9								= 0x01
	.EQU		PCINT8								= 0x00
	.EQU	PCMSK2									= 0x6d
	.EQU		PCINT23								= 0x07
	.EQU		PCINT22								= 0x06
	.EQU		PCINT21								= 0x05
	.EQU		PCINT20								= 0x04
	.EQU		PCINT19								= 0x03
	.EQU		PCINT18								= 0x02
	.EQU		PCINT17								= 0x01
	.EQU		PCINT16								= 0x00
	;---INT---
	.EQU	EICRA									= 0x69
	.EQU		ISC11								= 0x03
	.EQU		ISC10								= 0x02
	.EQU		ISC01								= 0x01
	.EQU		ISC00								= 0x00
	.EQU	EIMSK									= 0x3d
	.EQU		INT0								= 0x00
	.EQU		INT1								= 0x01
	.EQU	EIFR									= 0x3c
	.EQU		INTF0								= 0x00
	.EQU		INTF1								= 0x01

	;---EEPROM---
	.EQU	EEARH									= 0x42
	.EQU	EEARL									= 0x41
	.EQU	EEDR									= 0x40
	.EQU	EECR									= 0x3F
	.EQU		EEPM1								= 0x05
	.EQU		EEPM0								= 0x04
	.EQU		EERIE								= 0x03
	.EQU		EEMPE								= 0x02
	.EQU		EEMWE								= EEMPE
	.EQU		EEPE								= 0x01
	.EQU		EEWE								= EEPE
	.EQU		EERE								= 0x00
	;---ADC---
	.EQU	ADMUX									= 0x7c
	.EQU		REFS1								= 0x07
	.EQU		REFS0								= 0x06
	.EQU		ADLAR								= 0x05
	.EQU		MUX3								= 0x03
	.EQU		MUX2								= 0x02
	.EQU		MUX1								= 0x01
	.EQU		MUX0								= 0x00
	.EQU	ADCSRA									= 0x7a
	.EQU		ADEN								= 0x07
	.EQU		ADSC								= 0x06
	.EQU		ADATE								= 0x05
	.EQU		ADIF								= 0x04
	.EQU		ADIE								= 0x03
	.EQU		ADPS2								= 0x02
	.EQU		ADPS1								= 0x01
	.EQU		ADPS0								= 0x00
	.EQU	ADCSRB									= 0x7b
	.EQU		ACME								= 0x06
	.EQU		ADTS2								= 0x02
	.EQU		ADTS1								= 0x01
	.EQU		ADTS0								= 0x00
	.EQU	ADCL									= 0x78
	.EQU	ADCH									= 0x79
	.EQU	ADC0									= 0x00
	.EQU	ADC1									= 0x01
	.EQU	ADC2									= 0x02
	.EQU	ADC3									= 0x03
	.EQU	ADC4									= 0x04
	.EQU	ADC5									= 0x05
	.EQU	ADC6									= 0x06
	.EQU	ADC7									= 0x07
	.EQU	ADCT									= 0x08
	;--SPI---
	.EQU	SPCR									= 0x4c
	.EQU		SPIE								= 0x07
	.EQU		SPE									= 0x06
	.EQU		DORD								= 0x05
	.EQU		MSTR								= 0x04
	.EQU		CPOL								= 0x03
	.EQU		CPHA								= 0x02
	.EQU		SPR1								= 0x01
	.EQU		SPR0								= 0x00
	.EQU	SPSR									= 0x4d
	.EQU		SPIF								= 0x07
	.EQU		WCOL								= 0x06
	.EQU		SPI2X								= 0x00
	.EQU	SPDR									= 0x4e
	;--SPM---
	.EQU	SPMCSR									= 0x57
	.EQU	SPMCR									= SPMCSR
	.EQU		SPMIE								= 0x07
	.EQU		RWWSB								= 0x06
	.EQU		SIGRD								= 0x05
	.EQU		RWWSRE								= 0x04
	.EQU		BLBSET								= 0x03
	.EQU		PGWRT								= 0x02
	.EQU		PGERS								= 0x01
	.EQU		SPMEN								= 0x00
	;---WATCHDOG---
	.EQU	WDTCR									= 0x60
	.EQU		WDIF								= 0x07
	.EQU		WDIE								= 0x06
	.EQU		WDP3								= 0x05
	.EQU		WDCE								= 0x04
	.EQU		WDE									= 0x03
	.EQU		WDP2								= 0x02
	.EQU		WDP1								= 0x01
	.EQU		WDP0								= 0x00

	;---ADC-VOLTAGE-REFERENCE---
	.EQU	ADC_VREF_AREF							= (0<<REFS1)|(0<<REFS0)
	.EQU	ADC_VREF_AVCC							= (0<<REFS1)|(1<<REFS0)
	.EQU	ADC_VREF_1_1_CAP						= (1<<REFS1)|(1<<REFS0)
	;---ADC-PRESCALLER---
	.EQU	ADC_PRESC_X1							= 0x00
	.EQU	ADC_PRESC_X2							= 0x01
	.EQU	ADC_PRESC_X4							= 0x02
	.EQU	ADC_PRESC_X8							= 0x03
	.EQU	ADC_PRESC_X16							= 0x04
	.EQU	ADC_PRESC_X32							= 0x05
	.EQU	ADC_PRESC_X64							= 0x06
	.EQU	ADC_PRESC_X128							= 0x07

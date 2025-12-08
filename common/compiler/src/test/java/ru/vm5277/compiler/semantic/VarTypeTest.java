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

package ru.vm5277.compiler.semantic;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.AstNode;

public class VarTypeTest {
    @Test
    void numericTypesCompatibility() throws CompileException {
        assertTrue(AstNode.isCompatibleWith(null, VarType.SHORT, VarType.BYTE));
		assertTrue(AstNode.isCompatibleWith(null, VarType.INT, VarType.BYTE));
		assertTrue(AstNode.isCompatibleWith(null, VarType.INT, VarType.SHORT));
		
        assertTrue(AstNode.isCompatibleWith(null, VarType.FIXED, VarType.BOOL));
    }

    @Test
    void arrayTypesCompatibility() throws CompileException {
        VarType intArray = VarType.arrayOf(VarType.INT);
        VarType anotherIntArray = VarType.arrayOf(VarType.INT);
        assertTrue(AstNode.isCompatibleWith(null, intArray, anotherIntArray));
    }
}
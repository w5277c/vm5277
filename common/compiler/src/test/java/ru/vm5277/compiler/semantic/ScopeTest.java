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

import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class ScopeTest {
    private ClassScope globalScope;
    private ClassScope childScope;

    @BeforeEach
    void setup() throws CompileException {
        globalScope = new ClassScope("Global", null, new ArrayList<>());
        childScope = new ClassScope("Child", globalScope, new ArrayList<>());
    }

    @Test
    void resolveFieldFromParentScope() throws CompileException {
        globalScope.addField(new Symbol("x", VarType.INT, false, false));
        Symbol resolved = childScope.resolveField("x", false);
        assertNotNull(resolved);
        assertEquals(VarType.INT, resolved.getType());
    }

    @Test
    void duplicateFieldThrowsException() throws CompileException {
        globalScope.addField(new Symbol("x", VarType.INT, false, false));
        assertThrows(CompileException.class, new Executable() {
			@Override
			public void execute() throws Throwable {
				globalScope.addField(new Symbol("x", VarType.CSTR, false, false));
			}
		});
    }
}
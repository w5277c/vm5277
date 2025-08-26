/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.vm5277.compiler.semantic;

import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.compiler.semantic.ClassScope;
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
        globalScope = new ClassScope("Global", null, null);
        childScope = new ClassScope("Child", globalScope, null);
    }

    @Test
    void resolveFieldFromParentScope() throws CompileException {
        globalScope.addField(new Symbol("x", VarType.INT, false, false));
        Symbol resolved = childScope.resolveSymbol("x");
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
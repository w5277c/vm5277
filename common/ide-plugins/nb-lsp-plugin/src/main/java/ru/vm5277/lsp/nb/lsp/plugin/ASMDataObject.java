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

package ru.vm5277.lsp.nb.lsp.plugin;

import java.io.IOException;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@Messages({
	"LBL_VMASM_LOADER=Files of ASM"
})
@MIMEResolver.ExtensionRegistration(
		displayName = "#LBL_VMASM_LOADER",
		mimeType = "text/x-vm5277-asm",
		extension = {"vmasm", "asm", "ASM"},
		position = 1010
)
@DataObject.Registration(
		mimeType = "text/x-vm5277-asm",
		iconBase = "ru/vm5277/lsp/nb/lsp/plugin/list_16.png",
		displayName = "#LBL_VMASM_LOADER",
		position = 310
)

public class ASMDataObject extends MultiDataObject {
	public ASMDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
		super(pf, loader);
		registerEditor("text/x-vm5277-asm", true);
	}

	@Override
	protected int associateLookup() {
		return 1;
	}

	@MultiViewElement.Registration(
			displayName = "#LBL_VMASM_EDITOR",
			iconBase = "ru/vm5277/lsp/nb/lsp/plugin/list_16.png",
			mimeType = "text/x-vm5277-asm",
			persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
			preferredID = "ASM",
			position = 1010
	)
	
	@Messages("LBL_VMASM_EDITOR=Source")
	public static MultiViewEditorElement createEditor(Lookup lkp) {
		return new MultiViewEditorElement(lkp);
	}
}

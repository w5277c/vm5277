// CompileASMToolbarAction.java
package ru.vm5277.lsp.nb.lsp.plugin.asm;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.ImageUtilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

public class ASMCompileAction extends AbstractAction {
    
    public ASMCompileAction() {
        putValue(NAME, "Compile ASM");
               WindowManager.getDefault().getRegistry().addPropertyChangeListener(evt -> {
            if ("activated".equals(evt.getPropertyName())) {
                setEnabled(checkFile());
            }
        });
	
	//putValue(SMALL_ICON, ImageUtilities.loadImageIcon("ru/vm5277/lsp/nb/lsp/plugin/buildASM24.png", false));
  //  putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("ru/vm5277/lsp/nb/lsp/plugin/buildASM24.png", false));
			   
	
ImageIcon buildIcon = ImageUtilities.loadImageIcon("ru/vm5277/lsp/nb/lsp/plugin/buildASM24.png", false);
    
    if (buildIcon != null) {
        // Создаем составную иконку с текстом
        Image combined = createCombinedIcon(buildIcon.getImage(), false);
        putValue(SMALL_ICON, new ImageIcon(combined));
    }
	
        WindowManager.getDefault().getRegistry().addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("activated".equals(evt.getPropertyName())) {
					setEnabled(checkFile());
				}
			}
		});
			   
        setEnabled(checkFile());
    }
    
  private boolean checkFile() {
    TopComponent active = WindowManager.getDefault().getRegistry().getActivated();
    if (active != null) {
        DataObject dataObject = active.getLookup().lookup(DataObject.class);
        if (dataObject != null) {
            FileObject file = dataObject.getPrimaryFile();
            boolean result = (file != null && "text/x-vm5277-asm".equals(file.getMIMEType()));
			if(result) {
				String fileName = dataObject.getPrimaryFile().getName();
				putValue(NAME, "Compile " + fileName);
				putValue(SHORT_DESCRIPTION, "Compile " + fileName + ".asm");
			}
			else {
				putValue(NAME, "Compile ASM");
				putValue(SHORT_DESCRIPTION, "Compile ASM file");
			}
			return result;
        }
    }
    return false;
}
    @Override
    public void actionPerformed(ActionEvent ev) {
        if (!checkFile()) {
            return;
        }
        // Здесь код компиляции
    }
	
	
	protected Image createCombinedIcon(Image original, boolean isSmallIcon) {
		String text = isSmallIcon ? "A" : "asm";
		int w = original.getWidth(null);
		int h = original.getHeight(null);
		BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = combined.createGraphics();

		// Рисуем оригинальную иконку
		g.drawImage(original, 0, 0, null);

		// Настраиваем шрифт для наложения
		g.setFont(new Font("Arial", Font.BOLD, 10));
		g.setColor(Color.WHITE);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Рисуем текст поверх
		FontMetrics fm = g.getFontMetrics();
		int textWidth = fm.stringWidth(text);
		int x = w - textWidth - 2;
		int y = h - 2;

		// Черный контур для читаемости
		g.setColor(Color.BLACK);
		g.drawString(text, x - 1, y - 1);
		g.drawString(text, x + 1, y - 1);
		g.drawString(text, x - 1, y + 1);
		g.drawString(text, x + 1, y + 1);

		g.setColor(Color.WHITE);
		g.drawString(text, x, y);

		g.dispose();
		return combined;
	}
}
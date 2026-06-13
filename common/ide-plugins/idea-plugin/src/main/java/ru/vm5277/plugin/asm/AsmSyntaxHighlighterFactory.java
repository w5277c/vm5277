package ru.vm5277.plugin.asm;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.plugin.EmptySyntaxHighlighter;

import javax.swing.text.DefaultHighlighter;
import java.io.File;
import java.util.Properties;

public class AsmSyntaxHighlighterFactory extends SyntaxHighlighterFactory {

    @NotNull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile) {
		PlatformType platformType = null;
		String mcu = "atmega328p";

		MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
		if(manager!=null && !manager.getProjects().isEmpty()) {
			for (MavenProject mavenProject : manager.getProjects()) {
				if (null != platformType) break;

				MavenId mavenId = mavenProject.getMavenId();
				if (mavenId.getGroupId().equalsIgnoreCase("ru.vm5277.rtos")) {
					platformType = PlatformType.fromName(mavenId.getArtifactId());
					if (null != platformType) {
						break;
					}
				}
			}
		}
		if(null==platformType) {
			String target = FSUtils.getTargetFromAsmPrebild(new File(virtualFile.getPath()));
			if(null!=target) {
				try {
					int index = target.indexOf(":");
					String platformName = target.substring(0x00, index).trim();
					String mcuStr = target.substring(index+0x01).trim();
					platformType = PlatformType.fromName(platformName);
					mcu = mcuStr;
				}
				catch (Exception ex) {}
			}
		}

		if(null!=platformType) {
			return new AsmSyntaxHighlighter(project, virtualFile, platformType, mcu);
		}
		return new EmptySyntaxHighlighter();
    }
}
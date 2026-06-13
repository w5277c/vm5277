package ru.vm5277.plugin.old;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.lsp.server.ASMLanguageServer;
import ru.vm5277.lsp.server.J8BLanguageServer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class LanguageServerManager {
    private static  J8BLanguageServer j8bServer = new J8BLanguageServer();
    private static final ConcurrentHashMap<String, ASMLanguageServer> servers = new ConcurrentHashMap<>();

    public static J8BLanguageServer getJ8BServer() {
        return j8bServer;
    }
    public static ASMLanguageServer getASMServer(Project project, VirtualFile file) {
        // Ключ - путь к проекту или файлу
        String key = project.getBasePath();

        return servers.computeIfAbsent(key, new Function<String, ASMLanguageServer>() {
            @Override
            public ASMLanguageServer apply(String k) {
                PlatformType platform = detectPlatform(project, file);
                return new ASMLanguageServer(platform, "atmega328");
            }
        });
    }

    private static PlatformType detectPlatform(Project project, VirtualFile file) {
        return PlatformType.AVR;
    }
        /*
        MavenProjectsManager mavenManager = MavenProjectsManager.getInstance(project);
        Collection<MavenProject> mavenProjects = mavenManager.getProjects();
        for (MavenProject mavenProject : mavenProjects) {
            // Координаты
            String groupId = mavenProject.getGroupId();
            String artifactId = mavenProject.getArtifactId();
            String version = mavenProject.getVersion();

            // Свойства
            String platform = mavenProject.getProperties().getProperty("platform");
            String mcu = mavenProject.getProperties().getProperty("mcu");

            // Родительский POM
            MavenProject parent = mavenProject.getParent();

            // Файл pom.xml
            VirtualFile pomFile = mavenProject.getFile();
        }

        return PlatformType.AVR;
    }*/
}
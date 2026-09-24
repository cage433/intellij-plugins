package cage433.intellij.frametitle;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.FrameTitleBuilder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A window title that is nothing but the name of the directory identifying the project - the
 * deepest one that tells it apart from the other projects open at the time. With a dozen sbt
 * worktrees open, all of them projects named topaz living in ~/tz/topaz-NN/topaz, the platform's
 * title says topaz [~/tz/topaz-01/topaz] - Foo.scala [module] and this one says topaz-01.
 */
public class ProjectOnlyFrameTitleBuilder extends FrameTitleBuilder {

    @Override
    public String getProjectTitle(Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return project.getName();
        }
        Path path = Path.of(basePath);
        List<Path> others = otherOpenProjectPaths(project);
        for (int fromEnd = 0; fromEnd < path.getNameCount(); fromEnd++) {
            int index = path.getNameCount() - fromEnd - 1;
            Path suffix = path.subpath(index, path.getNameCount());
            if (others.stream().noneMatch(other -> other.endsWith(suffix))) {
                return path.getName(index).toString();
            }
        }
        return project.getName();
    }

    /**
     * The file half of the title, which the whole point here is to be rid of. Blank parts are
     * dropped by ProjectFrameHelper rather than left with a dangling separator.
     */
    @Override
    public String getFileTitle(Project project, VirtualFile file) {
        return "";
    }

    private List<Path> otherOpenProjectPaths(Project project) {
        List<Path> paths = new ArrayList<>();
        for (Project open : ProjectManager.getInstance().getOpenProjects()) {
            String basePath = open.getBasePath();
            if (open != project && basePath != null) {
                paths.add(Path.of(basePath));
            }
        }
        return paths;
    }
}

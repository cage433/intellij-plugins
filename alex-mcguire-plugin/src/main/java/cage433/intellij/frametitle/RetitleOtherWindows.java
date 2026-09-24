package cage433.intellij.frametitle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.FrameTitleBuilder;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

import javax.swing.JFrame;

/**
 * A window works out its title when it has reason to redraw it, so windows already open when
 * another project opens or closes keep a title worked out against the old set of projects: the
 * first topaz worktree stays `topaz` after its siblings arrive. This retitles the other windows
 * whenever the set changes - on project open as a startup activity, on close as a listener.
 */
public class RetitleOtherWindows implements ProjectActivity, ProjectCloseListener {

    @Override
    public Object execute(Project project, Continuation<? super Unit> continuation) {
        retitleOtherWindows(project);
        return Unit.INSTANCE;
    }

    @Override
    public void projectClosed(Project project) {
        retitleOtherWindows(project);
    }

    private void retitleOtherWindows(Project changed) {
        ApplicationManager.getApplication().invokeLater(() -> {
            FrameTitleBuilder titleBuilder = FrameTitleBuilder.getInstance();
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                if (project == changed || project.isDisposed()) {
                    continue;
                }
                JFrame frame = WindowManager.getInstance().getFrame(project);
                if (frame != null) {
                    frame.setTitle(titleBuilder.getProjectTitle(project));
                }
            }
        });
    }
}

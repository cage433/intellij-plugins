package cage433.intellij.projecttabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * Keeps the tabs in order as projects come and go: a project opening adds its tab at the end, and
 * one closing leaves a gap, neither of which respects {@link ProjectTabOrder}.
 */
public class SortTabsOnProjectChange implements ProjectActivity, ProjectCloseListener {

    @Override
    public Object execute(Project project, Continuation<? super Unit> continuation) {
        sortLater(project);
        return Unit.INSTANCE;
    }

    /**
     * The closing project keeps the selection for as long as it is still there, so nothing is
     * asked to stay selected here - the platform settles that on its own.
     */
    @Override
    public void projectClosed(Project project) {
        sortLater(null);
    }

    private void sortLater(Project active) {
        ApplicationManager.getApplication().invokeLater(() -> ProjectTabs.sort(active));
    }
}

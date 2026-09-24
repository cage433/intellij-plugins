package cage433.intellij.projecttabs;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * Sorts the project tabs on demand. {@link SortTabsOnProjectChange} keeps them sorted as projects
 * come and go, so this is for putting them back after dragging one about, and for the times the
 * automatic sort has not noticed something.
 */
public class SortProjectTabsAction extends AnAction {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(ProjectTabs.canSort());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        ProjectTabs.sort(e.getProject());
    }
}

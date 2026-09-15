package cage433.intellij.instancecreations;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScopesCore;
import org.jetbrains.plugins.scala.findUsages.factory.ScalaFindUsagesConfiguration;
import org.jetbrains.plugins.scala.findUsages.factory.ScalaTypeDefinitionFindUsagesOptions;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass;

public class FindInstanceCreationsAction extends AnAction {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null && targetClass(e) != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        ScClass scClass = targetClass(e);
        if (project == null || scClass == null) {
            return;
        }
        FindUsagesManager manager = ((FindManagerImpl) FindManager.getInstance(project)).getFindUsagesManager();
        FindUsagesHandler handler = manager.getFindUsagesHandler(scClass, false);
        if (handler == null) {
            return;
        }
        manager.findUsages(instanceCreationElements(project, handler), PsiElement.EMPTY_ARRAY, handler,
                productionScopeOptions(project), false);
    }

    private PsiElement[] instanceCreationElements(Project project, FindUsagesHandler handler) {
        ScalaTypeDefinitionFindUsagesOptions options =
                ScalaFindUsagesConfiguration.getInstance(project).getTypeDefinitionOptions();
        boolean saved = options.isOnlyNewInstances();
        try {
            options.isOnlyNewInstances_$eq(true);
            return handler.getPrimaryElements();
        } finally {
            options.isOnlyNewInstances_$eq(saved);
        }
    }

    private ScalaTypeDefinitionFindUsagesOptions productionScopeOptions(Project project) {
        ScalaTypeDefinitionFindUsagesOptions options = new ScalaTypeDefinitionFindUsagesOptions(project);
        options.isUsages = true;
        options.isSearchForTextOccurrences = false;
        options.searchScope = GlobalSearchScopesCore.projectProductionScope(project);
        return options;
    }

    private ScClass targetClass(AnActionEvent e) {
        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (element == null) {
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            if (editor != null) {
                element = TargetElementUtil.findTargetElement(editor,
                        TargetElementUtil.ELEMENT_NAME_ACCEPTED | TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED);
            }
        }
        return element instanceof ScClass ? (ScClass) element : null;
    }
}

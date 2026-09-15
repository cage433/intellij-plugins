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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.plugins.scala.findUsages.factory.ScalaTypeDefinitionFindUsagesOptions;
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition;
import scala.Option;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.List;

public abstract class InstanceCreationsAction extends AnAction {

    protected abstract SearchScope searchScope(Project project);

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null && targetClass(e) != null);
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
        manager.findUsages(instanceCreationElements(scClass), PsiElement.EMPTY_ARRAY, handler,
                findUsagesOptions(project), false);
    }

    private PsiElement[] instanceCreationElements(ScClass scClass) {
        List<PsiElement> elements = new ArrayList<>(CollectionConverters.asJava(scClass.constructors()));
        ScTemplateDefinition companion = companionOf(scClass);
        if (companion != null) {
            scala.collection.Iterator<PsiMethod> applyMethods = companion.allFunctionsByName("apply");
            while (applyMethods.hasNext()) {
                PsiMethod method = applyMethods.next();
                if (buildsInstanceOf(method, scClass)) {
                    elements.add(method);
                }
            }
        }
        return elements.toArray(PsiElement.EMPTY_ARRAY);
    }

    private ScTemplateDefinition companionOf(ScClass scClass) {
        Option<ScTypeDefinition> declared = ScalaPsiUtil.getCompanionModule(scClass);
        if (declared.isDefined()) {
            return declared.get();
        }
        Option<ScObject> synthetic = scClass.fakeCompanionModule();
        return synthetic.isDefined() ? synthetic.get() : null;
    }

    private boolean buildsInstanceOf(PsiMethod method, ScClass scClass) {
        PsiType returnType = method.getReturnType();
        if (!(returnType instanceof PsiClassType)) {
            return true;
        }
        PsiClass resolved = ((PsiClassType) returnType).resolve();
        String qualifiedName = scClass.qualifiedName();
        if (resolved == null || qualifiedName == null) {
            return true;
        }
        return qualifiedName.equals(resolved.getQualifiedName());
    }

    private ScalaTypeDefinitionFindUsagesOptions findUsagesOptions(Project project) {
        ScalaTypeDefinitionFindUsagesOptions options = new ScalaTypeDefinitionFindUsagesOptions(project);
        options.isUsages = true;
        options.isSearchForTextOccurrences = false;
        options.searchScope = searchScope(project);
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

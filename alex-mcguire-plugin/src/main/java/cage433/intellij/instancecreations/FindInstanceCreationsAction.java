package cage433.intellij.instancecreations;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;

public class FindInstanceCreationsAction extends InstanceCreationsAction {

    @Override
    protected SearchScope searchScope(Project project) {
        return GlobalSearchScope.projectScope(project);
    }
}

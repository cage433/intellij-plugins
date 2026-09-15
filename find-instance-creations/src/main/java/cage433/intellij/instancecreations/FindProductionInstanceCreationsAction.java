package cage433.intellij.instancecreations;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.SearchScope;

public class FindProductionInstanceCreationsAction extends InstanceCreationsAction {

    @Override
    protected SearchScope searchScope(Project project) {
        return GlobalSearchScopesCore.projectProductionScope(project);
    }
}

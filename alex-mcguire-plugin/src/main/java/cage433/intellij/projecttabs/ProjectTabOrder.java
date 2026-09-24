package cage433.intellij.projecttabs;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The order project tabs are wanted in: everything that is not a topaz worktree first, in
 * alphabetical order, then topaz-main, then the development worktrees topaz-01 .. topaz-NN in
 * numerical order, and the PR review worktrees topaz-pr-01 .. topaz-pr-NN last.
 */
public final class ProjectTabOrder {

    private static final Pattern MAIN = Pattern.compile("topaz-main");
    private static final Pattern DEVELOPMENT = Pattern.compile("topaz-(\\d+)");
    private static final Pattern PULL_REQUEST = Pattern.compile("topaz-pr-(\\d+)");

    private static final int OTHER = 0;
    private static final int TOPAZ_MAIN = 1;
    private static final int TOPAZ_DEVELOPMENT = 2;
    private static final int TOPAZ_PULL_REQUEST = 3;

    private ProjectTabOrder() {
    }

    public static Comparator<Path> comparator() {
        return Comparator.comparingInt(ProjectTabOrder::group)
                .thenComparingInt(ProjectTabOrder::ordinal)
                .thenComparing(ProjectTabOrder::name, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * The directory that names a project. A topaz worktree lives at ~/tz/topaz-01/topaz, so the
     * directory that says which worktree it is sits one above the project itself.
     */
    static String name(Path basePath) {
        String directory = basePath.getFileName().toString();
        Path parent = basePath.getParent();
        if (parent != null && parent.getFileName() != null && !isTopazWorktree(directory)) {
            String above = parent.getFileName().toString();
            if (isTopazWorktree(above)) {
                return above;
            }
        }
        return directory;
    }

    private static boolean isTopazWorktree(String name) {
        return MAIN.matcher(name).matches()
                || DEVELOPMENT.matcher(name).matches()
                || PULL_REQUEST.matcher(name).matches();
    }

    private static int group(Path basePath) {
        String name = name(basePath);
        if (MAIN.matcher(name).matches()) {
            return TOPAZ_MAIN;
        }
        if (PULL_REQUEST.matcher(name).matches()) {
            return TOPAZ_PULL_REQUEST;
        }
        if (DEVELOPMENT.matcher(name).matches()) {
            return TOPAZ_DEVELOPMENT;
        }
        return OTHER;
    }

    private static int ordinal(Path basePath) {
        String name = name(basePath);
        Matcher pullRequest = PULL_REQUEST.matcher(name);
        if (pullRequest.matches()) {
            return Integer.parseInt(pullRequest.group(1));
        }
        Matcher development = DEVELOPMENT.matcher(name);
        if (development.matches()) {
            return Integer.parseInt(development.group(1));
        }
        return 0;
    }
}

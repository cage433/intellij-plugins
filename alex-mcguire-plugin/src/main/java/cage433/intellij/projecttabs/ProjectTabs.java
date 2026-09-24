package cage433.intellij.projecttabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.ui.mac.WindowTabsComponent;
import com.intellij.ui.mac.foundation.Foundation;
import com.intellij.ui.mac.foundation.ID;
import com.intellij.ui.mac.foundation.MacUtil;
import com.intellij.ui.tabs.TabInfo;

import javax.swing.JFrame;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lays the project tabs out in the order of {@link ProjectTabOrder}.
 *
 * <p>Two orders have to be set. The tabs on show are IntelliJ's own - a JBTabs whose TabInfo
 * objects are the frames. The windows are also a native macOS tab group, and that is the order
 * macOS's own Show Next Tab walks, so sorting only the first leaves tab switching going its own
 * way.
 *
 * <p>The native half follows WindowTabsComponent.moveTabToNewIndex, which is what dragging a tab
 * runs: take the window out of its NSWindowTabGroup and insert it back at the wanted index, on the
 * AppKit main thread. Doing it any other way is not a detail - calling addTabbedWindow:ordered: on
 * the EDT for a window already in the group takes AppKit through NSWindowStackController and
 * SIGTRAPs the IDE.
 */
public final class ProjectTabs {

    private ProjectTabs() {
    }

    public static boolean canSort() {
        return SystemInfo.isMac && orderedFrames().size() > 1;
    }

    /**
     * @param active the project whose window should stay selected, if it should stay selected at
     *               all - a window is selected by moving it, so without this the window that
     *               happened to move last would win.
     */
    public static void sort(Project active) {
        if (!SystemInfo.isMac) {
            return;
        }
        List<IdeFrameImpl> frames = orderedFrames();
        if (frames.size() < 2) {
            return;
        }
        sortVisibleTabs(frames);
        sortNativeTabGroup(frames, active);
    }

    private static List<IdeFrameImpl> orderedFrames() {
        Map<IdeFrameImpl, Path> paths = new IdentityHashMap<>();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            String basePath = project.getBasePath();
            JFrame frame = WindowManager.getInstance().getFrame(project);
            if (basePath != null && frame instanceof IdeFrameImpl ideFrame) {
                paths.put(ideFrame, Path.of(basePath));
            }
        }
        List<IdeFrameImpl> frames = new ArrayList<>(paths.keySet());
        frames.sort(Comparator.comparing(paths::get, ProjectTabOrder.comparator()));
        return frames;
    }

    private static void sortVisibleTabs(List<IdeFrameImpl> frames) {
        Map<Object, Integer> positions = new IdentityHashMap<>();
        for (int i = 0; i < frames.size(); i++) {
            positions.put(frames.get(i), i);
        }
        Comparator<TabInfo> byPosition =
                Comparator.comparingInt(tab -> positions.getOrDefault(tab.getObject(), Integer.MAX_VALUE));
        for (IdeFrameImpl frame : frames) {
            WindowTabsComponent tabs = findTabsComponent(frame.getRootPane());
            if (tabs != null) {
                tabs.sortTabs(byPosition);
                recalculateIndexes(tabs);
            }
        }
    }

    /**
     * The component keeps a frame-to-index map of its own, used when a tab is later inserted or
     * removed. Its refresh is private, and a stale map is only worth a misplaced tab, so a failure
     * here is not worth reporting.
     */
    private static void recalculateIndexes(WindowTabsComponent tabs) {
        try {
            Method recalculate = WindowTabsComponent.class.getDeclaredMethod("recalculateIndexes");
            recalculate.setAccessible(true);
            recalculate.invoke(tabs);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    /**
     * Each window is moved to its place in turn, lowest index first, so that by the time a window
     * is placed everything before it is already where it belongs. Windows that are not in the same
     * tab group - a project opened in a window of its own - are left where they are rather than
     * dragged into the group.
     */
    private static void sortNativeTabGroup(List<IdeFrameImpl> frames, Project active) {
        JFrame activeFrame = active == null || active.isDisposed()
                ? null
                : WindowManager.getInstance().getFrame(active);
        Foundation.executeOnMainThread(true, false, () -> {
            ID tabGroup = tabGroupOf(frames.get(0));
            if (ID.NIL.equals(tabGroup)) {
                return;
            }
            for (int i = 0; i < frames.size(); i++) {
                ID window = MacUtil.getWindowFromJavaWindow(frames.get(i));
                if (ID.NIL.equals(window) || !tabGroup.equals(tabGroupOf(frames.get(i)))) {
                    continue;
                }
                Foundation.invoke(tabGroup, "removeWindow:", window);
                Foundation.invoke(tabGroup, "insertWindow:atIndex:", window, i);
            }
            if (activeFrame != null) {
                ID selected = MacUtil.getWindowFromJavaWindow(activeFrame);
                if (!ID.NIL.equals(selected)) {
                    Foundation.invoke(tabGroup, "setSelectedWindow:", selected);
                }
            }
        });
    }

    private static ID tabGroupOf(IdeFrameImpl frame) {
        ID window = MacUtil.getWindowFromJavaWindow(frame);
        return ID.NIL.equals(window) ? ID.NIL : Foundation.invoke(window, "tabGroup");
    }

    private static WindowTabsComponent findTabsComponent(Container container) {
        if (container == null) {
            return null;
        }
        for (Component child : container.getComponents()) {
            if (child instanceof WindowTabsComponent tabs) {
                return tabs;
            }
            if (child instanceof Container nested) {
                WindowTabsComponent tabs = findTabsComponent(nested);
                if (tabs != null) {
                    return tabs;
                }
            }
        }
        return null;
    }
}

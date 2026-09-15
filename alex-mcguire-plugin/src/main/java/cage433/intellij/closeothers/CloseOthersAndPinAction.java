package cage433.intellij.closeothers;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;

public class CloseOthersAndPinAction extends AnAction {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(focusedWindow(e) != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        EditorWindow focusedWindow = focusedWindow(e);
        if (project == null || focusedWindow == null) {
            return;
        }
        VirtualFile focusedFile = focusedWindow.getSelectedFile();
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        for (EditorWindow window : manager.getWindows()) {
            for (VirtualFile file : new ArrayList<>(window.getFileList())) {
                if (window == focusedWindow && focusedFile.equals(file)) {
                    continue;
                }
                window.setFilePinned(file, false);
                manager.closeFile(file, window);
            }
        }
        focusedWindow.setFilePinned(focusedFile, true);
    }

    private EditorWindow focusedWindow(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return null;
        }
        EditorWindow window = FileEditorManagerEx.getInstanceEx(project).getCurrentWindow();
        return window != null && window.getSelectedFile() != null ? window : null;
    }
}

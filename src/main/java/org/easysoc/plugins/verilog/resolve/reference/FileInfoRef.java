package org.easysoc.plugins.verilog.resolve.reference;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileInfoRef extends PsiReferenceBase<PsiElement> {

  public FileInfoRef(@NotNull PsiElement element, TextRange textRange) {
    super(element, textRange);
  }
  
  @Nullable
  @Override
  public PsiElement resolve() {
    return new MyFakePsiElement();
  }

  public class MyFakePsiElement extends FakePsiElement implements SyntheticElement {
    @Override
    public PsiElement getParent() {
      return myElement;
    }

    @Override
    public void navigate(boolean requestFocus) {

      String[] token = myElement.getText().split(" ");
      String filename = token[1].substring(2);
      String[] pos = token[2].split(":");
      int line = Integer.valueOf(pos[0]) - 1;
      int column = Integer.valueOf(pos[1].replace("]", "")) - 1;

      Project project = myElement.getProject();
      PsiFile[] files = FilenameIndex.getFilesByName(project,filename, GlobalSearchScope.projectScope(project));
      int length = files.length;
      if (length != 0){
        if (length == 1) {
          new OpenFileDescriptor(project, files[0].getVirtualFile(),line,column).navigate(true);
        } else {
          List<String> list = new ArrayList<>();
          final Map<String, VirtualFile> map = new HashMap<>();
          for (PsiFile file : files) {
            VirtualFile vFile = file.getVirtualFile();
            String relativePath = vFile.getCanonicalPath().replace(project.getBasePath(), "").substring(1);

            map.put(relativePath, vFile);
            list.add(relativePath);
          }
          JBPopupFactory.getInstance().createPopupChooserBuilder(list)
                  .setTitle("File Chooser")
                  .setItemChosenCallback((chosenFile) -> new OpenFileDescriptor(project, map.get(chosenFile),line,column).navigate(true))
                  .createPopup().showInFocusCenter();
        }
      } else {
        Notifications.Bus.notify(new Notification("FileInfo","",filename + " not found!",
                NotificationType.ERROR));
      }
    }
  }
}

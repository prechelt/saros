package saros.intellij.filesystem;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import saros.filesystem.IPath;
import saros.filesystem.IReferencePoint;

public class IntelliJReferencePointManagerTest {

  IReferencePoint referencePoint;
  IPath fileReferencePointRelativePath;
  IPath folderReferencePointRelativePath;
  IPath projectFullPath;
  IntelliJReferencePointManager intelliJReferencePointManager;
  VirtualFile file;
  VirtualFile folder;
  VirtualFile moduleRoot;
  String moduleRootPath;
  ModuleRootManager manager;

  @Before
  public void setup() {
    // moduleRootPath = "path/to/foo";
    moduleRootPath = "";
    referencePoint = EasyMock.createMock(IReferencePoint.class);

    EasyMock.replay(referencePoint);

    intelliJReferencePointManager = new IntelliJReferencePointManager();
  }

  @Test
  public void testCreateReferencePoint() {
    IReferencePoint referencePoint =
        IntelliJReferencePointManager.create(
            createModule(createModuleRootManager(createVirtualRoot(moduleRootPath, "/asd", null))));
    Assert.assertNotNull(referencePoint);
  }

  @Test
  public void testModulePutIfAbsent() {
    Module module =
        createModule(createModuleRootManager(createVirtualRoot(moduleRootPath, "/asd", null)));
    IReferencePoint referencePoint = IntelliJReferencePointManager.create(module);
    intelliJReferencePointManager.putIfAbsent(module);
    Module module2 = intelliJReferencePointManager.getModule(referencePoint);

    Assert.assertNotNull(module2);
  }

  @Test
  public void testPairPutIfAbsent() {
    Module module =
        createModule(createModuleRootManager(createVirtualRoot(moduleRootPath, "/asd", null)));
    intelliJReferencePointManager.putIfAbsent(referencePoint, module);
    Module module2 = intelliJReferencePointManager.getModule(referencePoint);

    Assert.assertNotNull(module2);
  }

  @Test
  public void testGetResource() {
    VirtualFile file = EasyMock.createMock(VirtualFile.class);
  }

  private IPath createReferencePointPath() {
    IPath fileReferencePointRelativePath = EasyMock.createMock(IPath.class);
    EasyMock.expect(fileReferencePointRelativePath.isAbsolute()).andStubReturn(false);
    EasyMock.expect(fileReferencePointRelativePath.segmentCount()).andStubReturn(2);

    return fileReferencePointRelativePath;
  }

  private Module createModule(ModuleRootManager moduleRootManager) {
    Module module = EasyMock.createMock(Module.class);
    EasyMock.expect(module.getComponent(ModuleRootManager.class)).andStubReturn(moduleRootManager);
    EasyMock.expect(module.getName()).andStubReturn("Module1");
    EasyMock.replay(module);

    return module;
  }

  private VirtualFile createVirtualRoot(
      String moduleRootPath, String pathToFile, VirtualFile resource) {
    VirtualFile moduleRoot = EasyMock.createMock(VirtualFile.class);
    EasyMock.expect(moduleRoot.getPath()).andStubReturn(moduleRootPath);
    EasyMock.expect(moduleRoot.findFileByRelativePath(pathToFile)).andStubReturn(resource);

    EasyMock.replay(moduleRoot);

    return moduleRoot;
  }

  private ModuleRootManager createModuleRootManager(VirtualFile moduleRoot) {
    ModuleRootManager moduleRootManager = EasyMock.createMock(ModuleRootManager.class);
    EasyMock.expect(moduleRootManager.getContentRoots())
        .andStubReturn(new VirtualFile[] {moduleRoot});
    EasyMock.replay(moduleRootManager);

    return moduleRootManager;
  }
}

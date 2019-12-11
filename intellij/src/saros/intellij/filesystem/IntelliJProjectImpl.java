package saros.intellij.filesystem;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saros.filesystem.IContainer;
import saros.filesystem.IFile;
import saros.filesystem.IFolder;
import saros.filesystem.IPath;
import saros.filesystem.IProject;
import saros.filesystem.IResource;
import saros.intellij.project.filesystem.IntelliJPathImpl;

/** A <code>IntelliJProjectImpl</code> represents a specific module loaded in a specific project. */
public final class IntelliJProjectImpl extends IntelliJResourceImpl implements IProject {
  private static final Logger LOG = Logger.getLogger(IntelliJProjectImpl.class);

  private final Project project;

  private final Module module;

  /**
   * Creates a core compatible {@link IProject project} using the given IntelliJ module.
   *
   * <p><b>Note:</b> Only top level modules are fully supported. Modules inside of other modules
   * will be created as top level modules on the receiving side of the session initialization. Inner
   * modules of the shared module will also be transmitted but not registered with IntelliJ as
   * modules.
   *
   * <p><b>Note:</b> Only modules with exactly one content root are currently supported. IProject
   * objects for modules with fewer or more than one content root can not be created.
   *
   * @param module an IntelliJ <i>module</i>
   * @throws IllegalArgumentException if the given module does not have at least one content root or
   *     has multiple content roots with the same name
   */
  public IntelliJProjectImpl(@NotNull final Module module) {
    this.module = module;

    this.project = module.getProject();

    // Still used to enforce module restrictions
    getModuleContentRoot(module);
  }

  /**
   * Returns the content root of the given module.
   *
   * <p>This method is used to enforce the current restriction that shared modules must contain at
   * least one content root.
   *
   * @param module the module to get the content root for
   * @return the content root of the given module
   * @throws IllegalArgumentException if the given module does not have at least one content root or
   *     has multiple content roots with the same name
   */
  @NotNull
  private static List<VirtualFile> getModuleContentRoot(@NotNull Module module) {

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

    VirtualFile[] contentRoots = moduleRootManager.getContentRoots();

    int numberOfContentRoots = contentRoots.length;

    if (numberOfContentRoots == 0) {
      throw new IllegalArgumentException(
          "Modules shared with Saros currently must contain at least one content root. The given module "
              + module
              + " does not have any content roots.");
    }

    Set<String> rootNames = new HashSet<>();

    for (VirtualFile root : contentRoots) {
      String rootName = root.getName();

      if (!rootNames.contains(rootName)) {
        rootNames.add(rootName);

      } else {
        throw new IllegalArgumentException(
            "Modules shared with Saros currently must contain distinctive content roots. The given module "
                + module
                + " has multiple content roots with the name \""
                + rootName
                + "\".");
      }
    }

    return Arrays.asList(contentRoots);
  }

  /**
   * Returns the content root with the given name for the module.
   *
   * @param rootName the name of the content root
   * @return the content root with the given name for the module
   * @throws IllegalArgumentException if no content root with the given name was found
   */
  @NotNull
  private VirtualFile getModuleContentRoot(@NotNull String rootName) {
    List<VirtualFile> contentRoots = getModuleContentRoot(module);

    VirtualFile contentRoot =
        contentRoots
            .stream()
            .filter(root -> root.getName().equals(rootName))
            .findFirst()
            .orElse(null);

    if (contentRoot == null) {
      throw new IllegalArgumentException(
          "Could not find content root " + rootName + " for module " + module);
    }

    return contentRoot;
  }

  /**
   * Returns the content root the given <code>VirtualFile</code> belongs to.
   *
   * @param virtualFile the virtual file to get the content root for
   * @return the content root the given <code>VirtualFile</code> belongs to
   * @throws IllegalArgumentException if the module is not located under any content root of the
   *     module
   */
  @NotNull
  private VirtualFile getModuleContentRoot(@NotNull VirtualFile virtualFile) {
    List<VirtualFile> contentRoots = getModuleContentRoot(module);

    Path filePath = Paths.get(virtualFile.getPath());

    for (VirtualFile contentRoot : contentRoots) {
      Path rootPath = Paths.get(contentRoot.getPath());

      if (filePath.startsWith(rootPath)) {
        return contentRoot;
      }
    }

    throw new IllegalArgumentException(
        "File " + virtualFile + " is not located under any content root of module " + module);
  }

  /**
   * Returns the IntelliJ {@link Module module}.
   *
   * @return the IntelliJ module.
   */
  @NotNull
  public Module getModule() {
    return module;
  }

  /**
   * Returns whether the resource for the given path exists.
   *
   * <p><b>Note:</b> An ignored resource is treated as being nonexistent.
   *
   * @return <code>true</code> if the resource exists and is not ignored, <code>false</code>
   *     otherwise
   * @see #isIgnored()
   */
  @Override
  public boolean exists(final IPath path) {
    final VirtualFile file = findVirtualFile(path);

    return file != null && file.exists();
  }

  @NotNull
  @Override
  public IResource[] members() throws IOException {
    final List<IResource> result = new ArrayList<>();

    for (VirtualFile contentRoot : getModuleContentRoot(module)) {
      IPath contentRootName = IntelliJPathImpl.fromString(contentRoot.getName());

      final VirtualFile[] children = contentRoot.getChildren();

      ModuleFileIndex moduleFileIndex = ModuleRootManager.getInstance(module).getFileIndex();

      for (final VirtualFile child : children) {

        if (!Filesystem.runReadAction(() -> moduleFileIndex.isInContent(child))) {
          continue;
        }

        final IPath childPath = IntelliJPathImpl.fromString(child.getName());

        IPath qualifiedChildPath = contentRootName.append(childPath);

        result.add(
            child.isDirectory()
                ? new IntelliJFolderImpl(this, qualifiedChildPath)
                : new IntelliJFileImpl(this, qualifiedChildPath));
      }
    }

    return result.toArray(new IResource[0]);
  }

  @NotNull
  @Override
  public IResource[] members(final int memberFlags) throws IOException {
    return members();
  }

  @Nullable
  @Override
  public String getDefaultCharset() throws IOException {
    // TODO retrieve encoding for the module or use the project settings
    return null;
  }

  @Override
  public boolean exists() {
    return !module.isDisposed() && module.isLoaded();
  }

  @NotNull
  @Override
  public IPath getFullPath() {
    return IntelliJPathImpl.fromString(getName());
  }

  @NotNull
  @Override
  public String getName() {
    return module.getName();
  }

  @Nullable
  @Override
  public IContainer getParent() {
    return null;
  }

  @NotNull
  @Override
  public IProject getProject() {
    return this;
  }

  @NotNull
  @Override
  public IPath getProjectRelativePath() {
    return IntelliJPathImpl.EMPTY;
  }

  /**
   * Returns the path to the given file relative to the content root of this module.
   *
   * <p><b>Note:</b> This methods expects that the given <code>VirtualFile</code> exists.
   *
   * @param file the <code>VirtualFile</code> to get the relative path for
   * @return a relative path for the given file or <code>null</code> if the file does not belong to
   *     this module or there is no relative path from the content root to the file
   */
  @Nullable
  private IPath getProjectRelativePath(@NotNull VirtualFile file) {
    ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
    Module fileModule = Filesystem.runReadAction(() -> projectFileIndex.getModuleForFile(file));

    if (!module.equals(fileModule)) {
      return null;
    }

    VirtualFile moduleRoot = getModuleContentRoot(file);

    try {
      Path contentRootPath = Paths.get(moduleRoot.getPath()).getParent();
      Path qualifiedRelativePath = contentRootPath.relativize(Paths.get(file.getPath()));

      return IntelliJPathImpl.fromString(qualifiedRelativePath.toString());

    } catch (IllegalArgumentException e) {
      LOG.warn(
          "Could not find a relative path from the content root "
              + moduleRoot
              + " to the file "
              + file,
          e);

      return null;
    }
  }

  @Override
  public int getType() {
    return IResource.PROJECT;
  }

  @Override
  public boolean isIgnored() {
    return false;
  }

  @Override
  public void delete(final int updateFlags) throws IOException {
    throw new IOException("delete is not supported");
  }

  @Override
  public void move(final IPath destination, final boolean force) throws IOException {
    throw new IOException("move is not supported");
  }

  /**
   * Returns the location of the module file. This is done as there is no central location that all
   * resources relate to. Instead, resources are located relative to different content roots.
   *
   * @return the location of the module file
   */
  @NotNull
  @Override
  public IPath getLocation() {
    return IntelliJPathImpl.fromString(module.getModuleFilePath());
  }

  @Nullable
  @Override
  public IResource findMember(final IPath path) {
    final VirtualFile file = findVirtualFile(path);

    if (file == null) return null;

    return file.isDirectory()
        ? new IntelliJFolderImpl(this, path)
        : new IntelliJFileImpl(this, path);
  }

  @NotNull
  @Override
  public IFile getFile(final String name) {
    return getFile(IntelliJPathImpl.fromString(name));
  }

  @NotNull
  @Override
  public IFile getFile(final IPath path) {

    if (path.segmentCount() == 0)
      throw new IllegalArgumentException("cannot create file handle for an empty path");

    return new IntelliJFileImpl(this, path);
  }

  /**
   * Returns an <code>IFile</code> for the given file.
   *
   * @param file the <code>VirtualFile</code> to get the <code>IFile</code> for
   * @return an <code>IFile</code> for the given file or <code>null</code> if the given file is a
   *     directory, does not exist, or the relative path of the file could not be constructed
   */
  @Nullable
  public IFile getFile(@NotNull final VirtualFile file) {
    if (file.isDirectory() || !file.exists()) {
      return null;
    }

    IPath relativePath = getProjectRelativePath(file);

    return relativePath != null ? new IntelliJFileImpl(this, relativePath) : null;
  }

  @NotNull
  @Override
  public IFolder getFolder(final String name) {
    return getFolder(IntelliJPathImpl.fromString(name));
  }

  @NotNull
  @Override
  public IFolder getFolder(final IPath path) {

    if (path.segmentCount() == 0)
      throw new IllegalArgumentException("cannot create folder handle for an empty path");

    return new IntelliJFolderImpl(this, path);
  }

  /**
   * Returns an <code>IFolder</code> for the given file.
   *
   * @param file the <code>VirtualFile</code> to get the <code>IFolder</code> for
   * @return an <code>IFolder</code> for the given file or <code>null</code> if the given file is
   *     not a directory, does not exist, or the relative path of the file could not be constructed
   */
  @Nullable
  public IFolder getFolder(@NotNull final VirtualFile file) {
    if (!file.isDirectory() || !file.exists()) {
      return null;
    }

    IPath relativePath = getProjectRelativePath(file);

    return relativePath != null ? new IntelliJFolderImpl(this, relativePath) : null;
  }

  /**
   * Returns an <code>IResource</code> for the given file.
   *
   * @param file the <code>VirtualFile</code> to get the <code>IResource</code> for
   * @return an <code>IResource</code> for the given file or <code>null</code> if the given file
   *     does not exist or the relative path of the file could not be constructed
   */
  @Nullable
  public IResource getResource(@NotNull VirtualFile file) {
    if (file.isDirectory()) {
      return getFolder(file);
    } else {
      return getFile(file);
    }
  }

  /**
   * Returns the virtual file for the given path belonging to this module.
   *
   * <p><b>Note:</b> This method can not return files for ignored resources or resources belonging
   * to a sub-module.
   *
   * @param path relative path to the file
   * @return the virtual file or <code>null</code> if it does not exists in the VFS snapshot, is
   *     ignored, belongs to a sub-module, or the given path is absolute.
   * @throws IllegalArgumentException if the path does not contain a content root qualifier (has a
   *     name count of 0)
   * @see #isIgnored()
   */
  @Nullable
  public VirtualFile findVirtualFile(IPath path) {

    if (path.isAbsolute()) return null;

    if (path.segmentCount() == 0) {
      throw new IllegalArgumentException("Encountered path without a content root qualifier.");
    }

    String rootName = path.segment(0);

    path = path.removeFirstSegments(1);

    VirtualFile moduleRoot = getModuleContentRoot(rootName);

    if (path.segmentCount() == 0) return moduleRoot;

    VirtualFile virtualFile = moduleRoot.findFileByRelativePath(path.toString());

    if (virtualFile == null) {
      return null;
    }

    ModuleFileIndex moduleFileIndex = ModuleRootManager.getInstance(module).getFileIndex();
    boolean isInContent = Filesystem.runReadAction(() -> moduleFileIndex.isInContent(virtualFile));

    return isInContent ? virtualFile : null;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This method operates under the assumption that module objects are handled as a singleton
   * across the IDE lifecycle, i.e. a module will always be represented by a single, unique <code>
   * Module</code> object.
   *
   * @return a hash code value for this object
   */
  @Override
  public int hashCode() {
    return module.hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This method operates under the assumption that module objects are handled as a singleton
   * across the IDE lifecycle, i.e. a module will always be represented by a single, unique <code>
   * Module</code> object.
   *
   * @return whether the given objects is equal to this object
   */
  @Override
  public boolean equals(final Object obj) {

    if (this == obj) return true;

    if (obj == null) return false;

    if (getClass() != obj.getClass()) return false;

    IntelliJProjectImpl other = (IntelliJProjectImpl) obj;

    return module.equals(other.module);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " : " + project + " - " + module;
  }
}

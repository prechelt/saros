package saros.filesystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.core.runtime.CoreException;

public abstract class EclipseBaseFolderImpl extends EclipseResourceImpl_V2 implements IFolder_V2 {

  public EclipseBaseFolderImpl(org.eclipse.core.resources.IContainer delegate) {
    super(delegate);
  }

  @Override
  public void create(int updateFlags, boolean local) throws IOException {
    throw new NotImplementedException(
        "create() is not implemented in " + this.getClass().getName());
  }

  @Override
  public void create(boolean force, boolean local) throws IOException {
    create(0, local);
  }

  @Override
  public IResource_V2 findMember(IPath path) {
    org.eclipse.core.resources.IResource resource =
        getDelegate().findMember(((EclipsePathImpl) path).getDelegate());

    if (resource == null) return null;

    return new EclipseResourceImpl_V2(resource);
  }

  @Override
  public IFile_V2 getFile(String name) {
    return new EclipseFileImpl_V2(getDelegate().getFile(toIPath(name)));
  }

  @Override
  public IFile_V2 getFile(IPath path) {
    return new EclipseFileImpl_V2(getDelegate().getFile(((EclipsePathImpl) path).getDelegate()));
  }

  @Override
  public IFolder_V2 getFolder(String name) {
    return new EclipseFolderImpl_V2(getDelegate().getFolder(toIPath(name)));
  }

  @Override
  public IFolder_V2 getFolder(IPath path) {
    return new EclipseFolderImpl_V2(
        getDelegate().getFolder(((EclipsePathImpl) path).getDelegate()));
  }

  @Override
  public boolean exists(IPath path) {
    return getDelegate().exists(((EclipsePathImpl) path).getDelegate());
  }

  @Override
  public IResource_V2[] members() throws IOException {
    return members(org.eclipse.core.resources.IResource.NONE);
  }

  @Override
  public IResource_V2[] members(int memberFlags) throws IOException {
    org.eclipse.core.resources.IResource[] resources;

    try {
      resources = getDelegate().members(memberFlags);

      List<IResource> result = new ArrayList<>(resources.length);
      ResourceAdapterFactory.convertTo(Arrays.asList(resources), result);

      return result.toArray(new IResource_V2[0]);
    } catch (CoreException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String getDefaultCharset() throws IOException {
    try {
      return getDelegate().getDefaultCharset();
    } catch (CoreException e) {
      throw new IOException(e);
    }
  }

  /**
   * Returns the original {@link org.eclipse.core.resources.IProject IProject} object.
   *
   * @return
   */
  @Override
  public org.eclipse.core.resources.IContainer getDelegate() {
    return (org.eclipse.core.resources.IContainer) delegate;
  }

  private org.eclipse.core.runtime.IPath toIPath(String toPath) {
    EclipsePathFactory factory = new EclipsePathFactory();
    return ResourceAdapterFactory.convertBack(factory.fromString(toPath));
  }
}

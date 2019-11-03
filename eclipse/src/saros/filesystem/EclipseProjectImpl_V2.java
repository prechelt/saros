package saros.filesystem;

public class EclipseProjectImpl_V2 extends EclipseBaseFolderImpl implements IFolder_V2 {

  public EclipseProjectImpl_V2(org.eclipse.core.resources.IProject delegate) {
    super(delegate);
  }

  @Override
  public org.eclipse.core.resources.IProject getDelegate() {
    return (org.eclipse.core.resources.IProject) delegate;
  }
}

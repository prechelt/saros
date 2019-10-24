package saros.filesystem;

import java.io.IOException;

public interface IResource_V2 {

  int NONE = 0;
  int FILE = 1;
  int FOLDER = 2;
  int ROOT = 8;
  int FORCE = 16;
  int KEEP_HISTORY = 32;

  /**
   * Returns whether this resource exists
   *
   * @return true, if this resource exists
   */
  boolean exists();

  /**
   * Returns the {@link IPath} path of this resource relative to the {@link IWorkspace} workspace
   *
   * @return the path of this resource relative to the workspace
   */
  @Deprecated
  IPath getFullPath();

  /**
   * Returns the {@link String} name of this resource.
   *
   * @return the name of this resource
   */
  String getName();

  /**
   * Returns the resource which is the {@link IFolder_V2} parent of this resource or null if it has
   * no parent
   *
   * @return the parent of this resource or null if it has not parent
   */
  IFolder_V2 getParent();

  /**
   * Returns a {@link IPath} relative path of this resource with respect to its {@link
   * IReferencePoint} reference point.
   *
   * @return the relative path of this resource outgoing of its reference point
   */
  IPath getReferencePointRelativePath();

  /**
   * Returns the {@link Integer} type of the resource
   *
   * @return the type of the resource
   */
  int getType();

  /**
   * Returns true if this resource is marked as derived
   *
   * @param checkAncestors bit-wise or of option flag constants
   * @return true if this resource is marked as derived
   */
  boolean isDerived(boolean checkAncestors);

  /**
   * Returns true if this resource is marked as derived
   *
   * @return true if this resources is marked as derived
   */
  boolean isDerived();

  /**
   * Deletes this resource
   *
   * @param updateFlags bit-wise or of update flag constants
   * @throws IOException if an I/O error occurred
   */
  void delete(int updateFlags) throws IOException;

  /**
   * Moves this resource to the given {@link IPath} relative path.
   *
   * @param destination the target path
   * @param force flag for controlling, if the resources are not synchronized with the located
   *     filesystem, should be tolerated
   * @throws IOException if an I/O error occurred
   */
  void move(IPath destination, boolean force) throws IOException;

  /**
   * Returns the {@link IPath} absolute path of this resource in the local filesystem
   *
   * @return the absolute path of this resource
   */
  IPath getLocation();

  <T extends IResource_V2> T adaptTo(Class<T> clazz);
}

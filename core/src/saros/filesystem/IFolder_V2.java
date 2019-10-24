package saros.filesystem;

import java.io.IOException;

public interface IFolder_V2 {

  /** Equivalent to the Eclipse call <code>IFolder#create(updateFlags, local, null)</code> */
  void create(int updateFlags, boolean local) throws IOException;

  /** Equivalent to the Eclipse call <code>IFolder#create(force, local, null)</code> */
  void create(boolean force, boolean local) throws IOException;

  /**
   * Finds and returns the {@link IResource_V2} resource which is located in the {@link IPath}
   * relative path or null if the resource does not exist
   *
   * @param path relative path from this folder to the resource
   * @return the resource or null if the resource does not exist
   */
  IResource_V2 findMember(IPath path);

  /**
   * Return true if a {@link IResource_V2} exists in the given {@link IPath} relative path
   *
   * @param path relative path to the resource
   * @return true if the resource exists else false
   */
  boolean exists(IPath path);

  /**
   * Returns a list of {@link IResource_V2} resources which are are located in this folder
   *
   * @return a list of resources which are located in this folder
   * @throws IOException
   */
  IResource_V2[] members() throws IOException;

  /**
   * Returns a list of {@link IResource_V2} resources which are are located in this folder
   *
   * @param memberFlags bit-wise or of member flag constants
   * @return a list of resources which are located in this folder
   * @throws IOException if an I/O error occurred
   */
  IResource_V2[] members(int memberFlags) throws IOException;

  /**
   * Returns a handle to the {@link IFile_V2} file in this folder given by the {@link String} name
   *
   * @param name the name of the file
   * @return a handle of the file
   */
  IFile_V2 getFile(String name);

  /**
   * Returns a handle to the {@link IFile_V2} file in this folder given by the {@link IPath}
   * relative path to the file
   *
   * @param path relative path to the file
   * @return a handle of the file
   */
  IFile_V2 getFile(IPath path);

  /**
   * Returns a handle to the {@link IFolder_V2} folder in this folder given by the {@link String}
   * name
   *
   * @param name the name of the folder
   * @return a handle of the folder
   */
  IFolder_V2 getFolder(String name);

  /**
   * Returns a handle to the {@link IFolder_V2} folder in this folder given by the {@link IPath}
   * relative path to the folder
   *
   * @param path relative path to the folder
   * @return a handle of the folder
   */
  IFolder_V2 getFolder(IPath path);

  /**
   * Returns the default charset of this folder
   *
   * @return the default charset of this folder
   * @throws IOException if an I/O error occurred
   */
  String getDefaultCharset() throws IOException;
}

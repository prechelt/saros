package saros.filesystem;

import java.io.IOException;
import java.io.InputStream;

public interface IFile_V2 {

  /**
   * Return the charset of this file
   *
   * @return the charset of this file
   * @throws IOException if an I/O error occurred
   */
  String getCharset() throws IOException;

  /**
   * Returns an open an {@link InputStream} input stream on the contents of this file
   *
   * @return the input stream on the contents of this file
   * @throws IOException if an I/O error occurred
   */
  InputStream getContents() throws IOException;

  /**
   * Sets the contents of this file in the given {@link InputStream} input stream
   *
   * @param input input stream which contains the new content of this file
   * @param force flag for controlling if the resources are not synchronized with the located
   *     filesystem should be tolerated
   * @param keepHistory flag for keeping the current content of this file in the local history
   * @throws IOException if an I/O error occurred
   */
  void setContents(InputStream input, boolean force, boolean keepHistory) throws IOException;

  /**
   * Creates a new {@link IFolder_V2} file resource as a handle of it's parent.
   *
   * @param input input stream with the initial content for this file
   * @param force flag for controlling if the resources are not synchronized with the located *
   *     filesystem should be tolerated
   * @throws IOException if an I/O error occurred
   */
  void create(InputStream input, boolean force) throws IOException;

  /**
   * Returns the size of the file.
   *
   * @return the size of the file in bytes
   * @throws IOException if an I/O error occurred
   */
  long getSize() throws IOException;
}

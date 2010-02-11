/* http://google-ukdev.blogspot.com/2009/01/crimes-against-code-and-using-threads.html
 * Some modification made by barclay to be consistent with formatting.
 */

package net.redgeek.android.eventrend.util;

public interface GUITask {
  void executeNonGuiTask() throws Exception;

  void afterExecute();

  void onFailure(Throwable t);
}

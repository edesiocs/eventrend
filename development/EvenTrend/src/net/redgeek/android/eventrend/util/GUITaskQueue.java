/* http://google-ukdev.blogspot.com/2009/01/crimes-against-code-and-using-threads.html
 * Some modification made by barclay to be consistent with formatting.
 */

package net.redgeek.android.eventrend.util;

import android.os.Handler;
import android.os.Message;

public class GUITaskQueue {
  private static final int HANDLE_EXCEPTION = 0x1337;
  private static final int HANDLE_AFTER_EXECUTE = 0x1338;
  private TaskQueue mTaskQ;
  private Handler mHandler;
  private static GUITaskQueue singleton;

  public static GUITaskQueue getInstance() {
    if (singleton == null) {
      singleton = new GUITaskQueue();
      singleton.start();
    }
    return singleton;
  }

  private GUITaskQueue() {
    mTaskQ = new TaskQueue();
    mHandler = new MyHandler();
  }

  public void start() {
    mTaskQ.start();
  }

  public void stop() {
    mTaskQ.stop();
  }

  public void addTask(GUITask task) {
    mTaskQ.addTask(new GUITaskAdapter(task));
  }

  /**
   * Adds a task with an associated progress indicator. The indicator's
   * showProgressIndicator() get called immediately then the
   * hideProgressIndicator() gets called before the GUITask's handle_exception()
   * or after_execute() method gets called.
   */
  public void addTask(ProgressIndicator progressIndicator, GUITask task) {
    if (progressIndicator == null) {
      addTask(task);
    } else {
      addTask(new GUITaskWithProgress(task, progressIndicator));
    }
  }

  private static class GUITaskWithProgress implements GUITask {
    private GUITask mDelegate;
    private ProgressIndicator mProgressIndicator;

    GUITaskWithProgress(GUITask _delegate, ProgressIndicator _progressIndicator) {
      mDelegate = _delegate;
      mProgressIndicator = _progressIndicator;
      mProgressIndicator.showProgressIndicator();
    }

    public void executeNonGuiTask() throws Exception {
      mDelegate.executeNonGuiTask();
    }

    public void onFailure(Throwable t) {
      mProgressIndicator.hideProgressIndicator();
      mDelegate.onFailure(t);
    }

    public void afterExecute() {
      mProgressIndicator.hideProgressIndicator();
      mDelegate.afterExecute();
    }
  };

  private static class GUITaskWithSomething {
    GUITask mGuiTask;
    Throwable mSomething;

    GUITaskWithSomething(GUITask _guiTask, Throwable _something) {
      mGuiTask = _guiTask;
      mSomething = _something;
    }
  }

  private void postMessage(int what, Object thingToPost) {
    Message msg = new Message();
    msg.obj = thingToPost;
    msg.what = what;
    mHandler.sendMessage(msg);
  }

  private void postException(GUITask task, Throwable t) {
    postMessage(HANDLE_EXCEPTION, new GUITaskWithSomething(task, t));
  }

  private class MyHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case HANDLE_EXCEPTION:
          GUITaskWithSomething thingie = (GUITaskWithSomething) msg.obj;
          thingie.mGuiTask.onFailure(thingie.mSomething);
          break;

        case HANDLE_AFTER_EXECUTE:
          GUITask task = (GUITask) msg.obj;
          try {
            task.afterExecute();
          } catch (Throwable t) {
            // LogX.e(t);
          }
          break;
      }
      super.handleMessage(msg);
    }
  }

  private class GUITaskAdapter implements Runnable {
    private GUITask mTask;

    GUITaskAdapter(GUITask _task) {
      mTask = _task;
    }

    public void run() {
      try {
        mTask.executeNonGuiTask();
        postMessage(HANDLE_AFTER_EXECUTE, mTask);
      } catch (Throwable t) {
        postException(mTask, t);
      }
    }
  }
}
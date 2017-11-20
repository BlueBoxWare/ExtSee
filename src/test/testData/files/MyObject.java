public class Object {
  private static native void registerNatives();

  static {registerNatives();}

  public final native Class<?> getClass();

  public native int hashCode();

  public boolean equals(Object obj) {return false;}

  protected native Object clone() throws CloneNotSupportedException;

  public String toString() {return null;}

  public final native void notify();

  public final native void notifyAll();

  public final native void wait(long timeout) throws InterruptedException;

  public final void wait(long timeout, int nanos) throws InterruptedException {}

  public final void wait() throws InterruptedException {}

  protected void finalize() throws Throwable { }
}
package hbz;

import io.vertx.core.Launcher;

public class MainLauncher extends Launcher {
	
  public static void main(String[] args) {    
    MainLauncher dummyLauncher = new MainLauncher();
    dummyLauncher.dispatch(args);
  }
  
}
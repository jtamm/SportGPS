/**
 * Copyright (C) 2014 android10.org. All rights reserved.
 * @author Fernando Cejas (the android10 coder)
 */
package com.example.androidstudio.sportgps;

import java.util.concurrent.TimeUnit;

/**
 * Class representing a StopWatch for measuring time.
 */
public class StopWatch {
  private long startTime;
  private long endTime;
  private long elapsedTime;
  private Boolean activated;
  public StopWatch() {
    //empty
  }

  private void reset() {
    startTime = 0;
    endTime = 0;
    elapsedTime = 0;
  }

  public void start() {
    reset();
    startTime = System.nanoTime();
    activated = true;
  }

  public void stop() {
    if (startTime != 0) {
      endTime = System.nanoTime();
      elapsedTime = endTime - startTime;
    } else {
      reset();
    }
    activated = false;
  }

  public long getTotalTimeMillis() {
    if(activated){
      return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }
    return (elapsedTime != 0) ? TimeUnit.NANOSECONDS.toMillis(endTime - startTime) : 0;
  }

  public long getTotalTimeSeconds() {
    if(activated){
      return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
    }
    return (elapsedTime != 0) ? TimeUnit.NANOSECONDS.toSeconds(endTime - startTime) : 0;
  }

}
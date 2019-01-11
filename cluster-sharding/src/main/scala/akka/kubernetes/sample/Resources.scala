/*
 * Copyright (C) 2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.kubernetes.sample

class Resources {
  val runtime = Runtime.getRuntime()
  val cores = runtime.availableProcessors()
  val maxMemory = runtime.maxMemory() / 1000000
  override def toString = s"Resources($cores cores, $maxMemory mb)"
}

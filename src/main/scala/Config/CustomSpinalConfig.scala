package Config

import better.files._
import spinal.core.sim.{SimConfig, SpinalSimConfig}
import spinal.core.{FixedFrequency, HertzNumber, IntToBuilder, SpinalConfig}

object CustomSpinalConfig {

  def buildDir = "build/"

  def extractBuildPathFromClassName(classCanonicalName: String): String =
    classCanonicalName.split("\\.").drop(1).mkString("/") + "/"

  def simDirPath(classCanonicalName: String): String =
    (buildDir + extractBuildPathFromClassName(classCanonicalName) + "sim/").toFile
      .createIfNotExists(true)
      .pathAsString + "/"

  def rtlDirPath(classCanonicalName: String): String =
    (buildDir + extractBuildPathFromClassName(classCanonicalName) + "rtl/").toFile
      .createIfNotExists(true)
      .pathAsString + "/"

  def simConfig(
                 classCanonicalName: String,
                 waves: Boolean = true,
                 clockFrequency: HertzNumber = 100 MHz
               ): SpinalSimConfig = {
    val frequency = FixedFrequency(clockFrequency)
    if (waves) {
      SimConfig
        .withConfig(SpinalConfig(defaultClockDomainFrequency = frequency, targetDirectory = "rtl"))
        .withWave
        .workspacePath(simDirPath(classCanonicalName))
        .workspaceName("")
    } else {
      SimConfig
        .withConfig(SpinalConfig(defaultClockDomainFrequency = frequency, targetDirectory = "rtl"))
        .workspacePath(simDirPath(classCanonicalName))
        .workspaceName("")
    }
  }

}

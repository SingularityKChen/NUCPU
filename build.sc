// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import scalalib._
// support BSP
import mill.bsp._
// for pomSettings
import publish._
// input build.sc from each repositories.
import $file.dependencies.chisel3.build
import $file.dependencies.firrtl.build
import $file.dependencies.treadle.build
import $file.dependencies.`chisel-testers2`.build
import $file.dependencies.`api-config-chipsalliance`.`build-rules`.mill.build

// Global Scala Version
object ivys {
  val sv = "2.12.13"
  val upickle = ivy"com.lihaoyi::upickle:1.3.15"
  val oslib = ivy"com.lihaoyi::os-lib:0.7.8"
  val pprint = ivy"com.lihaoyi::pprint:0.6.6"
  val utest = ivy"com.lihaoyi::utest:0.7.10"
  val macroParadise = ivy"org.scalamacros:::paradise:2.1.1"
  val jline = ivy"org.scala-lang.modules:scala-jline:2.12.1"
  val scalatest = ivy"org.scalatest::scalatest:3.2.2"
  val scalatestplus = ivy"org.scalatestplus::scalacheck-1-14:3.1.1.1"
  val scalacheck = ivy"org.scalacheck::scalacheck:1.14.3"
  val scopt = ivy"com.github.scopt::scopt:3.7.1"
  val playjson =ivy"com.typesafe.play::play-json:2.6.10"
  val spire = ivy"org.typelevel::spire:0.16.2"
  val breeze = ivy"org.scalanlp::breeze:1.1"
}

object helper extends Module {
  def seed = T(s"${os.pwd.baseName}_${System.getProperty("user.name")}")
  def sync(server: String) = T.command {
    os.proc("rsync", "-avP", "--delete" , "--exclude=.git/", "--exclude=out/", s"${os.pwd.toString()}/", s"$server:/tmp/${seed()}").call()
  }
}

// For modules not support mill yet, need to have a ScalaModule depend on our own repositories.
trait CommonModule extends ScalaModule {
  override def scalaVersion = ivys.sv

  override def scalacPluginClasspath = super.scalacPluginClasspath() ++ Agg(
    mychisel3.plugin.jar()
  )

  override def moduleDeps: Seq[ScalaModule] = Seq(mychisel3)

  override def compileIvyDeps = Agg(ivys.macroParadise)

  override def scalacPluginIvyDeps = Agg(ivys.macroParadise)
}


// Chips Alliance

object myfirrtl extends dependencies.firrtl.build.firrtlCrossModule(ivys.sv) {
  override def millSourcePath = os.pwd / "dependencies" / "firrtl"
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivys.pprint
  )
}

object mychisel3 extends dependencies.chisel3.build.chisel3CrossModule(ivys.sv) {
  override def millSourcePath = os.pwd / "dependencies" / "chisel3"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)

  def treadleModule: Option[PublishModule] = Some(mytreadle)
}

object mytreadle extends dependencies.treadle.build.treadleCrossModule(ivys.sv) {
  override def millSourcePath = os.pwd /  "dependencies" / "treadle"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
}

object myconfig extends dependencies.`api-config-chipsalliance`.`build-rules`.mill.build.config with PublishModule {
  override def millSourcePath = os.pwd /  "dependencies" / "api-config-chipsalliance" / "design" / "craft"

  override def scalaVersion = ivys.sv

  override def pomSettings = PomSettings(
      description = artifactName(),
      organization = "edu.berkeley.cs",
      url = "https://github.com/chipsalliance/rocket-chip",
      licenses = Seq(License.`Apache-2.0`, License.`BSD-3-Clause`),
      versionControl = VersionControl.github("chipsalliance", "rocket-chip"),
      developers = Seq.empty
    )

  override def publishVersion = "1.2-SNAPSHOT"
}

// UCB

object mychiseltest extends dependencies.`chisel-testers2`.build.chiseltestCrossModule(ivys.sv) {
  override def millSourcePath = os.pwd /  "dependencies" / "chisel-testers2"

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

  def treadleModule: Option[PublishModule] = Some(mytreadle)
}

object playground extends CommonModule {

  // add some scala ivy module you like here.
  override def ivyDeps = Agg(
    ivys.oslib,
    ivys.pprint
  )

  // use scalatest as your test framework
  object tests extends Tests with TestModule.ScalaTest {
    override def ivyDeps = Agg(
      ivys.scalatest
    )
    override def moduleDeps = super.moduleDeps ++ Seq(mychiseltest)
  }
}

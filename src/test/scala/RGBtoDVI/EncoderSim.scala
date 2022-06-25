package RGBtoDVI

import Config.CustomSpinalConfig
import spinal.core.{Bool, ClockDomain, LiteralBuilder}
import org.scalatest._
import spinal.core.{Bool, ClockDomain}
import spinal.core.sim._

import scala.collection.mutable.ListBuffer
import scala.util.Random

class EncoderSim extends FunSuite {
  var compiled = CustomSpinalConfig.simConfig(this.getClass.getCanonicalName).compile(new Encoder())

  def dutInitialization(encoder: Encoder): Unit ={
    encoder.clockDomain.forkStimulus(10)

    encoder.io.data_DI.payload #= 0
    encoder.io.data_DI.valid #= false
    encoder.io.dataEnable_SI #= false

    encoder.io.control0_SI #= false
    encoder.io.control1_SI #= false
  }

  def LongToBitvector(data: Long): ListBuffer[String] ={
    val res = new ListBuffer[String]
    var tmp = data

    for (i <- 0 until 10){
      if (tmp % 2 == 0)
        res.insert(i, "0")
      else
        res.insert(i, "1")

      tmp /= 2
    }
    res
  }

  def not(bit: String): String ={
    if (bit == "1")
      "0"
    else
      "1"
  }

  def xor(left: String, right: String): String ={
    (left.toInt ^ right.toInt).toString
  }

  def xnor(left: String, right: String): String ={
    not(xor(left, right))
  }

  def decode(d: Long): BigInt ={
    val bits = LongToBitvector(d)

    val bitsNew = new ListBuffer[String]
    val res = new ListBuffer[String]

    for (i <- 0 until 8){
      if (bits(9) == "1") {
        bitsNew.insert(i, not(bits(i)))
      }
      else
        bitsNew.insert(i, bits(i))
    }

    res.insert(0, bitsNew.head)

    for (i <- 1 until 8){
      if(bits(8) == "1"){
        res.insert(i, xor(bitsNew(i), bitsNew(i-1)))
      }
      else{
        res.insert(i, xnor(bitsNew(i), bitsNew(i-1)))
      }
    }

    BigInt(res.reverse.mkString, 2)
  }

  test("EncodeAllVariations") {
    compiled.doSim(name = "EncodeAllVariations") { dut =>
      SimTimeout(10000000)
      dutInitialization(dut)

      dut.clockDomain.waitSampling()

      for (i <- 0 until 256){
        dut.io.data_DI.payload #= i
        dut.io.data_DI.valid #= true
        dut.io.dataEnable_SI #= true

        dut.clockDomain.waitFallingEdge()

        assert(decode(dut.io.data_DO.toLong) == i)

        dut.clockDomain.waitRisingEdge()
      }

    }
  }

  test("ControlSignals") {
    compiled.doSim(name = "ControlSignals") { dut =>
      SimTimeout(10000000)
      dutInitialization(dut)

      val controlEncoding: Map[Int, BigInt] = Map(  0 -> BigInt("1101010100", 2),
                                                    1 -> BigInt("0010101011", 2),
                                                    2 -> BigInt("0101010100", 2),
                                                    3 -> BigInt("1010101011", 2))

      dut.clockDomain.waitSampling()

      for (i <- 0 until 4){
        val crtl0 = if (i % 2 == 0) false else true
        val crtl1 = if (i < 2) false else true

        dut.io.control0_SI #= crtl0
        dut.io.control1_SI #= crtl1
        dut.io.data_DI.valid #= true
        dut.io.dataEnable_SI #= false

        dut.clockDomain.waitFallingEdge()

        assert(controlEncoding(i) == dut.io.data_DO.toBigInt)

        dut.clockDomain.waitRisingEdge()
      }

    }
  }
}

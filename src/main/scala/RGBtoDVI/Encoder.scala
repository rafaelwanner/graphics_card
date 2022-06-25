package RGBtoDVI

import spinal.core._
import spinal.lib._

/**
 * SpinalHDL component that implements the 8b/10b encoding used in the DVI protocol
 *
 *
 * @see  [[https://en.wikipedia.org/wiki/8b/10b_encoding]]
 */
class Encoder extends Component {

  val io = new Bundle {
    val dataEnable_SI = in Bool()
    val control0_SI = in Bool()
    val control1_SI = in Bool()

    val data_DI = slave Flow Bits(8 bits)

    val data_DO = out Bits(10 bits)
  }

  /** Tracks the running disparity */
  val cnt = RegInit(S"11111")

  val d1 = CountOne(io.data_DI.payload) > 4 | (CountOne(io.data_DI.payload) === 4 & !io.data_DI.payload(0))

  val q_m = Vec(Bool, 9)
  q_m(0) := io.data_DI.payload(0)
  when(d1){
    for (i <- 1 until q_m.length - 1)
      q_m(i) := ~(q_m(i-1) ^ io.data_DI.payload(i))
    q_m(8) := False
  }.otherwise{
    for (i <- 1 until q_m.length - 1)
      q_m(i) := q_m(i-1) ^ io.data_DI.payload(i)
    q_m(8) := True
  }

  val q_mLSB = q_m.asBits(7 downto 0)

  val d2 = (cnt === 0) | (CountOne(q_mLSB) === CountOne(~q_mLSB))

  val d3 = ((cnt > 0) & (CountOne(q_mLSB) > CountOne(~q_mLSB))) | ((cnt < 0) & (CountOne(q_mLSB) < CountOne(~q_mLSB)))

  val videoCode = Bits(10 bits)
  when(d2){
    videoCode(9) := ~q_m(8)
    videoCode(8) := q_m(8)
    videoCode(7 downto 0) := Mux(q_m(8), q_mLSB, ~q_mLSB)
  }.otherwise{
    when(d3){
      videoCode(9) := True
      videoCode(8) := q_m(8)
      videoCode(7 downto 0) := ~q_mLSB
    }.otherwise{
      videoCode(9) := False
      videoCode(8) := q_m(8)
      videoCode(7 downto 0) := q_mLSB
    }
  }

  val controlCode = Bits(10 bits)
  when(io.control1_SI ## io.control0_SI === B"00") {
    controlCode := B"1101010100"
  }.elsewhen(io.control1_SI ## io.control0_SI === B"01") {
    controlCode := B"0010101011"
  }.elsewhen(io.control1_SI ## io.control0_SI === B"10"){
    controlCode := B"0101010100"
  }.elsewhen(io.control1_SI ## io.control0_SI === B"11"){
     controlCode := B"1010101011"
  }.otherwise{
    controlCode := 0
  }

  val acc = SInt(5 bits)
  when(d2 & q_m(8)){
    acc := (CountOne(~q_mLSB).intoSInt - CountOne(q_mLSB).intoSInt).resized
  }.elsewhen(d2 & ~q_m(8)){
    acc := (CountOne(q_mLSB).intoSInt - CountOne(~q_mLSB).intoSInt).resized
  }.elsewhen(~d2 & d3){
    acc := (2 * (B"0" ## q_m(8)).asSInt + (CountOne(~q_mLSB).intoSInt - CountOne(q_mLSB).intoSInt)).resized
  }.elsewhen(~d2 & ~d3){
    acc := (-2 * (B"0" ## q_m(8)).asSInt + (CountOne(q_mLSB).intoSInt) - CountOne(~q_mLSB).intoSInt).resized
  }.otherwise{
    acc := 0
  }

  when(io.dataEnable_SI & io.data_DI.valid){
    cnt := cnt + acc
    io.data_DO := videoCode
  }.elsewhen(~io.dataEnable_SI & io.data_DI.valid){
    cnt := 0
    io.data_DO := controlCode
  }.otherwise{
    io.data_DO := 0
  }




}

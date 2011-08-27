package yuesong.util
import java.text.SimpleDateFormat

object Helper extends Helper

trait Helper {

  val dateFormat = new SimpleDateFormat("HH:mm:ss,SSS")

  def now = dateFormat.format(compat.Platform.currentTime)
  
  def info(msg: Any) { println("%s [%s] %s - %s".format(now, currentThread.getName, getClass.getSimpleName, msg)) }

}
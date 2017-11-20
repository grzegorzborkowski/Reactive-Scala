package productcatalog.Util

object Util {

  def occurences(pattern: String, string: String): Int = {
    val split = pattern.split(" ")
    split.map(splited =>
      if (splited.contains(string)) 1 else 0).sum
  }

}

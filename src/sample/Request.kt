package sample

class Request(
        private val id: Int, // time remained to complied
              var time: Double, // вермя создания заявки
              val timeOfCreation: Double)
{
    internal var timeOfBeginning :Double = 0.0  // время, когда заявка поступила в обработку каналами = 0.0
    internal var timeOfEnding : Double = 0.0 // время, когда заявка вышла из обработки каналами = 0.0

    /**
     *
     * @return true if time>0, false otherwise
     */
    fun reduceBy(i: Double): Boolean {
        time -= i
        return time > 0
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is Request) return false
        return obj.id == id
    }

    override fun hashCode(): Int {
        return id
    }

}
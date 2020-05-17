package sample

import java.util.*

class QueuingSystem(requestsQueueMax: Int, channelsNam: Int, accuracy: Double) {

    internal var listOfCompletedRequests : LinkedList<Request> = LinkedList()
    private var listOfRejectedRequests : LinkedList<Request> = LinkedList()
    private val channelsNam: Int
    private val requestsQueueMax: Int
    private val accuracy: Double
    var createdRequests = 0
        private set
    var handler: SystemHandler? = null

    /**
     *
     * @param lambda - average number of applications
     * @param mu - average service per application
     * @param totalTime - program run time
     * @throws Exception - if requestsQueueMax<=0 or channelsNam<=0 or totalTime<=0 or lambda<=0
     */
    @Throws(Exception::class)
    fun main(lambda: Double, mu: Double, totalTime: Int): IntArray {
        if (totalTime <= 0 || lambda <= 0 || mu <= 0) {
            throw Exception("lambda, mu and totalTime cannot be <=0")
        }
        listOfCompletedRequests = LinkedList()
        listOfRejectedRequests = LinkedList()
        handler = SystemHandler(channelsNam, accuracy)
        // Очередь Заявок
        var requestQueue = LinkedList<Request>()
        val dimensions = Math.ceil(totalTime / accuracy).toInt()
        val vector = IntArray(dimensions + 1) // вектор времени на состояние totalTime + t=0
        Arrays.fill(vector, 0)
        var timeBetweenRequests = getRandomTime(lambda) // получаем время до первой заявки
        createdRequests = 0 // количесвто созданых заявок
        for (time in 0..dimensions) {
            var rejected = false // будет ли отвергнута заявка
            requestQueue = handler!!.add(requestQueue, time * accuracy) // добавляем заявки из очереди

            // время новой заявки,
            while (timeBetweenRequests < accuracy) {
                val newRequest = Request(++createdRequests, getRandomTime(mu),time * accuracy)
                // добавляем новыую заявку в систему, если заявка будет добавлена, ей припишут время добавления
                val add = handler!!.add(newRequest, time * accuracy)

                // если заявку не добавли в систему добавляем в очередь, если очередь не заполнена
                if (!add) {
                    if (requestQueue.size < requestsQueueMax) {
                        requestQueue.add(newRequest)
                    } else {
                        listOfRejectedRequests.add(newRequest)
                        rejected = true // записываем, что был отказ
                    }
                }
                timeBetweenRequests = getRandomTime(lambda) // получаем новое время до сл заявки
            }

            // после добавления заявок, узнаем их количесво в системе
            // и приписываем состояние в вектор времени/состояний
            // Состояние = Количесво заявок в системе + количесво заявок в очереди
            if (rejected) {
                // если заявка была отврегнута
                // Состояние системы = общее число каналов + максимальное число заявок в очереди + 1
                vector[time] = channelsNam + requestsQueueMax + 1
            } else {
                // Состояние когда заявок вообще нет здесь тоже учитывается.
                vector[time] = handler!!.requestsNumber + requestQueue.size
            }
            val listOfCompletedRequestsOnThisIter = handler!!.handle() // Вычитаем время из заявок
            for (request in listOfCompletedRequestsOnThisIter) {
                request.timeOfEnding = time * accuracy // Устанавливаем время окончания заявки
            }

            // Добавляем заявки в список завершенных
            listOfCompletedRequests.addAll(listOfCompletedRequestsOnThisIter)
            timeBetweenRequests -= accuracy // уменьшаем время до сл заявки
        } // end of main for

        // ставим время заявкам, которые не успели закончится
        val listOfUncompletedRequests = handler!!.requests
        for (request in listOfUncompletedRequests) {
            request.timeOfEnding = totalTime.toDouble() + request.time
        }

        // ставим время заявкам в очереди
        for (request in requestQueue) {
            request.timeOfBeginning = totalTime.toDouble() + request.time
            request.timeOfEnding = totalTime.toDouble() + request.time
        }
        listOfCompletedRequests.addAll(listOfUncompletedRequests)
        listOfCompletedRequests.addAll(requestQueue)
        return vector
    }

//   private lateinit var listOfCompletedRequests: LinkedList<Request>

    companion object {
//        var listOfCompletedRequests = LinkedList<Request>() // набор выполненых заявок
        fun getRandomTime(lambda: Double): Double {
            return Math.log(1 - Math.random()) / -lambda
        }
    }

    /**
     * Конструктор Системы массового обслуживания
     * @param requestsQueueMax - Max number of requests in queue
     * @param channelsNam - number of channel
     */
    init {
        if (requestsQueueMax <= 0 || channelsNam <= 0 || accuracy <= 0) {
            throw Exception("requestsQueueMax, channelsNam and accuracy cannot be <=0")
        }
        this.channelsNam = channelsNam
        this.requestsQueueMax = requestsQueueMax
        this.accuracy = accuracy
    }
}
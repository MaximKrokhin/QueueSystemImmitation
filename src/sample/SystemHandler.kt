package sample

import java.util.*

/**
 * Класс который занимается распределением заявок по каналам
 */
class SystemHandler internal constructor(channelsNam: Int, accuracy: Double) {
    // Набор свободных каналов
    private val freeChannels = LinkedList<Int>()

    // Мапа каналов, и заявок, которые они обрабатывают
    private val channelMap: MutableMap<Int, Request?> = HashMap()

    // Мапа обрабатваемых заявок со списком каналов, которые их обрабатывают
    private val requestMap: MutableMap<Request, LinkedList<Int>> = HashMap()
    private var changes // переменная, для слежения были ли добавлены или удалены заявки
            : Boolean
    private val accuracy: Double

    /**
     *
     * @return число заявок в системе.
     */
    val requestsNumber: Int
        get() = requestMap.size

    val requests: LinkedList<Request>
        get() = LinkedList(requestMap.keys)

    /**
     * Добавление заявок в систему
     * @param requests - заявки
     * @return возвращает заявки, которые не получилось добавить
     */
    fun add(requests: LinkedList<Request>, time: Double?): LinkedList<Request> {
        if (requests.size == 0) { // проверка на пустой список
            return requests
        }
        for (i in requests.indices) {
            if (requestMap.size == channelMap.size) {
                break
            }
            val request = requests.poll()
            request.timeOfBeginning = time!!
            requestMap[request] = LinkedList()
        }
        changes = true // были добавлены заявки
        return requests
    }

    /**
     * Добавление заявки в систему
     * @param request - заявка
     * @return возвращает заявки, которые не получилось добавить
     */
    fun add(request: Request?, time: Double?): Boolean {
        if (request == null || requestMap.size >= channelMap.size) {
            return false
        }
        request.timeOfBeginning = time!!
        requestMap[request] = LinkedList()
        changes = true // была добавлена заявка
        return true
    }

    /**
     * уменьшаем время у всех заявок в системе,
     * если время заявки <=0 добавляем в список исполненых и освобждаем каналы
     */
    fun handle(): LinkedList<Request> {
        val listOfCompletedRequests = LinkedList<Request>()
        if (requestMap.size == 0) { // если список заявок пуст, делать нечего
            return listOfCompletedRequests
        }
        if (changes) { // если были добавлены или удалены заявки, переопределяем каналы
            // очищаем список заявок
            for (entry in requestMap.entries) {
                entry.setValue(LinkedList())
            }
            var channelId = 1
            var wasAdd = true
            while (channelId <= channelMap.size && wasAdd) {
                wasAdd = false // проверка, если могут быть свободные каналы.
                for ((key, value) in requestMap) {
                    if (channelId > channelMap.size) {
                        break
                    }
                    // канал добавляется, если время выполнения заявки < каналов
                    if (key.time > accuracy * value.size) {
                        value.add(channelId)
                        channelMap.replace(channelId, key)
                        channelId++
                        wasAdd = true
                    }
                }
            }
            changes = false // изменения обработаны
        }

        // цикл обработки заявок
        for ((key, value) in requestMap) {

            // уменьшаем время
            key.reduceBy(accuracy * value.size)

            // проверяем время
            if (key.time < accuracy) {
                // добавляем в список выполненых
                listOfCompletedRequests.add(key)
            }
        }

        // Удаляем все выполненые заявки из системы
        for (request in listOfCompletedRequests) {
            // Освобождаем каналы
            for ((key, value) in channelMap) {
                if (value === request) {
                    channelMap.replace(key, null)
                    freeChannels.add(key)
                }
            }
            // Удаляем заявку
            requestMap.remove(request)
            changes = true // были удалены заявки
        }
        return listOfCompletedRequests
    }

    /**
     * Конструктор системы
     * @param channelsNam - количесво каналов
     * @param accuracy - точность
     */
    init {
        // В начале все каналы свободные
        for (i in 1..channelsNam) {
            freeChannels.add(i)
            channelMap[i] = null
        }
        this.accuracy = accuracy
        changes = false // в начале изменений нет
    }
}
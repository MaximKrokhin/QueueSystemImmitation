package sample

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.chart.XYChart.Series
import javafx.stage.Stage
import java.util.*

class Main : Application() {
    override fun start(stage: Stage) {
        stage.title = "Графики"
        //defining the axes
        val xAxis = NumberAxis()
        val yAxis = NumberAxis()
        xAxis.label = "Время"
        yAxis.label = "Вероятность"
        //creating the chart
        val lineChart = LineChart(xAxis, yAxis)
        lineChart.title = "График состояний системы"
        lineChart.createSymbols = false
        // опредляем количесво графиков
        val conditions = ArrayList<Series<Number, Number>>()
        for (i in 0..channelsNum + requestsQueueMax) {
            val condition = Series<Number, Number>()
            if (i < channelsNum + requestsQueueMax + 1) {
                condition.name = "Состояние $i"
            }
            conditions.add(condition)
        }

        // заполняем графики информацией
        for (time in matrix.indices) {
            //populating the series with data
            for (con in 0..channelsNum + requestsQueueMax) {
                conditions[con].data.add(XYChart.Data(time * accuracy, matrix[time][con]))
            }
        }

        // добавляем графики на сцену
        val scene = Scene(lineChart, 640.0, 480.0)
        for (con in 0..channelsNum + requestsQueueMax) {
            lineChart.data.add(conditions[con])
        }
        stage.scene = scene
        stage.show()
    }

    companion object {
        private lateinit var matrix: Array<DoubleArray>
        private var requestsQueueMax: Int = 0
        private var channelsNum: Int = 0
        private var accuracy: Double = 0.0

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            // p = lambda/channelsNum*mu
            // lambda - среднее число заявок
            // mu - среднее время обслуживания одной заявки одним каналом!!!
            requestsQueueMax = 6 // максимальный объем очереди
            channelsNum = 5 // число каналов
            val totalTime = 50 // максимальное время
            accuracy = 0.01 // шаг
            val dimensions = Math.ceil(totalTime / accuracy).toInt() + 1
            // матрица времени/состояний totalTime + t=0; requestsQueueMax + channelsNam + rejected + 0
            matrix = Array(dimensions) { DoubleArray(requestsQueueMax + channelsNum + 2) }
            for (vec in matrix) { // обнуляем все значения матрицы
                Arrays.fill(vec, 0.0)
            }
            val queuingSystem = QueuingSystem(requestsQueueMax, channelsNum, accuracy)
            val NumberOfIterations = 1000 // число итераций
            // список завершенных заявок
            val listOfCompletedRequests = LinkedList<Request>()
            //  число созданных заявок
            var createdRequests = 0
            // среднее число Запросов в очереди
            var numberOfRequestsInQueue = 0.0
            // число отказов
            var numberOfRejections = 0.0
            // Число заявок в системе (обслуживается каналми)
            var numberOfRequestsInSystem = 0.0
            // Сколько раз в системе была очердь
            var numberOfQueues = 0
            for (i in 1..NumberOfIterations) {
                // вектор состояний на каждой итерации
                val vector = queuingSystem.main(0.5, 0.15, totalTime)
                // добавляем в список все заявки, которые завершились за итерацию
                listOfCompletedRequests.addAll(queuingSystem.listOfCompletedRequests)
                // увеличиваем количесвто созданных заявок за итерацию
                createdRequests += queuingSystem.createdRequests

                // проходемся по всему времени в итерации
                for (time in vector.indices) {
                    // в каждое время увеличиваем количесво произошедших состояний на 1;
                    //matrix[time][vector[time]] += 1
                    matrix[time].set(vector[time], matrix[time][vector[time]]+1)
                    // если очередь не пустая, записыаем сколько элементов в очереди
                    if (vector[time] > channelsNum) {
                        // число заявок обслуживаемых каналами = числу каналов
                        numberOfRequestsInSystem += channelsNum.toDouble()
                        // увеличиваем количесвто раз, когда была очередь
                        numberOfQueues++
                        if (vector[time] == channelsNum + requestsQueueMax + 1) {
                            // если заявка была опровергнута -> очередь полна
                            numberOfRequestsInQueue += requestsQueueMax.toDouble()
                            // увеличиваем количество раз отказаа заявки
                            numberOfRejections++
                        } else {
                            numberOfRequestsInQueue += vector[time] - channelsNum.toDouble()
                        }
                    } else {
                        // увеличиваем число заявок на каналах
                        numberOfRequestsInSystem += vector[time]
                    }
                }
            }

            // Частоты состояний делим на количесво итераций
            for (time in matrix.indices) {
                for (condition in 0 until matrix[time].size) {
                    matrix[time].set(condition, matrix[time][condition]/NumberOfIterations)
                }
            }
            println("Сумма всех вероятностей: " + Arrays.stream(matrix[dimensions - 1]).sum())
            Arrays.stream(matrix[dimensions - 1]).limit(channelsNum + requestsQueueMax + 1.toLong()).forEach { x: Double -> println(x) }
            var timeInQueue = 0.0 // время ожидания в очереди
            var timeInQueuingSystem = 0.0 // время пребывания заявки в СМО

            // Вычисляем время ожидания в очереди и в СМО для всех выполненых заявок
            for (request in listOfCompletedRequests) {
                timeInQueue += request.timeOfBeginning - request.timeOfCreation
                timeInQueuingSystem += request.timeOfEnding - request.timeOfCreation
            }

            // Вычисляем средние от времени ожидания в очереди и в СМО
            timeInQueue /= listOfCompletedRequests.size.toDouble()
            timeInQueuingSystem /= listOfCompletedRequests.size.toDouble()

            println("Интенсивность потока заявок: " +
                    createdRequests.toDouble() / (timeInQueuingSystem * NumberOfIterations * totalTime))
            println("Среднее число заявок в очереди: " +
                    numberOfRequestsInQueue / (NumberOfIterations * totalTime / accuracy))
            println("Среднее время ожидания в очереди: $timeInQueue")
            println("Среднее число заявок в системе: " +
                    (numberOfRequestsInSystem + numberOfRequestsInQueue) / (NumberOfIterations * totalTime / accuracy))
            println("Среднее время пребывания заявки в системе: $timeInQueuingSystem")
            println("Вероятность отказа: " +
                    numberOfRejections / (NumberOfIterations * totalTime / accuracy))
            println("Вероятность отсусвия очереди: " +
                    (NumberOfIterations * totalTime / accuracy - numberOfQueues.toDouble()) /
                    (NumberOfIterations * totalTime / accuracy))
            // отношение среднего числа обслуженных заявок к среднему числу поступивших заявок за единицу времени
            println("Относительная пропускная способность: " +
                    listOfCompletedRequests.size.toDouble() / createdRequests)
            println("Абсолютная пропускная способность: " +
                    listOfCompletedRequests.size.toDouble() / (NumberOfIterations * totalTime))

            launch(Main::class.java,*args) // выводим графики
        }
    }
}





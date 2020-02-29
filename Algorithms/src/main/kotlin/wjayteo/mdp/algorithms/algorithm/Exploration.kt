package wjayteo.mdp.algorithms.algorithm

import kotlinx.coroutines.*
import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.arena.Sensor
import wjayteo.mdp.algorithms.arena.MapDescriptor
import wjayteo.mdp.algorithms.arena.Robot
import wjayteo.mdp.algorithms.wifi.WifiMessageListener
import wjayteo.mdp.algorithms.wifi.WifiSocketController
import java.lang.NumberFormatException
import java.util.concurrent.atomic.AtomicInteger

class Exploration : Algorithm() {
    private var started: Boolean = false
    private var wallHug: Boolean = true
    private var stop: Boolean = false
    private var previousCommand: Int = COMMAND_FORWARD
    private var lastStartedThread: Thread? = null
    private val stepReference: AtomicInteger = AtomicInteger(0)

    override fun messageReceived(message: String) {
        if (message.contains("#")) {
            CoroutineScope(Dispatchers.Default).launch {
                val sensorReadings: List<String> = message.split("#")
                if (sensorReadings.size != 7) return@launch

                try {
                    val step: Int = sensorReadings[0].toInt()
                    val currentStep = stepReference.incrementAndGet()

                    if (step > 0) {
                        if (step != currentStep) return@launch
                        Robot.moveTemp()
                    }
                } catch (e: NumberFormatException) { return@launch }

                val facing: Int = Robot.facing
                val x: Int = Robot.position.x
                val y: Int = Robot.position.y
                try { Sensor.updateArenaSensor1(x, y, facing, sensorReadings[1].toInt()) } catch (e: NumberFormatException) {}
                try { Sensor.updateArenaSensor2(x, y, facing, sensorReadings[2].toInt()) } catch (e: NumberFormatException) {}
                try { Sensor.updateArenaSensor3(x, y, facing, sensorReadings[3].toInt()) } catch (e: NumberFormatException) {}
                try { Sensor.updateArenaSensor4(x, y, facing, sensorReadings[4].toInt()) } catch (e: NumberFormatException) {}
                try { Sensor.updateArenaSensor5(x, y, facing, sensorReadings[5].toInt()) } catch (e: NumberFormatException) {}
                try { Sensor.updateArenaSensor6(x, y, facing, sensorReadings[6].toInt()) } catch (e: NumberFormatException) {}
            }

            return
        }

        when (message) {
            "C" -> lastStartedThread?.join()
            "S" -> sendArena()

            "M" -> {
                lastStartedThread?.join()
                step()
                Thread.sleep(10)
                sendArena()
            }

            "L" -> {
                if (!started) Robot.turn(-90)
                lastStartedThread?.join()
                step()
                Thread.sleep(10)
                WifiSocketController.writeSynchronous("D", "#robotPosition:${Robot.position.x}, ${Robot.position.y}, ${Robot.facing}")
            }

            "R" -> {
                if (!started) Robot.turn(90)
                lastStartedThread?.join()
                step()
                Thread.sleep(10)
                WifiSocketController.writeSynchronous("D", "#robotPosition:${Robot.position.x}, ${Robot.position.y}, ${Robot.facing}")
            }

            else -> return
        }

        if (stop) started = false
    }

    fun init() {
        WifiSocketController.setListener(this)
        Arena.reset()
        Robot.reset()
    }

    fun start() {
        init()
        stepReference.set(0)
        wallHug = true
        started = true
        stop = false
        if (ACTUAL_RUN) step()
        else simulate()
    }

    fun stop() {
        stop = true
        WifiSocketController.write("A", "B")
    }

    fun testSensorReadings() {
        WifiSocketController.write("A", "I")
    }

    private fun step() {
        if (stop) return

        if (wallHug) {
            stepReference.set(0)

            if (!Robot.isLeftObstructed() && previousCommand != COMMAND_LEFT) {
                previousCommand = COMMAND_LEFT
                WifiSocketController.write("A", "L")
                Robot.turn(-90)
                return
            }

            if (!Robot.isFrontObstructed()) {
                previousCommand = COMMAND_FORWARD
                WifiSocketController.write("A", "N")
                return
            }

            previousCommand = COMMAND_RIGHT
            WifiSocketController.write("A", "R")
            Robot.turn(90)
        }
    }

    private fun simulate() {
        CoroutineScope(Dispatchers.Default).launch {
            while (!stop) {
                delay(100)

                if (wallHug) {
                    if (!Robot.isLeftObstructed() && previousCommand != COMMAND_LEFT) {
                        previousCommand = COMMAND_LEFT
                        Robot.turn(-90)
                        continue
                    }

                    if (!Robot.isFrontObstructed()) {
                        previousCommand = COMMAND_FORWARD
                        Robot.moveTemp()
                        continue
                    }

                    previousCommand = COMMAND_RIGHT
                    Robot.turn(90)
                    continue
                }
            }
        }

    }

    private fun sendArena() {
        val descriptor: List<String> = MapDescriptor.fromArray(Arena.exploreArray, Arena.obstacleArray, 1)
        WifiSocketController.writeSynchronous("D", "#r:${descriptor[0]},${descriptor[1]},${Robot.position.x},${Robot.position.y},${Robot.facing}")
    }
}
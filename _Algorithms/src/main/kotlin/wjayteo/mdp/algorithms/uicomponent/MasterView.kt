package wjayteo.mdp.algorithms.uicomponent

import wjayteo.mdp.algorithms.algorithm.Exploration
import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.arena.Robot
import javafx.scene.Parent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import tornadofx.*
import wjayteo.mdp.algorithms.algorithm.Algorithm
import wjayteo.mdp.algorithms.algorithm.FastestPath
import wjayteo.mdp.algorithms.file.File
import wjayteo.mdp.algorithms.wifi.IdleListener
import java.lang.NumberFormatException

class MasterView : View("Algorithms Test") {
    companion object {
        lateinit var idleListener: IdleListener
        lateinit var exploration: Exploration
        lateinit var fastestPath: FastestPath
    }

    private val menuBar: MenuBar by inject()
    private val arenaMapView: ArenaMapView by inject()
    private val controlsView: ControlsView by inject()
    private var rootBox: VBox by singleAssign()

    override val root: Parent = borderpane {
        style = "-fx-font-family: 'Verdana';"
        top { add(menuBar) }

        center {
            rootBox = vbox {
                style = "-fx-background-color: #FFFFFF;"

                add(controlsView)
                add(arenaMapView)
                onLeftClick { requestFocus() }
                onRightClick { requestFocus() }
            }

            borderpaneConstraints {
                marginTop = 1.0
            }
        }
    }

    init {
        currentStage?.isResizable = false
        currentStage?.sizeToScene()

        runLater {
            Arena.setAttachedView(arenaMapView)
            Robot.setAttachedView(arenaMapView)
            idleListener = IdleListener()
            exploration = Exploration()
            fastestPath = FastestPath()
            idleListener.listen()
            rootBox.requestFocus()
            Arena.reset()
            Arena.setStartPoint(1, 1)
            Arena.setGoalPoint(1, 1)
            Robot.reset()

            val data: List<String> = File.readDataFile(File.ARENA_PREFERENCES)
            if (data.size < 3) return@runLater
            for (i in 0..2) {
                if (!data[i].contains(":")) continue
                val dataParts: List<String> = data[i].split(":")

                try {
                    when {
                        dataParts[0].trim() == "APS"      -> Algorithm.ACTIONS_PER_SECOND = dataParts[1].trim().toInt().coerceIn(1, 50)
                        dataParts[0].trim() == "Coverage" -> Algorithm.COVERAGE_LIMIT = dataParts[1].trim().toInt().coerceIn(0, 100)
                        dataParts[0].trim() == "Time"     -> Algorithm.TIME_LIMIT = dataParts[1].trim().toInt().coerceIn(0, 360000)
                    }
                } catch (e: NumberFormatException) {
                    continue
                }
            }

        }
    }
}
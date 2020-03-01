package wjayteo.mdp.algorithms.uicomponent

import wjayteo.mdp.algorithms.algorithm.Exploration
import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.arena.Robot
import javafx.scene.Parent
import javafx.scene.layout.HBox
import tornadofx.*
import wjayteo.mdp.algorithms.algorithm.FastestPath

class MasterView : View("Algorithms Test Ver. 0.0.1") {
    companion object {
        lateinit var exploration: Exploration
        lateinit var fastestPath: FastestPath
    }

    private val menuBar: MenuBar by inject()
    private val arenaMapView: ArenaMapView by inject()
    private val controlsView: ControlsView by inject()
    private var rootHBox: HBox by singleAssign()

    override val root: Parent = borderpane {
        top { add(menuBar) }

        center {
            rootHBox = hbox {
                style = "-fx-background-color: #FFFFFF;"

                vbox {
                    add(controlsView)
                    add(arenaMapView)
                }

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
            exploration = Exploration()
            fastestPath = FastestPath()
            rootHBox.requestFocus()
        }
    }
}
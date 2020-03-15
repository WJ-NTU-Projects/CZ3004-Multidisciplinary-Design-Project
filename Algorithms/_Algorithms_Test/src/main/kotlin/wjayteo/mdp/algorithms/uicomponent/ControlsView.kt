package wjayteo.mdp.algorithms.uicomponent

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import tornadofx.*
import wjayteo.mdp.algorithms.algorithm.Algorithm
import wjayteo.mdp.algorithms.algorithm.Algorithm.Companion.ACTUAL_RUN
import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.wifi.WifiSocketController

class ControlsView : View() {
    companion object {
        private var realRunCheckbox: CheckBox by singleAssign()
        private var explorationButton: Button by singleAssign()
        private var fastestPathButton: Button by singleAssign()
        private var stopButton: Button by singleAssign()

        fun start() {
            realRunCheckbox.isDisable = true
            explorationButton.isDisable = true
            fastestPathButton.isDisable = true
            stopButton.isDisable = false
        }

        fun stop() {
            realRunCheckbox.isDisable = false
            explorationButton.isDisable = false
            fastestPathButton.isDisable = false
            stopButton.isDisable = true
        }
    }

    override val root: Parent = hbox {
        alignment = Pos.CENTER_LEFT
        padding = Insets(16.0, 16.0, 16.0, 16.0)

        realRunCheckbox = checkbox("Actual Run") {
            isFocusTraversable = false
            action { Algorithm.ACTUAL_RUN = realRunCheckbox.isSelected }
        }

        region { prefWidth = 20.0 }

        explorationButton = button("Exploration") {
            minWidth = 100.0
            isFocusTraversable = false

            action {
                if (ACTUAL_RUN && !WifiSocketController.isConnected()) {
                    error("Not connected to RPi.")
                    return@action
                }

                start()
                MasterView.exploration.start()
            }
        }

        region { prefWidth = 10.0 }

        fastestPathButton = button("Fastest Path") {
            minWidth = 100.0
            isFocusTraversable = false

            action {
                if (ACTUAL_RUN && !WifiSocketController.isConnected()) {
                    error("Not connected to RPi.")
                    return@action
                }

                if (Arena.isInvalidCoordinates(Arena.waypoint)) {
                    error("Please set a waypoint first.")
                    return@action
                }

                start()
                MasterView.fastestPath.start()
            }
        }

        region { prefWidth = 10.0 }

        stopButton = button("Stop") {
            minWidth = 100.0
            isDisable = true
            isFocusTraversable = false

            action {
                MasterView.exploration.stop()
                MasterView.fastestPath.stop()
                stop()
            }
        }
    }
}
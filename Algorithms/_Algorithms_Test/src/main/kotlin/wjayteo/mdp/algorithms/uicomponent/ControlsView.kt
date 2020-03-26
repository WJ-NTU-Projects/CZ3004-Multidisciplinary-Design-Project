package wjayteo.mdp.algorithms.uicomponent

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.stage.StageStyle
import tornadofx.*
import wjayteo.mdp.algorithms.algorithm.Algorithm
import wjayteo.mdp.algorithms.algorithm.Algorithm.Companion.ACTUAL_RUN
import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.uicomponent.popup.ConnectionView
import wjayteo.mdp.algorithms.wifi.WifiSocketController

class ControlsView : View() {
    companion object {
        private var realRunCheckbox: CheckBox by singleAssign()
        private var connectButton: Button by singleAssign()
        private var explorationButton: Button by singleAssign()
        private var fastestPathButton: Button by singleAssign()
        private var stopButton: Button by singleAssign()
        private var connected: Boolean = false

        fun connectionChanged(connected: Boolean) {
            this.connected = connected
            //connectButton.text = if (connected) "Disconnect" else "Connect"
            MasterView.idleListener.listen()
        }

        fun start() {
            realRunCheckbox.isDisable = true
            connectButton.isDisable = true
            explorationButton.isDisable = true
            fastestPathButton.isDisable = true
            stopButton.isDisable = false
        }

        fun stop() {
            realRunCheckbox.isDisable = false
            connectButton.isDisable = false
            explorationButton.isDisable = false
            fastestPathButton.isDisable = false
            stopButton.isDisable = true
            MasterView.idleListener.listen()
        }
    }

    override val root: Parent = hbox {
        alignment = Pos.CENTER_LEFT
        padding = Insets(16.0, 16.0, 16.0, 16.0)

        realRunCheckbox = checkbox("Actual Run") {
            isFocusTraversable = false
            isSelected = true
            action { Algorithm.ACTUAL_RUN = realRunCheckbox.isSelected }
        }

        region { prefWidth = 20.0 }

        connectButton = button("Connect") {
            minWidth = 100.0
            isFocusTraversable = false

            action {
                if (connected) {
                    if (!WifiSocketController.isConnected()) return@action
                    var success = false

                    runAsync {
                        success = WifiSocketController.disconnect()
                    }.setOnSucceeded {
                        if (success) {
                            connectionChanged(false)
                            information("Disconnected from RPi successfully.")
                        } else {
                            error("Disconnection failed.")
                        }
                    }
                } else {
                    val f = find<ConnectionView>().openModal(stageStyle = StageStyle.UTILITY)
                    f?.isResizable = false
                    f?.setOnCloseRequest {  if (ConnectionView.processing) it.consume() }
                }
            }
        }

        region { prefWidth = 20.0 }

        explorationButton = button("EX") {
            minWidth = 60.0
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

        fastestPathButton = button("FP") {
            minWidth = 60.0
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
            minWidth = 60.0
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
package wjayteo.mdp.algorithms.uicomponent

import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.arena.Robot
import wjayteo.mdp.algorithms.uicomponent.popup.ConnectionView
import wjayteo.mdp.algorithms.uicomponent.popup.CoordinatesView
import javafx.scene.Parent
import javafx.scene.control.MenuItem
import javafx.stage.StageStyle
import tornadofx.*
import wjayteo.mdp.algorithms.wifi.WifiSocketController

class MenuBar : View() {
    companion object {
        private var connectMenuItem: MenuItem by singleAssign()
        private var disconnectMenuItem: MenuItem by singleAssign()

        fun connectionChanged(connected: Boolean) {
            connectMenuItem.isDisable = connected
            disconnectMenuItem.isDisable = !connected
            MasterView.idleListener.listen()
        }
    }

    override val root: Parent = menubar {
        style = "-fx-background-color: #FFFFFF;"

        menu("Connect") {
            connectMenuItem = item("Connect to RPi") {
                action {
                    val f = find<ConnectionView>().openModal(stageStyle = StageStyle.UTILITY)
                    f?.setOnCloseRequest {  if (ConnectionView.processing) it.consume() }
                }
            }

            disconnectMenuItem = item("Disconnect") {
                action {
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
                }
            }
        }

        menu("Arena") {
            item("Set Coordinates") {
                action { find<CoordinatesView>().openModal(stageStyle = StageStyle.UTILITY) }
            }

            item("Plot Fastest Path") {
                action {
                    if (Arena.isInvalidCoordinates(Arena.waypoint)) {
                        error("Please set a waypoint first.")
                        return@action
                    }

                    Arena.plotFastestPath()
                }
            }

            item("Reset") {
                action {
                    Arena.reset()
                    Robot.reset()
                }
            }

            separator()

            item("Save Descriptors") {
                action { information("This feature is currently unavailable.") }
            }

            item("Load Descriptors") {
                action { information("This feature is currently unavailable.") }
            }
        }

        menu("Test") {
            item("Test Sensor Readings") {
                action {
                    if (WifiSocketController.isConnected()) {
                        Arena.reset()
                        Robot.move(Robot.position.x, Robot.position.y)
                        MasterView.exploration.testSensorReadings()
                        return@action
                    }

                    error("Not connected to RPi.")
                }
            }
        }
    }

    init {
        disconnectMenuItem.isDisable = true
    }
}
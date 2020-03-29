package wjayteo.mdp.algorithms.uicomponent

import javafx.scene.Parent
import javafx.scene.control.MenuItem
import javafx.stage.StageStyle
import tornadofx.*
import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.arena.MapDescriptor
import wjayteo.mdp.algorithms.arena.Robot
import wjayteo.mdp.algorithms.file.File
import wjayteo.mdp.algorithms.uicomponent.popup.ArenaInfoView
import wjayteo.mdp.algorithms.uicomponent.popup.ArenaPreferenceView
import wjayteo.mdp.algorithms.uicomponent.popup.ConnectionView
import wjayteo.mdp.algorithms.uicomponent.popup.CoordinatesView
import wjayteo.mdp.algorithms.wifi.WifiSocketController
import java.io.IOException
import java.lang.NumberFormatException


class MenuBar : View() {
    companion object {
//        private var connectMenuItem: MenuItem by singleAssign()
//        private var disconnectMenuItem: MenuItem by singleAssign()
//
//        fun connectionChanged(connected: Boolean) {
//            connectMenuItem.isDisable = connected
//            disconnectMenuItem.isDisable = !connected
//            MasterView.idleListener.listen()
//        }
    }

    override val root: Parent = menubar {
        style = "-fx-background-color: #FFFFFF;"

        menu("File") {
            item("Load Arena...") {
                action {
                    try {
                        val file: java.io.File = File.selectOpenFile(currentStage) ?: return@action
                        val data: List<String> = File.readFile(file.path)

                        if (data.size != 3) {
                            error("Invalid file selected.")
                            return@action
                        }

                        if (!data[0].contains("MAPDESC:") || !data[1].contains("OBSDESC:") || !data[2].contains("WP:")) {
                            error("Invalid file selected.")
                            return@action
                        }

                        val exploreDescriptor: String = data[0].split("MAPDESC:")[1]
                        val obstacleDescriptor: String = data[1].split("OBSDESC:")[1]
                        val waypoint: String = data[2].split("WP:")[1]
                        val arenaArrays: ArrayList<Array<Array<Int>>> = MapDescriptor.fromString("$exploreDescriptor//$obstacleDescriptor", 1)
                        Arena.loadMap(arenaArrays[0], arenaArrays[1])

                        try {
                            val waypointCoordinates: List<String> = waypoint.split(",")
                            Arena.setWaypoint(waypointCoordinates[0].toInt(), waypointCoordinates[1].toInt())
                        } catch (e: NumberFormatException) {}
                    } catch (e: IOException) {
                        error("Failed to open file.")
                    }
                }
            }

            item("Save Current Arena...") {
                action {
                    try {
                        val file: java.io.File = File.selectSaveFile(currentStage) ?: return@action
                        val fileName: String = file.path

                        if (fileName.contains("connection", ignoreCase = true)) {
                            error("Protected file.")
                            return@action
                        }

                        val exploreArray: Array<Array<Int>> = Array(20) { Array(15) { 1 } }
                        val descriptor: List<String> = MapDescriptor.fromArray(exploreArray, Arena.obstacleArray, 1)
                        val line1 = "MAPDESC:${descriptor[0]}"
                        val line2 = "OBSDESC:${descriptor[1]}"
                        val line3 = "WP:${Arena.waypoint.x},${Arena.waypoint.y}"
                        File.replaceFileContent(fileName, "$line1\n$line2\n$line3")
                        information("Arena saved to file successfully.")
                    } catch (e: IOException) {
                        e.printStackTrace()
                        error("Failed to save to file.")
                    }
                }
            }

            separator()

            item("View Communication Logs...") {
                action {
                    error("Nothing to see here.", content = "I mean, look at the terminal or command prompt, the logs are there anyway.")
                }
            }
        }

//        menu("Connect") {
//            connectMenuItem = item("Connect to RPi...") {
//                action {
//                    val f = find<ConnectionView>().openModal(stageStyle = StageStyle.UTILITY)
//                    f?.isResizable = false
//                    f?.setOnCloseRequest {  if (ConnectionView.processing) it.consume() }
//                }
//            }
//
//            disconnectMenuItem = item("Disconnect") {
//                action {
//                    if (!WifiSocketController.isConnected()) return@action
//                    var success = false
//
//                    runAsync {
//                        success = WifiSocketController.disconnect()
//                    }.setOnSucceeded {
//                        if (success) {
//                            connectionChanged(false)
//                            information("Disconnected from RPi successfully.")
//                        } else {
//                            error("Disconnection failed.")
//                        }
//                    }
//                }
//            }
//        }

        menu("Arena") {
            item("Set Coordinates...") {
                action {
                    val f = find<CoordinatesView>().openModal(stageStyle = StageStyle.UTILITY)
                    f?.isResizable = false
                }
            }

            separator()

            item("Plot Obstacles") {
                action {
                    val newState: Boolean = !ArenaMapView.plotting
                    ArenaMapView.plotting = newState
                    if (newState) this.text = "Finish Plotting"
                    else this.text = "Plot Obstacles"
                }
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

            separator()

            item("Set Arena As Explored") {
                action { Arena.setAllExplored() }
            }

            item("Set Arena As Unknown") {
                action { Arena.setAllUnknown() }
            }

            item("Reset") {
                action {
                    confirm(header = "Reset arena to default state?", content = "ALL OBSTACLES WILL BE REMOVED. Please save the current arena setup to prevent data loss.", title = "Reset Arena") {
                        Arena.reset()
                        Robot.reset()
                    }
                }
            }

            separator()

            item("Arena Info") {
                action {
                    val f = find<ArenaInfoView>().openModal(stageStyle = StageStyle.UTILITY)
                    f?.isResizable = false
                }
            }

            item("Arena Preferences...") {
                action {
                    val f = find<ArenaPreferenceView>().openModal(stageStyle = StageStyle.UTILITY)
                    f?.isResizable = false
                }
            }
        }
    }

    init {
        //disconnectMenuItem.isDisable = true
    }
}
package wjayteo.mdp.algorithms.uicomponent.popup

import wjayteo.mdp.algorithms.arena.Arena
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.control.TextField
import tornadofx.*
import java.lang.NumberFormatException

class CoordinatesView : View("Set Coordinates") {
    private var startPointXTextField: TextField by singleAssign()
    private var startPointYTextField: TextField by singleAssign()
    private var goalPointXTextField: TextField by singleAssign()
    private var goalPointYTextField: TextField by singleAssign()
    private var waypointXTextField: TextField by singleAssign()
    private var waypointYTextField: TextField by singleAssign()
    private val fieldWidth: Double = 40.0

    override val root: Parent = vbox {
        style = "-fx-font-family: 'Verdana';"
        padding = Insets(8.0, 8.0, 8.0, 8.0)

        form {
            fieldset {
                field("Waypoint") {
                    waypointXTextField = textfield {
                        promptText = "x"
                        maxWidth = fieldWidth
                        prefWidth = fieldWidth
                    }

                    waypointYTextField = textfield {
                        promptText = "y"
                        maxWidth = fieldWidth
                        prefWidth = fieldWidth
                    }
                }

                field("Start Point") {
                    startPointXTextField = textfield {
                        text = "1"
                        maxWidth = fieldWidth
                        prefWidth = fieldWidth
                    }

                    startPointYTextField = textfield {
                        text = "1"
                        maxWidth = fieldWidth
                        prefWidth = fieldWidth
                    }
                }

                field("Goal Point") {
                    goalPointXTextField = textfield {
                        text = "13"
                        maxWidth = fieldWidth
                        prefWidth = fieldWidth
                    }

                    goalPointYTextField = textfield {
                        text = "18"
                        maxWidth = fieldWidth
                        prefWidth = fieldWidth
                    }
                }

                field {
                    button("Save") {
                        useMaxWidth = true

                        action {
                            this@form.isDisable = true

                            try {
                                Arena.setStartPoint(startPointXTextField.text.toInt(), startPointYTextField.text.toInt())
                            } catch (e: NumberFormatException) {
                            }

                            try {
                                Arena.setGoalPoint(goalPointXTextField.text.toInt(), goalPointYTextField.text.toInt())
                            } catch (e: NumberFormatException) {
                            }

                            try {
                                Arena.setWaypoint(waypointXTextField.text.toInt(), waypointYTextField.text.toInt())
                            } catch (e: NumberFormatException) {
                            }

                            this@form.isDisable = false
                            close()
                        }
                    }
                }
            }
        }
    }

    override fun onBeforeShow() {
        super.onBeforeShow()
        startPointXTextField.text = Arena.start.x.toString()
        startPointYTextField.text = Arena.start.y.toString()
        goalPointXTextField.text = Arena.goal.x.toString()
        goalPointYTextField.text = Arena.goal.y.toString()

        if (Arena.isInvalidCoordinates(Arena.waypoint)) return
        waypointXTextField.text = Arena.waypoint.x.toString()
        waypointYTextField.text = Arena.waypoint.y.toString()
    }
}
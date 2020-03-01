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

    override val root: Parent = vbox {
        padding = Insets(8.0, 8.0, 8.0, 8.0)

        form {
            fieldset() {
                field("Start Point") {
                    startPointXTextField = textfield {
                        text = "1"
                        maxWidth = 40.0
                        prefWidth = 40.0
                    }

                    startPointYTextField = textfield {
                        text = "1"
                        maxWidth = 40.0
                        prefWidth = 40.0
                    }
                }

                field("Goal Point") {
                    goalPointXTextField = textfield {
                        text = "13"
                        maxWidth = 40.0
                        prefWidth = 40.0
                    }

                    goalPointYTextField = textfield {
                        text = "18"
                        maxWidth = 40.0
                        prefWidth = 40.0
                    }
                }

                field("Waypoint") {
                    waypointXTextField = textfield {
                        promptText = "x"
                        maxWidth = 40.0
                        prefWidth = 40.0
                    }

                    waypointYTextField = textfield {
                        promptText = "y"
                        maxWidth = 40.0
                        prefWidth = 40.0
                    }
                }

                button("Save") {
                    useMaxWidth = true
                    vboxConstraints { marginTop = 10.0 }

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
package wjayteo.mdp.algorithms.uicomponent.popup

import wjayteo.mdp.algorithms.uicomponent.MenuBar
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import wjayteo.mdp.algorithms.file.File
import tornadofx.*
import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.arena.MapDescriptor
import wjayteo.mdp.algorithms.wifi.WifiSocketController
import java.io.IOException

class ArenaInfoView : View("Arena Info") {
    private var exploreDescriptor: TextField by singleAssign()
    private var obstacleDescriptor: TextField by singleAssign()

    override val root: Parent = vbox {
        style = "-fx-font-family: 'Verdana';"
        padding = Insets(8.0, 8.0, 8.0, 8.0)

        form {
            fieldset("Map & Obstacle Descriptor") {
                prefWidth = 700.0
                maxWidth = 700.0

                field {
                    exploreDescriptor = textfield("-") {
                        style = "-fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-font-size: 14;"
                        vboxConstraints { marginTop = 10.0 }
                        useMaxWidth = true
                        isEditable = false
                    }
                }

                field {
                    obstacleDescriptor = textfield("-") {
                        style = "-fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-font-size: 14;"
                        vboxConstraints { marginTop = 10.0 }
                        useMaxWidth = true
                        isEditable = false
                    }
                }
            }
        }
    }

    override fun onBeforeShow() {
        super.onBeforeShow()
        val descriptor: List<String> = MapDescriptor.fromArray(Arena.exploreArray, Arena.obstacleArray, 1)
        exploreDescriptor.text = descriptor[0].toUpperCase()
        obstacleDescriptor.text = descriptor[1].toUpperCase()
    }
}
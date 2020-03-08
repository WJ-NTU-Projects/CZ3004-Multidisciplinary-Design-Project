package wjayteo.mdp.algorithms.uicomponent.popup

import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.control.TextField
import tornadofx.*
import wjayteo.mdp.algorithms.algorithm.Algorithm
import wjayteo.mdp.algorithms.file.File
import java.io.IOException
import java.lang.NumberFormatException

class ArenaPreferenceView : View("Arena Preferences") {
    private var actionsTextField: TextField by singleAssign()
    private var coverageTextField: TextField by singleAssign()
    private var timeTextField: TextField by singleAssign()

    override val root: Parent = vbox {
        style = "-fx-font-family: 'Verdana';"
        padding = Insets(8.0, 8.0, 8.0, 8.0)

        form {
            fieldset {
                vboxConstraints { marginTop = 5.0 }

                field("Actions Per Second") {
                    actionsTextField = textfield {
                        useMaxWidth = true
                    }
                }

                field("Coverage Limit") {
                    coverageTextField = textfield {
                        useMaxWidth = true
                    }
                }

                field("Time Limit") {
                    timeTextField = textfield {
                        useMaxWidth = true
                    }
                }

                field {
                    button("Save") {
                        useMaxWidth = true

                        action {
                            this@form.isDisable = true

                            try {
                                val actionsPerSecond: Int = actionsTextField.text.toInt().coerceIn(1, 50)
                                val coverageLimit: Int = coverageTextField.text.toInt().coerceIn(0, 100)
                                val timeLimit: Int = timeTextField.text.toInt().coerceIn(0, 360000)
                                File.replaceDataFileContent(File.ARENA_PREFERENCES, "APS:$actionsPerSecond\nCoverage:$coverageLimit\nTime:$timeLimit")
                                Algorithm.ACTIONS_PER_SECOND = actionsPerSecond
                                Algorithm.COVERAGE_LIMIT = coverageLimit
                                Algorithm.TIME_LIMIT = timeLimit
                                this@form.isDisable = false
                                information("Preferences saved successfully.")
                                close()
                            } catch (e: NumberFormatException) {
                                error("Invalid inputs.")
                            } catch (e: IOException) {
                                error("Failed to save preferences to file.")
                            }

                            this@form.isDisable = false
                        }
                    }
                }
            }
        }

        vbox {
            style = "-fx-font-size: 10;"
            padding = Insets(0.0, 12.0, 12.0, 12.0)

            label("Actions per second is limited from 1 to 50.")
            region { prefHeight = 2.0 }
            label("Coverage limit is in percentage of grids covered: 0 to 100%.")
            region { prefHeight = 2.0 }
            label("Time limit is infinite if set to 0, otherwise limited to 360 seconds.")
        }
    }

    override fun onBeforeShow() {
        super.onBeforeShow()
        val data: List<String> = File.readDataFile(File.ARENA_PREFERENCES)
        if (data.size < 3) return

        for (i in 0..2) {
            if (!data[i].contains(":")) continue
            val dataParts: List<String> = data[i].split(":")

            when {
                dataParts[0].trim() == "APS"      -> actionsTextField.text = dataParts[1].trim()
                dataParts[0].trim() == "Coverage" -> coverageTextField.text = dataParts[1].trim()
                dataParts[0].trim() == "Time"     -> timeTextField.text = dataParts[1].trim()
            }
        }
    }
}
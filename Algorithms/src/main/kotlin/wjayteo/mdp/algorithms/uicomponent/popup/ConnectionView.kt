package wjayteo.mdp.algorithms.uicomponent.popup

import wjayteo.mdp.algorithms.uicomponent.MenuBar
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TextField
import wjayteo.mdp.algorithms.file.File
import tornadofx.*
import wjayteo.mdp.algorithms.wifi.WifiSocketController
import java.io.IOException

class ConnectionView : View("Connect to RPi") {
    companion object {
        var processing = false
    }

    private var ipAddressTextField: TextField by singleAssign()
    private var portTextField: TextField by singleAssign()
    private var progressIndicator: ProgressIndicator by singleAssign()

    override val root: Parent = stackpane {
        style = "-fx-font-family: 'Verdana';"
        padding = Insets(8.0, 8.0, 8.0, 8.0)

        form {
            fieldset {
                vboxConstraints { marginTop = 5.0 }

                field("IP Address") {
                    ipAddressTextField = textfield {
                        promptText = "IP Address"
                        maxWidth = 150.0
                        prefWidth = 150.0
                    }
                }

                field("Port") {
                    portTextField = textfield {
                        promptText = "Port"
                        maxWidth = 150.0
                        prefWidth = 150.0
                    }
                }

                field {
                    button("Connect to RPi") {
                        useMaxWidth = true

                        action {
                            processing = true
                            progressIndicator.show()
                            this@form.isDisable = true
                            var success = false

                            try {
                                val ipAddress: String = ipAddressTextField.text
                                val port: Int = portTextField.text.toInt()
                                File.replaceDataFileContent(File.CONNECTION, "IP:$ipAddress\nPort:$port")

                                runAsync {
                                    success = WifiSocketController.connect(ipAddress, port)
                                }.setOnSucceeded {
                                    progressIndicator.hide()
                                    this@form.isDisable = false
                                    processing = false

                                    if (success) {
                                        MenuBar.connectionChanged(true)
                                        information("Connected to RPi successfully.")
                                        close()
                                    } else {
                                        error("Connection failed.")
                                    }
                                }
                            } catch (e: NumberFormatException) {
                                processing = false
                                error("Invalid input.")
                            } catch (e: IOException) {
                                processing = false
                                error("Failed to save inputs to file.")
                            }

                            if (!processing) {
                                progressIndicator.hide()
                                this@form.isDisable = false
                            }
                        }
                    }
                }
            }
        }

        progressIndicator = progressindicator {
            maxWidth = 50.0
            prefWidth = 50.0
        }
    }

    override fun onBeforeShow() {
        super.onBeforeShow()
        val connectionData: List<String> = File.readDataFile(File.CONNECTION)
        if (connectionData.size < 2) return

        for (i in 0..1) {
            if (!connectionData[i].contains(":")) continue
            val dataParts: List<String> = connectionData[i].split(":")
            if (dataParts[0].trim() == "IP") ipAddressTextField.text = dataParts[1].trim()
            else if (dataParts[0].trim() == "Port") portTextField.text = dataParts[1].trim()
        }
    }

    init {
        progressIndicator.hide()
    }
}
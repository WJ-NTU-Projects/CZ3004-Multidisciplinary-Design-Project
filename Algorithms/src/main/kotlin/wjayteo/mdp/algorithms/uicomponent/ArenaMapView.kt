package wjayteo.mdp.algorithms.uicomponent

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import tornadofx.*

class ArenaMapView : View() {
    private val gridArray: Array<Array<Rectangle>> = Array(20) { Array(15) { Rectangle() } }
    private lateinit var robot: ImageView
    private lateinit var startPoint: ImageView
    private lateinit var goalPoint: ImageView
    private lateinit var waypoint: ImageView

    override val root: Parent = stackpane {
        gridpane {
            for (y in 19 downTo 0) {
                row {
                    for (x in 0..14) {
                        gridArray[y][x] = rectangle(width = 30.0, height = 30.0) {
                            fill = Color.rgb(251, 152, 164)
                            gridpaneConstraints { margin = Insets(1.0, 1.0, 1.0, 1.0) }
                        }
                    }
                }
            }
        }

        waypoint = imageview("/img_waypoint.png") {
            alignment = Pos.TOP_LEFT
            setMaxSize(94.0, 94.0)
            setPrefSize(94.0, 94.0)
            fitHeight = 94.0
            fitWidth = 94.0
            isPreserveRatio = true

            stackpaneConstraints {
                marginTop = 1.0
                marginLeft = 1.0
            }
        }

        goalPoint = imageview("/img_goalpoint.png") {
            alignment = Pos.TOP_LEFT
            setMaxSize(94.0, 94.0)
            setPrefSize(94.0, 94.0)
            fitHeight = 94.0
            fitWidth = 94.0
            isPreserveRatio = true

            stackpaneConstraints {
                marginTop = 1.0
                marginLeft = 1.0
            }
        }

        startPoint = imageview("/img_startpoint.png") {
            alignment = Pos.TOP_LEFT
            setMaxSize(94.0, 94.0)
            setPrefSize(94.0, 94.0)
            fitHeight = 94.0
            fitWidth = 94.0
            isPreserveRatio = true

            stackpaneConstraints {
                marginTop = 1.0
                marginLeft = 1.0
            }
        }

        robot = imageview("/img_robot_up.png") {
            alignment = Pos.TOP_LEFT
            setMaxSize(94.0, 94.0)
            setPrefSize(94.0, 94.0)
            fitHeight = 94.0
            fitWidth = 94.0
            isPreserveRatio = true

            stackpaneConstraints {
                marginTop = 1.0
                marginLeft = 1.0
            }
        }
    }

    fun setUnknown(x: Int, y: Int) {
        gridArray[y][x].fill = Color.rgb(251, 152, 164)
    }

    fun setExplored(x: Int, y: Int) {
        gridArray[y][x].fill = Color.rgb(152, 251, 238)
    }

    fun setObstacle(x: Int, y: Int) {
        gridArray[y][x].fill = Color.BLACK
    }

    fun setFastestPath(x: Int, y: Int) {
        gridArray[y][x].fill = Color.rgb(0, 255, 0)
    }

    fun setRobotPosition(x: Int, y: Int) {
        StackPane.setMargin(robot, Insets(32.0 * (19 - (y + 1)) + 1, 0.0, 0.0, 32.0 * (x - 1) + 1))
    }

    fun setRobotFacing(facing: Int) {
        when (facing) {
            0 -> robot.image = Image("/img_robot_up.png")
            90 -> robot.image = Image("/img_robot_right.png")
            180 -> robot.image = Image("/img_robot_down.png")
            270 -> robot.image = Image("/img_robot_left.png")
        }
    }

    fun setStartPoint(x: Int, y: Int) {
        StackPane.setMargin(startPoint, Insets(32.0 * (19 - (y + 1)) + 1, 0.0, 0.0, 32.0 * (x - 1) + 1))
    }

    fun setGoalPoint(x: Int, y: Int) {
        StackPane.setMargin(goalPoint, Insets(32.0 * (19 - (y + 1)) + 1, 0.0, 0.0, 32.0 * (x - 1) + 1))
    }

    fun setWaypoint(x: Int, y: Int) {
        waypoint.isVisible = true
        StackPane.setMargin(waypoint, Insets(32.0 * (19 - (y + 1)) + 1, 0.0, 0.0, 32.0 * (x - 1) + 1))
    }

    init {
        runLater {
            waypoint.isVisible = false
            setStartPoint(1, 1)
            setGoalPoint(13, 18)
            setRobotPosition(1, 1)
        }
    }
}
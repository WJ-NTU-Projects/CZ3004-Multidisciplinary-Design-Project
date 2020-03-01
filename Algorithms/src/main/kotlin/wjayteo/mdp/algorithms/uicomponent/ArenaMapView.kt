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
    companion object {
        private const val GRID_SIZE = 30.0
        private const val MARGIN_SIZE = 1.0
        private const val ROBOT_GRID_SIZE = GRID_SIZE + (MARGIN_SIZE * 2)
        private const val ROBOT_SIZE = (GRID_SIZE * 3) + (MARGIN_SIZE * 4)
    }
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
                        gridArray[y][x] = rectangle(width = GRID_SIZE, height = GRID_SIZE) {
                            fill = Color.rgb(251, 152, 164)
                            gridpaneConstraints { margin = Insets(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE) }
                        }
                    }
                }
            }
        }

        waypoint = imageview("/img_waypoint.png") {
            alignment = Pos.TOP_LEFT
            setMaxSize(ROBOT_SIZE, ROBOT_SIZE)
            setPrefSize(ROBOT_SIZE, ROBOT_SIZE)
            fitHeight = ROBOT_SIZE
            fitWidth = ROBOT_SIZE
            isPreserveRatio = true
        }

        goalPoint = imageview("/img_goalpoint.png") {
            alignment = Pos.TOP_LEFT
            setMaxSize(ROBOT_SIZE, ROBOT_SIZE)
            setPrefSize(ROBOT_SIZE, ROBOT_SIZE)
            fitHeight = ROBOT_SIZE
            fitWidth = ROBOT_SIZE
            isPreserveRatio = true
        }

        startPoint = imageview("/img_startpoint.png") {
            alignment = Pos.TOP_LEFT
            setMaxSize(ROBOT_SIZE, ROBOT_SIZE)
            setPrefSize(ROBOT_SIZE, ROBOT_SIZE)
            fitHeight = ROBOT_SIZE
            fitWidth = ROBOT_SIZE
            isPreserveRatio = true
        }

        robot = imageview("/img_robot_up.png") {
            alignment = Pos.TOP_LEFT
            setMaxSize(ROBOT_SIZE, ROBOT_SIZE)
            setPrefSize(ROBOT_SIZE, ROBOT_SIZE)
            fitHeight = ROBOT_SIZE
            fitWidth = ROBOT_SIZE
            isPreserveRatio = true
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
        StackPane.setMargin(startPoint, Insets(ROBOT_GRID_SIZE * (19 - (y + 1)) + MARGIN_SIZE, 0.0, 0.0, ROBOT_GRID_SIZE * (x - 1) + MARGIN_SIZE))
    }

    fun setGoalPoint(x: Int, y: Int) {
        StackPane.setMargin(goalPoint, Insets(ROBOT_GRID_SIZE * (19 - (y + 1)) + MARGIN_SIZE, 0.0, 0.0, ROBOT_GRID_SIZE * (x - 1) + MARGIN_SIZE))
    }

    fun setWaypoint(x: Int, y: Int) {
        waypoint.isVisible = true
        StackPane.setMargin(waypoint, Insets(ROBOT_GRID_SIZE * (19 - (y + 1)) + MARGIN_SIZE, 0.0, 0.0, ROBOT_GRID_SIZE * (x - 1) + MARGIN_SIZE))
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
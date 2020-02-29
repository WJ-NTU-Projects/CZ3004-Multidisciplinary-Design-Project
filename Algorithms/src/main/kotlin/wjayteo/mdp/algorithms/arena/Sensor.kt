package wjayteo.mdp.algorithms.arena

import wjayteo.mdp.algorithms.algorithm.Exploration
import java.lang.NumberFormatException

class Sensor {
    companion object {
        fun updateArenaSensor1(x: Int, y: Int, facing: Int, r: Int) {
            var x: Int = x
            var y: Int = y
            var reading: Int = r
            if (reading <= 0) return
            if (reading > 3) reading = 3

            when (facing) {
                0   -> {
                    if (reading == 1) {
                        Arena.setObstacle(x + 1, y + 1 + reading)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x + 1, y + 1 + offset)
                        }
                    }
                }

                90   -> {
                    x += 1
                    y -= 1

                    if (reading == 1) {
                        Arena.setObstacle(x + reading, y)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x + offset, y)
                        }
                    }
                }

                180   -> {
                    x -= 1
                    y -= 1

                    if (reading == 1) {
                        Arena.setObstacle(x, y - reading)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x, y - offset)
                        }
                    }
                }

                270   -> {
                    x -= 1
                    y += 1

                    if (reading == 1) {
                        Arena.setObstacle(x - reading, y)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x - offset, y)
                        }
                    }
                }
            }
        }

        fun updateArenaSensor2(x: Int, y: Int, facing: Int, r: Int) {
            var x: Int = x
            var y: Int = y
            var reading: Int = r
            if (reading <= 0) return
            if (reading > 3) reading = 3

            when (facing) {
                0   -> {
                    if (reading == 1) {
                        Arena.setObstacle(x, y + 1 + reading)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x, y + 1 + offset)
                        }
                    }
                }

                90   -> {
                    x += 1

                    if (reading == 1) {
                        Arena.setObstacle(x + reading, y)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x + offset, y)
                        }
                    }
                }

                180   -> {
                    y -= 1

                    if (reading == 1) {
                        Arena.setObstacle(x, y - reading)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x, y - offset)
                        }
                    }
                }

                270   -> {
                    x -= 1

                    if (reading == 1) {
                        Arena.setObstacle(x - reading, y)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x - offset, y)
                        }
                    }
                }
            }
        }

        fun updateArenaSensor3(x: Int, y: Int, facing: Int, r: Int) {
            var x: Int = x
            var y: Int = y
            var reading: Int = r
            if (reading <= 0) return
            if (reading > 3) reading = 3

            when (facing) {
                0   -> {
                    if (reading == 1) {
                        Arena.setObstacle(x - 1, y + 1 + reading)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x - 1, y + 1 + offset)
                        }
                    }
                }

                90   -> {
                    x += 1
                    y += 1

                    if (reading == 1) {
                        Arena.setObstacle(x + reading, y)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x + offset, y)
                        }
                    }
                }

                180   -> {
                    x += 1
                    y -= 1

                    if (reading == 1) {
                        Arena.setObstacle(x, y - reading)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x, y - offset)
                        }
                    }
                }

                270   -> {
                    x -= 1
                    y -= 1

                    if (reading == 1) {
                        Arena.setObstacle(x - reading, y)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x - offset, y)
                        }
                    }
                }
            }
        }

        fun updateArenaSensor4(x: Int, y: Int, facing: Int, r: Int) {
            var x: Int = x
            var y: Int = y
            var reading: Int = r
            if (reading <= 0) return
            if (reading > 3) reading = 3

            when (facing) {
                0   -> {
                    if (reading == 1) {
                        Arena.setObstacle(x - 1 - reading, y + 1)
                    }
                    else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x - 1 - offset, y + 1)
                        }
                    }
                }

                90   -> {
                    x += 1
                    y += 1
                    if (reading == 1) {
                        Arena.setObstacle(x, y + reading)
                    }
                    else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x, y + offset)
                        }
                    }
                }

                180   -> {
                    x += 1
                    y -= 1
                    if (reading == 1) {
                        Arena.setObstacle(x + reading, y)
                    }
                    else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x + offset, y)
                        }
                    }
                }

                270   -> {
                    x -= 1
                    y -= 1
                    if (reading == 1) {
                        Arena.setObstacle(x, y - reading)
                    }
                    else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x, y - offset)
                        }
                    }
                }
            }
        }

        fun updateArenaSensor5(x: Int, y: Int, facing: Int, r: Int) {
            var x: Int = x
            var y: Int = y
            var reading: Int = r
            if (reading <= 0) return
            if (reading > 3) reading = 3

            when (facing) {
                0   -> {
                    if (reading == 1) {
                        Arena.setObstacle(x - 1 - reading, y - 1)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x - 1 - offset, y - 1)
                        }
                    }
                }

                90   -> {
                    x -= 1
                    y += 1
                    if (reading == 1) {
                        Arena.setObstacle(x, y + reading)
                    }
                    else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x, y + offset)
                        }
                    }
                }

                180   -> {
                    x += 1
                    y += 1
                    if (reading == 1) {
                        Arena.setObstacle(x + reading, y)
                    }
                    else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x + offset, y)
                        }
                    }
                }

                270   -> {
                    x += 1
                    y -= 1
                    if (reading == 1) {
                        Arena.setObstacle(x, y - reading)
                    }
                    else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x, y - offset)
                        }
                    }
                }
            }
        }

        fun updateArenaSensor6(x: Int, y: Int, facing: Int, r: Int) {
            var x: Int = x
            var y: Int = y
            var reading: Int = r
            if (reading <= 0) return
            if (reading > 3) reading = 3

            when (facing) {
                0   -> {
                    if (reading == 1) {
                        Arena.setObstacle(x + 1 + reading, y)
                    } else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x + 1 + offset, y)
                        }
                    }
                }

                90   -> {
                    y -= 1

                    if (reading == 1) {
                        Arena.setObstacle(x, y - reading)
                    }
                    else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x, y - offset)
                        }
                    }
                }

                180   -> {
                    x -= 1

                    if (reading == 1) {
                        Arena.setObstacle(x - reading, y)
                    }
                    else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x - offset, y)
                        }
                    }
                }

                270   -> {
                    y += 1

                    if (reading == 1) {
                        Arena.setObstacle(x, y + reading)
                    }
                    else {
                        for (offset in reading downTo 1) {
                            Arena.setExplored(x, y + offset)
                        }
                    }
                }
            }
        }
    }
}

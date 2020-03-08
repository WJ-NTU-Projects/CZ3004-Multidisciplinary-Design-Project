#include "WallHug.h"

WallHug::WallHug() {

}

void WallHug::init() {
    memset(arena, 0, sizeof(arena));
    move(1, 1);
    facing = 0;
}

void WallHug::move(int x, int y) {
    if (!isMovable(x, y)) return;
    position[0] = x;
    position[1] = y;

    for (int b = -1; b <= 1; b++) {
        for (int a = -1; a <= 1; a++) {
            setExplored(x + a, y + b);
        }
    }
}

void WallHug::moveForward() {
    int x = position[0];
    int y = position[1];

    switch (facing) {
        case 0:
            y += 1;
            break;
        case 90:
            x += 1;
            break;
        case 180:
            y -= 1;
            break;
        case 270:
            x -= 1;
            break;
    }

    move(x, y);
}

void WallHug::turn(int angle) {
    facing += angle;
    if (facing >= 360) facing -= 360;
    else if (facing < 0) facing += 360;
}

void WallHug::setExplored(int x, int y) {
    if (isObstacle(x, y)) return;
    arena[y][x] = 1;
}

void WallHug::setObstacle(int x, int y) {
    if (isInvalidCoordinates(x, y, false)) return;
    arena[y][x] = 2;
}

bool WallHug::isFrontMovable() {
    switch (facing) {
        case 0: return isMovable(position[0], position[1] + 1);
        case 90: return isMovable(position[0] + 1, position[1]);
        case 180: return isMovable(position[0], position[1] - 1);
        case 270: return isMovable(position[0] - 1, position[1]);
        default: return false;
    }
}

bool WallHug::isLeftMovable() {
    switch (facing) {
        case 0: return isMovable(position[0] - 1, position[1]);
        case 90: return isMovable(position[0], position[1] + 1);
        case 180: return isMovable(position[0] + 1, position[1]);
        case 270: return isMovable(position[0], position[1] - 1);
        default: return false;
    }
}

bool WallHug::isBackAtStart() {
    return (position[0] == 1 && position[1] == 1);
}

void WallHug::updateSensor1(int reading) {
    if (reading != 1) return;
    int x = position[0];
    int y = position[1];

    switch (facing) {
        case 0:
            x += 1;
            y += 1;
            setObstacle(x, y + 1);
            break;
        case 90:
            x += 1;
            y -= 1;
            setObstacle(x + 1, y);
            break;
        case 180:
            x -= 1;
            y -= 1;
            setObstacle(x, y - 1);
            break;
        case 270:
            x -= 1;
            y += 1;
            setObstacle(x - 1, y);
            break;
    }
}

void WallHug::updateSensor2(int reading) {
    if (reading != 1) return;
    int x = position[0];
    int y = position[1];

    switch (facing) {
        case 0:
            y += 1;
            setObstacle(x, y + 1);
            break;
        case 90:
            x += 1;
            setObstacle(x + 1, y);
            break;
        case 180:
            y -= 1;
            setObstacle(x, y - 1);
            break;
        case 270:
            x -= 1;
            setObstacle(x - 1, y);
            break;
    }
}

void WallHug::updateSensor3(int reading) {
    if (reading != 1) return;
    int x = position[0];
    int y = position[1];

    switch (facing) {
        case 0:
            x -= 1;
            y += 1;
            setObstacle(x, y + 1);
            break;
        case 90:
            x += 1;
            y += 1;
            setObstacle(x + 1, y);
            break;
        case 180:
            x += 1;
            y -= 1;
            setObstacle(x, y - 1);
            break;
        case 270:
            x -= 1;
            y -= 1;
            setObstacle(x - 1, y);
            break;
    }
}

void WallHug::updateSensor4(int reading) {
    if (reading != 1) return;
    int x = position[0];
    int y = position[1];

    switch (facing) {
        case 0:
            x -= 1;
            y += 1;
            setObstacle(x - 1, y);
            break;
        case 90:
            x += 1;
            y += 1;
            setObstacle(x, y + 1);
            break;
        case 180:
            x += 1;
            y -= 1;
            setObstacle(x + 1, y);
            break;
        case 270:
            x -= 1;
            y -= 1;
            setObstacle(x, y - 1);
            break;
    }
}

void WallHug::updateSensor5(int reading) {
    if (reading != 1) return;
    int x = position[0];
    int y = position[1];

    switch (facing) {
        case 0:
            x -= 1;
            y -= 1;
            setObstacle(x - 1, y);
            break;
        case 90:
            x -= 1;
            y += 1;
            setObstacle(x, y + 1);
            break;
        case 180:
            x += 1;
            y += 1;
            setObstacle(x + 1, y);
            break;
        case 270:
            x += 1;
            y -= 1;
            setObstacle(x, y - 1);
            break;
    }
}

bool WallHug::isObstacle(int x, int y) {
    if (isInvalidCoordinates(x, y, false)) return true;
    return (arena[y][x] == 2);
}

bool WallHug::isMovable(int x, int y) {
    if (isInvalidCoordinates(x, y, true)) return false;

    for (int b = -1; b <= 1; b++) {
        for (int a = -1; a <= 1; a++) {
            if (isObstacle(x + a, y + b)) return false;
        }
    }

    return true;
}

bool WallHug::isInvalidCoordinates(int x, int y, bool robotSize) {
    if (robotSize) return (x < 1 || x > 13 || y < 1 || y > 18);
    else return (x < 0 || x > 14 || y < 0 || y > 19);
}









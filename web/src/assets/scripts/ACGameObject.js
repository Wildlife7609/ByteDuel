const AC_Game_OBJECTS = [];

export default class AcGameObject {
    constructor() {
        AC_Game_OBJECTS.push(this);
        this.has_called_start = false; // whether start() has run
        this.timedelta = 0; // time since the previous frame
    }

    start() { // runs once, on the first frame
    }

    update() { // runs every frame except the first

    }
    on_destroy() { // runs once, when the object is destroyed
    }

    destroy() { // removes the object
        this.on_destroy();
        for (let i in AC_Game_OBJECTS) {
            if (AC_Game_OBJECTS[i] === this) {
                AC_Game_OBJECTS.splice(i, 1);
                break;
            }
        }
    }
}

let last_timestamp = 0; // timestamp of the previous frame

const step = (timestamp) => {
    for (let obj of AC_Game_OBJECTS) {
        if (!obj.has_called_start) {
            obj.has_called_start = true;
            obj.start();
        } else {
            obj.timedelta = timestamp - last_timestamp;
            obj.update();
        }
    }
    last_timestamp = timestamp;
    requestAnimationFrame(step);
}

requestAnimationFrame(step);
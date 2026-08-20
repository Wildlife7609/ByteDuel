const AC_Game_OBJECTS = [];

export default class AcGameObject {
    constructor() {
        AC_Game_OBJECTS.push(this);
        this.has_called_start = false; //是否执行过start
        this.timedelta = 0; //上一帧到当前帧的时间间隔
    }

    start() { //创建时执行一次
    }

    update() { //每一帧执行一次，除了第一帧

    }
    on_destroy() { //当对象被销毁时，执行一次
    }

    destroy() { //销毁时执行一次
        this.on_destroy();
        for (let i in AC_Game_OBJECTS) {
            if (AC_Game_OBJECTS[i] === this) {
                AC_Game_OBJECTS.splice(i, 1);
                break;
            }
        }
    }
}

let last_timestamp = 0; //上一帧到当前帧的时间间隔

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
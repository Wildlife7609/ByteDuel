import AcGameObject from "./ACGameObject.js";

export class GameMap extends AcGameObject {
    constructor(ctx, parent) {
        super();
        this.ctx = ctx;
        this.parent = parent;
        this.L = 0; //绝对像素
    }
    start() {
        this.set_map_size();
    }
    set_map_size() {
    }
    update() {
        this.render()
    }
    render() {
        this.ctx.fillStyle = "#00aa00";
        this.ctx.fillRect(0, 0, this.parent.canvas.width, this.parent.canvas.height);
    }
}

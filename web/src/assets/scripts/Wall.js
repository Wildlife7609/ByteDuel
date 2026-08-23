import AcGameObject from "./ACGameObject.js";

export class Wall extends AcGameObject {
    constructor(r, c, gamemap) {
        super();
        this.r = r;
        this.c = c;
        this.gamemap = gamemap;
        this.color = "#475569"; // 温暖胡桃木色

    }

    start() {

    }

    update() {
        this.render();
    }

    render() {
        const L = this.gamemap.L;
        const ctx = this.gamemap.ctx;
        ctx.fillStyle = this.color;
        ctx.fillRect(this.c * L, this.r * L, L, L);
    }
}
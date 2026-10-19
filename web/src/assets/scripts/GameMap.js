import AcGameObject from "./ACGameObject.js";
import { Wall } from "./Wall.js";
import { Snake } from "./Snake.js";
import { usePkStore } from "@/stores/pk.js";
import { useRecordStore } from "@/stores/record.js";
import { useUserStore } from "@/stores/user.js";

export class GameMap extends AcGameObject {
    constructor(ctx, parent, gameObject) {
        super();
        this.ctx = ctx;
        this.parent = parent;
        this.gameObject = gameObject;
        this.L = 0; // cell size in pixels

        this.rows = gameObject.map.length;
        this.cols = gameObject.map[0].length;
        // Create walls before snakes: objects are drawn in creation order, so the name labels above the heads are not covered by walls
        this.walls = [];
        this.create_walls();

        // The logged-in user's snake gets "(you)" on its label; the id in the store is a string, a_id / b_id are numbers
        const myId = String(useUserStore().id);
        this.snakes = [
            new Snake({ id: 0, color: "#EC4899", r: gameObject.a_sx, c: gameObject.a_sy,
                username: gameObject.a_username, is_me: String(gameObject.a_id) === myId }, this),
            new Snake({ id: 1, color: "#06B6D4", r: gameObject.b_sx, c: gameObject.b_sy,
                username: gameObject.b_username, is_me: String(gameObject.b_id) === myId }, this),
        ]

        this.record_interval_id = null;
    }

    create_walls() {
        const g = this.gameObject.map;
        if (!g) return;

        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if (g[r][c]) {
                    this.walls.push(new Wall(r, c, this));
                }
            }
        }
    }

    add_listening_event() {
        const recordStore = useRecordStore();
        if (recordStore.is_record) {
            const a_steps = recordStore.a_steps;
            const b_steps = recordStore.b_steps;
            const loser = recordStore.loser;
            const [snake0, snake1] = this.snakes;
            recordStore.updateIsFinished(false);
            let k = 0;
            this.record_interval_id = setInterval(() => {
                if (k >= a_steps.length) {
                    snake0.finish(loser === "all" || loser === "A");
                    snake1.finish(loser === "all" || loser === "B");
                    clearInterval(this.record_interval_id);
                    recordStore.updateIsFinished(true);
                } else {
                    snake0.set_direction(parseInt(a_steps[k]));
                    snake1.set_direction(parseInt(b_steps[k]));
                }
                k++;
            }, 300); // snakes move 5 cells/s, about 200ms per step; 300ms leaves some margin
        }
        else {
            this.ctx.canvas.focus();
            const pkStore = usePkStore();
            if (pkStore.selectedBotId !== -1) return; // a bot is playing, so ignore the keyboard
            this.ctx.canvas.addEventListener("keydown", e => {
                const key = e.key.toLowerCase(); // so keys still work with Caps Lock on
                let d = -1;
                if (key === "w") d = 0;
                else if (key === "d") d = 1;
                else if (key === "s") d = 2;
                else if (key === "a") d = 3;

                if (d >= 0) {
                    pkStore.sendMove(d);
                }
            });
        }

    }

    start() {
        this.add_listening_event();
    }

    on_destroy() {
        for (let wall of this.walls) {
            wall.destroy();
        }
        for (let snake of this.snakes) {
            snake.destroy();
        }
        clearInterval(this.record_interval_id);
    }

    update_size() {
        this.L = parseInt(Math.min(this.parent.clientWidth / this.cols, this.parent.clientHeight / this.rows));
        this.ctx.canvas.width = this.L * this.cols;
        this.ctx.canvas.height = this.L * this.rows;
    }

    check_valid(cell) { // check whether the snake hits a wall, itself or the other snake
        if (this.gameObject.map[cell.r][cell.c]) return false;

        for (const snake of this.snakes) {
            let k = snake.cells.length;
            if (!snake.check_tail_increasing()) { // if the tail does not grow this step, it moves away, so skip it
                k--;
            }
            for (let i = 0; i < k; i++) {
                if (snake.cells[i].r === cell.r && snake.cells[i].c === cell.c) return false;
            }
        }
        return true;

    }

    next_step() {
        for (const snake of this.snakes) {
            snake.next_step();
        }
    }

    check_ready() { // ready for the next step: every snake is idle and has a queued direction
        for (const snake of this.snakes) {
            if (snake.status !== "idle") return false;
            if (!snake.has_next_step()) return false;
        }
        return true;
    }

    update() {
        this.update_size();
        if (this.check_ready()) {
            this.next_step()
        }
        this.render()
    }

    render() {
        const color_even = "#AAD751"; // light grass green
        const color_odd = "#A2D149";  // dark grass green

        for (let i = 0; i < this.rows; i++) {
            for (let j = 0; j < this.cols; j++) {
                // 1. Checkerboard background
                this.ctx.fillStyle = (i + j) % 2 === 0 ? color_even : color_odd;
                this.ctx.fillRect(j * this.L, i * this.L, this.L, this.L);

                // 2. Grass tufts, placed by a fixed hash of the coordinates so they never move
                if ((i * 7 + j * 13) % 9 === 0) {
                    const cx = (j + 0.5) * this.L;
                    const cy = (i + 0.5) * this.L;

                    // two small blades of grass
                    this.ctx.strokeStyle = "rgba(74, 120, 30, 0.4)";
                    this.ctx.lineWidth = this.L * 0.04;
                    this.ctx.lineCap = "round";

                    this.ctx.beginPath();
                    this.ctx.moveTo(cx - this.L * 0.08, cy + this.L * 0.08);
                    this.ctx.lineTo(cx - this.L * 0.12, cy - this.L * 0.08);

                    this.ctx.moveTo(cx, cy + this.L * 0.1);
                    this.ctx.lineTo(cx + this.L * 0.08, cy - this.L * 0.1);
                    this.ctx.stroke();
                }
            }
        }
    }
}
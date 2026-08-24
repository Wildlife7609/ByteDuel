export class Cell {
    constructor(r, c, game_map) {
        this.r = r;
        this.c = c;
        this.game_map = game_map;

        this.x = c + 0.5;
        this.y = r + 0.5;

    }
}
import * as PIXI from "pixi.js";

export class Renderer {
    constructor(graph) {
        let contentDiv = document.getElementById("content");

        const pixi = new PIXI.Application({
            width: document.body.clientWidth,
            height: document.body.clientHeight,
            transparent: true,
            resolution: 2
        });
        contentDiv.appendChild(pixi.view);
        pixi.renderer.autoResize = true;

        window.addEventListener('resize', () => {
            console.log("resizing");
            pixi.renderer.resize(document.body.clientWidth, document.body.clientHeight)
        }, true);
    }
}
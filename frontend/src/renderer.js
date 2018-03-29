import * as PIXI from "pixi.js";
import * as d3 from 'd3';

export class Renderer {
    constructor(graph) {
        this.graph = graph;
        this.app = new PIXI.Application({
            antialias: true,
            width: document.body.clientWidth,
            height: document.body.clientHeight,
            transparent: true,
            resolution: 1
        });

        let contentDiv = document.getElementById("content");
        this.app.view.id = "pixiCanvas";
        contentDiv.appendChild(this.app.view);

        this.app.renderer.autoResize = true;
        window.addEventListener('resize', () => {
            this.app.renderer.resize(document.body.clientWidth, document.body.clientHeight);
            console.log(`Resizing to: ${this.app.renderer.width}x${this.app.renderer.height}`)
        }, true);

        d3.zoom()
            .scaleExtent([0.2, 1])
            .on("zoom", () => {
                let s = this.app.stage;
                s.position.x = d3.event.transform.x;
                s.position.y = d3.event.transform.y;
                s.scale.x = d3.event.transform.k;
                s.scale.y = d3.event.transform.k;
            })(d3.select('#pixiCanvas'))
    }

    start() {
        this.app.ticker.add(this.mainLoop.bind(this));
        this.texts = {};
        this.lines = new PIXI.Graphics();
        this.app.stage.addChild(this.lines);

        this.point = new PIXI.Graphics();
        this.app.stage.addChild(this.point)
    }

    mainLoop() {
        this.graph.tags.forEach(tag => {
            this.handleTag(tag);
        });
        this.drawLines();
        this.drawPoint();
    }

    handleTag(tag) {
        if (this.texts[tag.tag]) {
            this.updateText(this.texts[tag.tag], tag);
        } else {
            this.createText(tag);
        }
    }

    updateText(text, tag) {
        text.style = Renderer.styleForTag(tag);
        this.setTextPosition(text, tag)
    }

    createText(tag) {
        let text = new PIXI.Text(`#${tag.tag}`, Renderer.styleForTag(tag));
        text.anchor.set(0.5);
        text.scale.x = 0.5;
        text.scale.y = 0.5;
        this.texts[tag.tag] = text;
        this.setTextPosition(text, tag);
        this.app.stage.addChild(text);
    }

    static styleForTag(tag) {
        return new PIXI.TextStyle({
            fontFamily: 'Arial',
            fontSize: (tag.count * 4 + 8) * 2,
            fill: '#ffffff',
            stroke: tag.highlightColor,
            strokeThickness: strokeThickness(tag),
            lineJoin: "round"
        });

        function strokeThickness(tag) {
            const multiplier = 8;
            if(!isNaN(tag.highlightAlpha)) {
                return tag.highlightAlpha * multiplier
            } else {
                return multiplier;
            }
        }
    }

    setTextPosition(text, tag) {
        text.x = this.translateX(tag.x);
        text.y = this.translateY(tag.y);
    }

    drawLines() {
        let g = this.lines;
        g.clear();
        this.graph.links.forEach(link => {
            g.lineStyle(Math.min(link.strength, 6), 0xFFFFFF, 1 - Math.min(link.strength * 0.01 + 0.20, 1));
            g.moveTo(this.translateX(link.source.x), this.translateY(link.source.y));
            g.lineTo(this.translateX(link.target.x), this.translateY(link.target.y));
        })
    }

    drawPoint() {
        this.point.clear();
        this.point.lineStyle(0);
        this.point.beginFill(0xFFFFFF, 0.2);
        this.point.drawCircle(this.translateX(0), this.translateY(0), 5);
        this.point.endFill();
    }


    translateX(x) {
        return x + this.app.renderer.width / 2 ;
    }

    translateY(y) {
        return y + this.app.renderer.height / 2;
    }
}
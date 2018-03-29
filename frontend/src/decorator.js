import * as d3 from 'd3';

export class Decorator {

    constructor(graph) {
        this.graph = graph
    }

    decorateGraph() {
        let freshTags = this.graph.tags.filter(t => t.fresh);
        freshTags.forEach(t => {
            t.highlightColor = Decorator.highlightColor(t.lastUpdatedBySex);
            t.highlightAlpha = 1;
            t.fresh = false;
        });
        this.graph.tags.forEach(t => {
            t.highlightAlpha = Math.max(t.highlightAlpha - 0.002, 0);
        })
    }

    static highlightColor(sex) {
        switch (sex) {
            case 'male':
                return 0x46abf2;
            case 'female':
                return 0xf246d0;
            default:
                return 0xcccbca;
        }
    }

}
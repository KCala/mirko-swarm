import * as d3 from 'd3';

export class GraphUpdater {
    constructor(initialGraph) {
        this.graph = initialGraph;
    }

    subscribeGraphToWSUpdates(updated) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                console.log("new one!");
                let contentDiv = d3.select("#content");
                // console.log(exampleGraph.tags);
                this.graph.tags.push({
                    tag: `nowy${1}`, count: Math.random() * 10,
                    x: +contentDiv.style("width").slice(0, -2) / 2,
                    y: contentDiv.style("height").slice(0, -2) / 2
                });
                this.graph.links.push({source: `nowy${1}`, target: "mirko", strength: 20});
                resolve();
            }, 1000);
        })
        // console.log(this.graph);
        // let i = 0;

        // updated();
    }
}
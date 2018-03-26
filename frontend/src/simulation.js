import * as d3 from 'd3';


export class Simulation {


    constructor(graph) {
        this.graph = graph;
    }

    /**
     * Initialises the simulation.
     * Graph object will be from this moment updated (mutated) by the simulation. Both tags and links will get additional
     * fields their position and velocity on the graph.
     * For tags it is "x" and "y"
     * For links "source|target.[x|y]
     * @param graph object containing nodes and links which are drawn on the graph. It is modified by simulation on every tick.
     */
    startSimulation(ticked) {
        this.simulation = d3.forceSimulation();
        this.simulation.velocityDecay(0.95);
        this.simulation.alphaDecay(0);
        this.simulation.force("link", d3.forceLink().id(d => d.tag).strength(d => d.strength));
        // this.simulation.force("link", d3.forceLink().id(d => d.tag).strength(0.2));
        this.simulation.force("charge", d3.forceManyBody().strength(d => -(d.count * d.tag.length * 100 + 10)));
        // this.simulation.force("gravity", d3.forceManyBody().strength(10).distanceMin(200));
        // this.simulation.force("collision", d3.forceCollide(d => d.count * d.tag.length + 1 * 1.1 + 20));

        this.simulation.nodes(this.graph.tags);
        this.simulation.on("tick", () => ticked(this.graph));
    }

    updateGraph() {
        if(this.simulation) {
            this.simulation.nodes(this.graph.tags)
        }
    }

}
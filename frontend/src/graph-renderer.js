import * as d3 from 'd3';


export class GraphRenderer {

    /** D3 force simulation */
    // simulation;

    /** Svg "g" element containing all links */
    // linksGroup;

    /** Svg "g" element containing all tags (graph nodes) */
    // tagsGroup;

    /**
     * SVG lines representing links.
     * They have d3 data attribute attached, which is updated by the simulation.
     */
    // links;

    /**
     * SVG text objects representing tags.
     * They have d3 data attribute attached, which is updated by the simulation.
     */
    // tags;

    /**
     * Initialises the simulation.
     * Graph object will be from this moment updated (mutated) by the simulation. Both tags and links will get additional
     * fields such as "x", "y" representing their position on the graph, and "vx" and "vy" representing their velocity.
     * @param svg SVG object on which the graph's elements will be placed
     * @param graph object containing nodes and links which are drawn on the graph. It is modified by simulation on every tick.
     *  Every time it is modified from the outside, {@link updateGraph} should be called to register changes in the simulation.
     */
    constructor(svg, graph) {
        this.svg = svg;
        this.graph = graph;

        const width = +svg.attr('width');
        const height = +svg.attr("height");

        this.linksGroup = svg.append("g").attr("class", "links");
        this.tagsGroup = svg.append("g").attr("class", "tags");

        this.simulation = d3.forceSimulation()
            .force("link", d3.forceLink().id(d => d.tag).strength(0.005))
            .force("charge", d3.forceManyBody().strength(-200))
            .force("center", d3.forceCenter(width / 2, height / 2))
            .force("collision", d3.forceCollide(30))
            .force("x", d3.forceX(width / 2))
            .force("y", d3.forceY(height / 2));

        this.updateGraph();
        this.simulation.on("tick", () => this.ticked());
    }

    /**
     * Updates simulation according to the changes in the graph object. Should be called after every change in it.
     */
    updateGraph() {
        this.updateTags();
        this.updateLinks();
        this.simulation.alpha(1).restart();
    }

    updateTags() {
        this.simulation.nodes(this.graph.tags);
        let tagsSelection = this.tagsGroup.selectAll("text").data(this.graph.tags);
        let newTags = tagsSelection.enter().append("text")
            .style('font-size', d => d.count * 4)
            .text(d => `#${d.tag}`)
            .attr("text-anchor", "middle")
            .attr("count", d => d.count)
            .attr("fill", "#87e9ff")
            .call(d3.drag()
                .on("start", dragStarted)
                .on("drag", dragged)
                .on("end", dragEnded))
            .merge(tagsSelection);

        newTags.transition().duration(3000).attr("fill", "white");
        this.tags = newTags.merge(tagsSelection);

        function dragStarted(d) {
            if (!d3.event.active) this.simulation.alphaTarget(0.3).restart();
            d.fx = d.x;
            d.fy = d.y;
        }

        function dragged(d) {
            d.fx = d3.event.x;
            d.fy = d3.event.y;
        }

        function dragEnded(d) {
            if (!d3.event.active) this.simulation.alphaTarget(0);
            d.fx = null;
            d.fy = null;
        }
    }

    updateLinks() {
        this.simulation.force("link").links(this.graph.links);
        let linksSelection = this.linksGroup.selectAll("line").data(this.graph.links);
        this.links = linksSelection.enter().append("line").merge(linksSelection);
    }

    /**
     * Represents the actual state of the simulation on the SVG representation.
     * Should be called by simulation on every tick.
     */
    ticked() {
        this.links
            .attr("x1", d => d.source.x)
            .attr("y1", d => d.source.y)
            .attr("x2", d => d.target.x)
            .attr("y2", d => d.target.y);

        this.tags
            .attr("x", d => d.x)
            .attr("y", d => d.y);
    }

}
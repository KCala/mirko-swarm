import * as d3 from 'd3';


export class GraphRenderer {

    /**
     * Fields description (since ES6 has classes without fields apparently :/)
     *
     * simulation - D3 force simulation
     * linksGroup - Svg "g" element containing all links
     * tagsGroup - Svg "g" element containing all tags (graph nodes)
     * links - SVG lines representing links.
     *         They have d3 data attribute attached, which is updated by the simulation.
     * tags - SVG text objects representing tags.
     *        They have d3 data attribute attached, which is updated by the simulation.
     */

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

        this.resizeSvgToWindow();
        this.subscribeSvgForWindowResizeEvents();

        this.width = +svg.attr('width');
        this.height = +svg.attr("height");

        this.linksGroup = svg.append("g").attr("class", "links");
        this.tagsGroup = svg.append("g").attr("class", "tags");

        this.simulation = d3.forceSimulation()
            .force("link", d3.forceLink().id(d => d.tag).strength(0.2))
            .force("charge", d3.forceManyBody().strength(-200))
            .force("collision", d3.forceCollide(30));

        this.recenterSimulation();
        this.updateGraph();
        this.simulation.on("tick", () => this.ticked());
    }

    /**
     * Updates simulation according to the changes in the graph object. Should be called after every change in it.
     */
    updateGraph() {
        console.log("Updating graph!");
        this.updateTags();
        this.updateLinks();
        this.simulation.alpha(1).restart();
    }

    updateTags() {
        this.simulation.nodes(this.graph.tags);
        let oldTags = this.tagsGroup.selectAll("text").data(this.graph.tags);
        //things below only affect new tags though!
        let allTags = oldTags.enter().append("text")
            .text(d => `#${d.tag}`)
            .attr("text-anchor", "middle")
            .attr("fill", "#87e9ff")
            .call(d3.drag()
                .on("start", dragStarted.bind(this))
                .on("drag", dragged.bind(this))
                .on("end", dragEnded.bind(this)))
            .merge(oldTags);

        allTags
            .attr("count", d => d.count)
            .style('font-size', d => d.count * 4 + 4)
            .transition().duration(3000).attr("fill", "white");
        this.tags = allTags.merge(oldTags);

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

    recenterSimulation() {
        this.simulation
            .force("center", d3.forceCenter(this.width / 2, this.height / 2))
            .force("x", d3.forceX(this.width / 2))
            .force("y", d3.forceY(this.height / 2))
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

    resizeSvgToWindow() {
        let contentDiv = d3.select("#content");
        this.width = +contentDiv.style("width").slice(0, -2);
        this.height = contentDiv.style("height").slice(0, -2);

        this.svg.attr('width', this.width).attr('height', this.height);
        if (this.simulation) {
            this.recenterSimulation();
            this.simulation.alpha(1).restart();
        }
    }

    subscribeSvgForWindowResizeEvents() {
        window.addEventListener('resize', () => {
            console.log("resizing");
            this.resizeSvgToWindow();
        }, true);
    }
}
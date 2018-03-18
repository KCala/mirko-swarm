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

        this.viewPort = svg.append("g").attr("class", "view-port");
        this.linksGroup = this.viewPort.append("g").attr("class", "links");
        this.tagsGroup = this.viewPort.append("g").attr("class", "tags");

        this.center = this.viewPort.append("circle").attr("r", 4).attr("fill", "white").attr("opacity", "0.2");
        //r="40" stroke="black" stroke-width="3" fill="red" />

        this.simulation = d3.forceSimulation();
        this.simulation.velocityDecay(0.95);
        this.simulation.alphaDecay(0.001);
        this.simulation.force("link", d3.forceLink().id(d => d.tag).strength(d => d.strength ));
        // this.simulation.force("link", d3.forceLink().id(d => d.tag).strength(0.2));
        this.simulation.force("charge", d3.forceManyBody().strength(d => -(d.count * d.tag.length * 100 + 10)));
        // this.simulation.force("gravity", d3.forceManyBody().strength(10).distanceMin(200));
        // this.simulation.force("collision", d3.forceCollide(d => d.count * d.tag.length + 1 * 1.1 + 20));

        this.recenterSimulation();
        this.updateGraph();
        this.simulation.on("tick", () => this.ticked());

        d3.zoom()
        // .scaleExtent([1, 50]
            .on("zoom", this.zoomHandler.bind(this))(svg);
    }

    /**
     * Updates simulation according to the changes in the graph object. Should be called after every change in it.
     */
    updateGraph() {
        // console.log("Updating graph!");
        this.updateTags();
        this.updateLinks();

        this.simulation.alpha(1).restart();
    }

    updateTags() {
        this.graph.tags.forEach(tag => {
            if(!tag.x) {
                // tag.x = Math.floor(Math.random() * this.width);
                // tag.y = Math.floor(Math.random() * this.height);
                tag.x = this.width/2
                tag.y = this.height/2
            }
        });
        this.simulation.nodes(this.graph.tags);
        //things below only affect new tags though!
        let oldTags = this.tagsGroup.selectAll("g.tag-group").data(this.graph.tags);
        let newTags = oldTags.enter()
            .append("g")
            .attr("class", "tag-group")
            .attr("text-anchor", "middle")
            .call(d3.drag()
                .on("start", dragStarted.bind(this))
                .on("drag", dragged.bind(this))
                .on("end", dragEnded.bind(this)));

        newTags.append("text")
            .attr("class", "tag-text")
            .text(d => `#${d.tag}`);

        newTags.append("text")
            .attr("class", "tag-glow")
            .attr("filter", "url(#glow)")
            .attr('fill-opacity', '1.0')
            .text(d => `#${d.tag}`);

        let allTags = newTags.merge(oldTags);
        // let allTags = oldTags.merge(newTags);

        allTags
            .filter(d => d.fresh)
            .select('.tag-glow')
            .attr('fill-opacity', '1.0')
            .attr("fill", d => {
                switch (d.lastUpdatedBySex) {
                    case 'male':
                        return 'rgba(70, 171, 242, 1)';
                    case 'female':
                        return 'rgba(242, 70, 208, 1)';
                    default:
                        return 'white';
                }
            });

        allTags
            .attr("count", d => d.count)
            .style('font-size', d => d.count * 4 + 4)
            .select('.tag-glow')
            .transition().duration(5000).attr("fill-opacity", "0.0");

        this.tags = allTags;

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
        let oldLinks = this.linksGroup.selectAll("line").data(this.graph.links);
        let allLinks = oldLinks.enter()
            .append("line")
            .merge(oldLinks);

        allLinks.attr("opacity", d => Math.min(d.strength * 0.01 + 0.20, 1));

        this.links = allLinks;
    }

    recenterSimulation() {

        // this.simulation.force("center", d3.forceCenter(this.width / 2, this.height / 2))
        this.simulation.force("x", d3.forceX(this.width / 2).strength(0.1));
        this.simulation.force("y", d3.forceY(this.height / 2).strength(0.1));
        // let radius = Math.min(this.height, this.width)/3
        // console.log(radius)
        // this.simulation.force("radial", d3.forceRadial(Math.min(this.height, this.width)/3, this.width/2, this.height/2));
        this.center.attr("cx", this.width/2);
        this.center.attr("cy", this.height/2);
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

        this.tags.selectAll('text')
            .attr("x", d => d.x)
            .attr("y", d => d.y);
    }

    zoomHandler() {
        this.viewPort.attr("transform", d3.event.transform);
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
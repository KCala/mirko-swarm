import * as d3 from 'd3';

let simulation;
let linksGroup;
let tagsGroup;
let svg;
let graph;

let links;
let tags;

export function initGraph(anSvg, aGraph) {
    svg = anSvg;
    graph = aGraph;

    const width = +svg.attr('width');
    const height = +svg.attr("height");

    linksGroup = svg.append("g").attr("class", "links");
    tagsGroup = svg.append("g").attr("class", "tags");

    simulation = d3.forceSimulation()
        .force("link", d3.forceLink().id(d => d.tag))
        .force("charge", d3.forceManyBody().strength(-200))
        .force("center", d3.forceCenter(width / 2, height / 2))
        .force("collision", d3.forceCollide(30))
        .force("x", d3.forceX(width / 2))
        .force("y", d3.forceY(height / 2));

    updateGraph();
    simulation.on("tick", ticked);
}

export function updateGraph() {
    simulation.nodes(graph.tags);
    simulation.force("link").links(graph.links).strength(0.005);

    let linksSelection = linksGroup.selectAll("line")
        .data(graph.links);

    links = linksSelection
        .enter().append("line")
        .merge(linksSelection);

    let tagsSelection = tagsGroup.selectAll("text")
        .data(graph.tags);

    let newTags = tagsSelection
        .enter().append("text")
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

    newTags.transition()
        .duration(3000)
        .attr("fill", "white");


    tags = newTags.merge(tagsSelection);


    simulation.alpha(1).restart();

    function dragStarted(d) {
        if (!d3.event.active) simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }

    function dragged(d) {
        d.fx = d3.event.x;
        d.fy = d3.event.y;
    }

    function dragEnded(d) {
        if (!d3.event.active) simulation.alphaTarget(0);
        d.fx = null;
        d.fy = null;
    }
}

function ticked() {
    links
        .attr("x1", d => d.source.x)
        .attr("y1", d => d.source.y)
        .attr("x2", d => d.target.x)
        .attr("y2", d => d.target.y);

    // node
    //     .attr("cx", d => d.x)
    //     .attr("cy", d => d.y);

    tags
        .attr("x", d => d.x)
        .attr("y", d => d.y)

}


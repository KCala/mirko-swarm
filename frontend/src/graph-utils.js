import * as d3 from 'd3';

export function resizeSvgToWindow(svg) {
    let contentDiv = d3.select("#content");
    let contentWidth = +contentDiv.style("width").slice(0, -2);
    let contentHeight = contentDiv.style("height").slice(0, -2);

    svg.attr('width', contentWidth).attr('height', contentHeight);
    return svg;
}

export function drawGraph(svg, graph) {
    let width = +svg.attr('width');
    let height = +svg.attr("height");

    let simulation = d3.forceSimulation()
        .force("link", d3.forceLink().id(d => d.id))
        .force("charge", d3.forceManyBody().strength(-200))
        .force("center", d3.forceCenter(width / 2, height / 2))
        .force("collision", d3.forceCollide(30));


    let link = svg.append("g")
        .attr("class", "links")
        .selectAll("line")
        .data(graph.links)
        .enter().append("line")
        .attr("stroke-width", function (d) {
            return Math.sqrt(d.value);
        });

    // let node = svg.append("g")
    //     .attr("class", "nodes")
    //     .selectAll("circle")
    //     .data(graph.nodes)
    //     .enter().append("circle")
    //     .attr("r", 5)
    //     .attr("fill", function (d) {
    //         return color(d.group);
    //     })
    //     .call(d3.drag()
    //         .on("start", dragstarted)
    //         .on("drag", dragged)
    //         .on("end", dragended));

    let text = svg.append("g")
        .attr("class", "labels")
        .selectAll("text")
        .data(graph.nodes)
        .enter().append("text")
        .attr("dx", 0)
        .attr("dy", 0)
        .style('font-size', d => Math.random() * 20 + 10 + 'px')
        .text(d => `#${d.id}`)
        .attr("text-anchor", "middle")
        .attr("nodeGroup", d => d.nodeGroup)
        .attr("fill", d => "white")
        .call(d3.drag()
            .on("start", dragstarted)
            .on("drag", dragged)
            .on("end", dragended));

    // node.append("title")
    //     .text(function (d) {
    //         return d.id;
    //     });


    simulation
        .nodes(graph.nodes)
        .on("tick", simulationToSvg);

    simulation.force("link")
        .links(graph.links);

    function simulationToSvg() {
        link
            .attr("x1", d => d.source.x)
            .attr("y1", d => d.source.y)
            .attr("x2", d => d.target.x)
            .attr("y2", d => d.target.y);

        // node
        //     .attr("cx", d => d.x)
        //     .attr("cy", d => d.y);

        text
            .attr("x", d => d.x)
            .attr("y", d => d.y)

    }

    function dragstarted(d) {
        if (!d3.event.active) simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }

    function dragged(d) {
        d.fx = d3.event.x;
        d.fy = d3.event.y;
    }

    function dragended(d) {
        if (!d3.event.active) simulation.alphaTarget(0);
        d.fx = null;
        d.fy = null;
    }
}
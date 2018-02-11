import './normalize.css'
import './main.css';
import * as d3 from 'd3';
import json from './graph.json';
import * as graphUtils from './graph-utils.js';

console.log("Hello mirko!");
let svg = d3.select("#content").select("svg");

graphUtils.resizeSvgToWindow(svg);
graphUtils.drawGraph(svg, json);

window.addEventListener('resize', ev => {
    svg.selectAll('*').remove();
    graphUtils.resizeSvgToWindow(svg);
    graphUtils.drawGraph(svg, json)
}, true);



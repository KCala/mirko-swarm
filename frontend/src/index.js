import './normalize.css'
import './main.css';
import * as d3 from 'd3';
// import json from './graph.json';
import {GraphRenderer} from './graph-renderer.js';
import {exampleGraph} from './graph-updater.js';

console.log("Hello mirko!");
let svg = d3.select("#content").append("svg");
const graphRenderer = new GraphRenderer(svg, exampleGraph);

// console.log(exampleGraph);
// let i = 0;
// setInterval(t => {
//     let contentDiv = d3.select("#content");
//     // console.log(exampleGraph.tags);
//     exampleGraph.tags.push({tag:`nowy${i}`, count: Math.random() * 10,
//         x:+contentDiv.style("width").slice(0, -2) / 2,
//         y: contentDiv.style("height").slice(0, -2) / 2});
//     exampleGraph.links.push({source: `nowy${i}`, target:"mirko", strength: 20});
//     graphRenderer.updateGraph(exampleGraph);
//     i++;
// }, 5000);

















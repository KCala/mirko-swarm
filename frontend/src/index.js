import './normalize.css'
import './main.css';
import * as d3 from 'd3';
// import json from './graph.json';
import {GraphRenderer} from './graph-renderer.js';
import {GraphUpdater} from './graph-updater.js';

console.log("Hello mirko!");
let svg = d3.select("#content").append("svg");


const graphUpdater = new GraphUpdater({
    tags: [],
    links: []
});
const graphRenderer = new GraphRenderer(svg, graphUpdater.graph);

graphUpdater.subscribeGraphToWSUpdates(graphRenderer.updateGraph.bind(graphRenderer));

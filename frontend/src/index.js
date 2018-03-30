import './normalize.css'
import './main.css';

import {Simulation} from './simulation.js';
import {BackendClient} from "./backend-client";
import {Renderer} from "./renderer";
import {Decorator} from "./decorator";

console.log("Hello mirko!");

const graph = {
    tags: [],
    links: []
};

const backendClient = new BackendClient(graph);
const simulation = new Simulation(backendClient.graph);
const renderer = new Renderer(graph);
const decorator = new Decorator(graph);

backendClient.startUpdatingGraphOnWebsocketMessages(simulation.updateGraph.bind(simulation));
simulation.startSimulation(() => decorator.decorateGraph.bind(decorator)());
renderer.start(simulation.simulation);
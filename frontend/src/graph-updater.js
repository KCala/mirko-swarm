import * as d3 from 'd3';

export class GraphUpdater {
    constructor(initialGraph) {
        this.graph = initialGraph;
    }

    subscribeGraphToWSUpdates(onUpdated) {
        console.log("Connecting to WS");
        this.socket = new WebSocket('ws://localhost:2137/api/v1/entries');
        this.socket.onmessage = msg => {
            console.log(msg.data);
            this.makeAlLTagsStale();
            let entry = JSON.parse(msg.data);
            this.handleEntry(entry);
            onUpdated();
        }
    }

    makeAlLTagsStale() {
        this.graph.tags.forEach(tag => tag.justUpdated = false);
    }

    handleEntry(entry) {
        entry.tags.forEach(tag => this.handleTag(tag, entry.authorsSex));
        this.handleLinks(entry.tags);
    }

    handleTag(tag, sex) {
        let existingTag = this.graph.tags.find(et => et.tag === tag);
        if (existingTag) {
            console.log(`Adding to existing tag ${tag}`);
            existingTag.count = existingTag.count + 1;
            existingTag.justUpdated = true;
            existingTag.lastUpdatedBySex = sex;
        } else {
            console.log(`Creating new tag ${tag}`);
            let newTag = {tag: tag, count: 1, justUpdated: true, lastUpdatedBySex: sex};
            this.graph.tags.push(newTag)
        }
    }

    handleLinks(tags) {
        console.log(`Tags length: ${tags.length}`);
        if (tags.length <= 1) {
            console.log("No links needed in this entry!")
        }
        else if (tags.length === 2) {
            this.handleLinkOneToOne(tags[0], tags[1])
        } else {
            this.handleLinksOneToMany(tags[0], tags.slice(1));
            this.handleLinks(tags.slice(1));
        }
    }

    handleLinksOneToMany(pivot, restOfTags) {
        restOfTags.forEach(tag => this.handleLinkOneToOne(pivot, tag))
    }

    handleLinkOneToOne(tagA, tagB) {
        let existingLink = this.findLinkBothDirections(tagA, tagB);
        if (existingLink) {
            existingLink.strength = existingLink.strength + 1;
            console.log(`Strengthening existing link ${tagA}--[${existingLink.strength}]--${tagB}`)
        } else {
            this.graph.links.push({
                source: tagA,
                target: tagB,
                strength: 1
            });
            console.log(`Linking new tags ${tagA}--[1]--${tagB}`)
        }
    }

    findLinkBothDirections(pointA, pointB) {
        this.graph.links.find(link =>
            link.source === pointA && link.target === pointB
            || link.source === pointB && link.target === pointA)
    }
}


// let contentDiv = d3.select("#content");
// x: +contentDiv.style("width").slice(0, -2) / 2,
//     y: contentDiv.style("height").slice(0, -2) / 2
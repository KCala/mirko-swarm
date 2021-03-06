export class BackendClient {
    constructor(initialGraph) {

            if(window.location.hostname === 'localhost') {
                this.wsAddress = `ws://localhost:2137/api/v1/entries`;
            } else {
                this.wsAddress =`ws://${window.location.hostname}:${window.location.port}/api/v1/entries`;
            }
        this.graph = initialGraph;
    }

    startUpdatingGraphOnWebsocketMessages(onUpdated) {
        console.log(`Connecting to WS at [${this.wsAddress}]...`);
        let that = this;
        this.socket = new WebSocket(this.wsAddress);
        this.socket.onopen = () => {
            console.log("Connected to WS!");
        };
        this.socket.onmessage = msg => {
            console.log(msg.data);
            let message = JSON.parse(msg.data);
            this.handleMessage(message);
            onUpdated();
        };
        this.socket.onclose = () => {
            console.error(`WS closed. Reconnecting in 10 seconds`);
            setTimeout(() => that.startUpdatingGraphOnWebsocketMessages(onUpdated), 10000)
        };
    }

    handleMessage(message) {
        if(message.error) {
            BackendClient.handleError(message)
        } else {
            this.handleEntry(message)
        }
    }

    handleEntry(entry) {
        entry.tags.forEach(tag => this.handleTag(tag, entry.authorsSex));
        this.handleLinks(entry.tags);
    }

    handleTag(tag, sex) {
        let existingTag = this.graph.tags.find(et => et.tag === tag);
        if (existingTag) {
            // console.log(`Adding to existing tag ${tag}`);
            existingTag.count = existingTag.count + 1;
            existingTag.fresh = true;
            existingTag.lastUpdatedBySex = sex;
            // console.log(`e!: ${JSON.stringify(existingTag)}`)
        } else {
            // console.log(`Creating new tag ${tag}`);
            let newTag = {tag: tag, count: 1, fresh: true, lastUpdatedBySex: sex};
            // console.log(`n!: ${JSON.stringify(newTag)}`);
            this.graph.tags.push(newTag)
        }
    }

    handleLinks(tags) {
        if (tags.length <= 1) {
            // console.log("No links needed in this entry!")
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
            // console.log(`Strengthening existing link ${tagA}--[${existingLink.strength}]--${tagB}`)
        } else {
            this.graph.links.push({
                source: tagA,
                target: tagB,
                strength: 1
            });
            // console.log(`Linking new tags ${tagA}--[1]--${tagB}`)
        }
    }

    findLinkBothDirections(pointA, pointB) {
        this.graph.links.find(link =>
            link.source === pointA && link.target === pointB
            || link.source === pointB && link.target === pointA)
    }

    static handleError(error) {
        console.log(`Error on backend! [${error.error}]`)
    }
}
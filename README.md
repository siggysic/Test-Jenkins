# Microservices Foundation (MSF)

## Overview

### Scope

- Explains the benefits/features of the Microservices Foundation
- List of available tutorials and guides and recommend the order

The template could be found at this private repository: https://github.com/dotography-code/LiftMicroservices

### Why Use the Template?

1. Faster setup and development - Instead of creating a build from an empty Lift template and copying code and configurations from a previous project, just clone the Microservice template and get started doing the interesting things right away.
2. Ease of deployment - The template includes script that will help deployment, including docker-izing and clustering. That's one less thing to worry about.
3. Premade integrations - Microservices need more tools than the usual application, such as centralized logging and monitoring. The template helps with that concern transparently.

### List of available tutorials
Recommended reading order for the [guides/tutorials](docs/)

1. [Microservices Developer Guide](docs/Lift MicroService Template - Developer Guide.docx) – Style guide and best practices for developing a Scala project
2. [Microservices Setup Guide](docs/Lift MicroService Template - setup guide.docx) – Setting up the template for further development
3. [Microservices Authentication with JWT Tutorial](docs/Microservices Authentication with JWT Tutorial.zip) – JWT explanation, and how to implement and configure authentication
4. [Microservices Authorization with Kibali Tutorial](docs/Microservices Authorization with Kibali Tutorial.zip) – How to do authentication with Kibali microservice, and integrate with Lift's authentication system
5. [Microservices Database Tutorial](docs/Lift Microservice Template - Database guide.docx) – Guide to implementing Database layer in various implementations
6. [Microservices Continuous Deployment Tutorial](docs/Microservices Continuous Deployment Tutorial.docx) – Setup automated processes with Jenkins
7. [Microservices Deployment and Scaling Tutorial](docs/Microservices Deployment and Scaling Tutorial.docx) – Different deployment techniques, using the template's scripts for deploying.
8. [Microservices Monitoring Tutorial](docs/Microservices Monitoring Tutorial.zip) – Guide to connect the monitoring tool in this version using [AWS CloudWatch](https://aws.
amazon.com/cloudwatch/)


# LiftMicroservices

## Purpose
This project is a proof of concept for deploying Lift based microservices to a Docker cluster.  There are 2 primary goals, each of which involves several tasks:

**Goal 1 - Development**
The end product of this goal should include:
 - A standard way to define stateless REST APIs using Lift
 - A sample API
 - Proof of concept unified logging for API calls
 - Proof of concept unified metrics collection for API calls
 
**Goal 2 - Deployment**
The end product of this goal should include:
 - A sample cluster environment
 - A process for building and deploying finalized apps
 - A way to load balance over microservice instance
 - A way to health check microservice instances and remove them when necessary

## Development

###  Prerequisites

In order work with this project, at a minimum, you must have docker installed on your machine. See the instructions at https://docs.docker.com/engine/installation/.  Note that if Docker is used outside of Linux you will also need to install, launch and properly configure docker-machine.  A getting started guide for docker-machine is available at https://docs.docker.com/machine/get-started/.  When using docker-machine port mappings are made to the docker-machine VM rather than directly to the host, therefore services much be accessed at the VMs IP address (returned by `docker-machine ip`) or the relevant docker-machine ports have to be mapped to the host. 

### Project Structure

The LiftMicroservices project consists of 2 parts

1. A Github repository containing a template for stateless Lift microservices.  The template contains a sample service that illustrates how to add endpoints, publish metrics, and perform unified logging. 
  
2. A shell script named `lm` that automates the initialization of the app inside a local Docker container.  Most importantly, the script will initialize the container so that it mounts ./target/webapp in the appropriate location so that it can be served by Jetty.  The shell script offers options that aid in setting up a development environment with features like debugger support.

### Getting Started with development

1. Clone the template.

2. Customize the template by changing the values of `name` in build.sbt, `couldwatch.namespace` in relevant properties files (default.props, production.props, etc)
 
3. Initialize the local docker instance by issuing `./lm init -n {service_name}` from the project root.  Available options during this process are -p {http_port} (defaults to 8080) and -d {debug_port} (defaults to no remote debugging).

4. Use the SBT `webappPrepare` command to recompile changes and assemble an exploded war file in ./target/webapp

5. Start the docker instance with `docker start {service_name}`.

6. Update the docker instance after a `webappPrepare` call with `docker stop {service_name}` followed by `docker start {service_name}.

### API

The project contains an API singleton object that allows for registration of one or more "modules".  Each module is registered with a prefix string and a request to /api/{prefix_string} will result in the prefix being stripped from the request and the module being invoked with the remaining path.  As an example, there is a Test module contained in the project.  In the Boot class the Test module is registered with the API via
```
API.register("test", Test)
```
The API itself is registered with Lift for dispatch during stateless request processing, at which point any request to /api/test will be routed to the Test module.  The Test module provides several endpoints for testing which are documented within the class itself.

### Logging
Lift has a mechanism for dynamically configuring the either Log4J or Logback depending on the runtime environment.  More information can be found at https://www.assembla.com/wiki/show/liftweb/Logging.  In this project, we build on that ability by choosing Logback and utilizing an appender that can publish events from multiple services deployed across a cluster of machines to a single repository.  For the purposes of this POC, we've chosen Papertrail as the repository and the configuration that accomplishes logging to the service can be found in `./src/main/resources/default.logback.xml`.  API calls that are registred with the API singleton will have their request times, along with whether a request resulted in a success or failure, logged.

### Metrics
Lift has no built in method for abstracting metrics reporting, but the Dropwizard Metrics app is a popular abstraction for JVM applications (https://dropwizard.github.io/metrics/3.1.0/getting-started/).  This template includes a Metrics singleton object that allows for arbitrary metrics collection, and is configured to report metrics to Amazon Cloudwatch at 1 min intervals. API calls that are registred with the API singleton will have their request times, success counts and error counts reported along with cloudwatch dimensions indicating the path accessed and the host that the request was directed to.  While the Dropwizard Metrics library offers some important abstractions, different backends do support different types of metrics and often require specific naming conventions.  In particular, this template uses a convention of including dimensions as name=value pairs and utilizing a trailing \* so that metrics will be available both broken down by dimension and in aggregate.  More information is available at the Github page for the adapter https://github.com/blacklocus/metrics-cloudwatch.
 
## Deployment
This project is designed a a stateless web service, with the intent that multiple instances can be deployed to a cluster in production.  To demonstrate this ability, the `./lm` script offers command that provides for creation of a sample cluster of VirtualBox virtual machines, creation of a finalized docker image containing the app, pushing the image to a central registry, deploying multiple instances of the app on the cluster, and updating the running instance using a blue/green deployment strategy

### Concepts
There are several components that participate in a clustered deployment:

- *Cluster Nodes* - To simulate a cluster locally, we use `docker-machine` to create VMs.  This results in a VirtualBox VM for each invocation with the appropriate docker libraries pre-installed
- *Cluster Federation* - Deploying to each node within a cluster would be a tedious process.  Instead we use Docker Swarm to federate the available nodes together.  Once the Swarm cluster is created, instead of deploying the app to each node individually, we will deploy the app to a single location and the Swarm manager will act as a scheduler, deciding where each instance should be run
- *Service Discovery* - A service discovery mechanism is necessary both for proper function of Swarm and to track deployments of our app, thereby announcing it's availability to the load balancer.  For this, we use the combination of Consul and Registrator.  Consul is a popular distributed service registry that also acts as a key value store.  Registrator is an application that watches for the deployment of Docker containers and automatically registers their availability with Consul.  Consul and Registrator are installed to each node during our setup process.
- *A Docker Registry* - A requirement of Swarm is that any Docker image that is deployed must exist in a central repository that each Swarm node can pull from.  We launch a separate VM to act as our registry.  Because the registry runs Consul, and Consul can act as a DNS server, this also gives us a common mechanism for VMs to communicate with each other
- *A Front End* - We'll have multiple instances of our application running, but they should all appear to be a single instance to consumers.  HAProxy acts as a front end to accomplish this.  As application instances are deployed, they are registered with Consul via Registrator.  The Docker container running HAProxy watches for changes to registration using a tool called Consul-Template.  Changes automatically trigger an update of the HAProxy configuration file and result in zero downtime HAProxy reloads.  Consul-Template also monitors a special app/active key within Consul that specifies whether HAProxy is currently serving the "blue" or the "green" backend at port 80.  The "inactive" backend is served at port 8080.

### Use

1. Create a sample cluster.  Issue the command `./lm swarm init` from the project root.  This will:
    1. Create a node to act as the Docker registry and install a docker registry container to it (note, a self signed SSL certificate will be created the first time this is done, which requires OpenSSL to be installed on the host machine)
    2. Create a node that will act as the Swarm manager
    3. Create two more Swarm nodes for deployment of the application
    4. Deploy the HAProxy front end to the Swarm manager node, building it first if necessary 
2. Now that we've got a cluster running, lets deploy something to it.  Deploying the app consists of 2 steps.
    1. We need to build the WAR, package it as a Docker container, and push it to our registry.  This is accomplished with the command `./lm swarm publish -n app -v 1.0.0`.  The -v flag defines the version of the app, and we'll increment it each time we publish a new image.  The -n flag defines the name of the app, and for the purposes of this sample cluster, it should always be `app`.
    2. We need to deploy our newly built container to the Swarm cluster.  This is accomplished with the command `./lm swarm deploy -n app -v 1.0.0 -g blue -c 2`.  The -n and -v flags should both correspond to the values we specified in the previous step as they specify that we'll be deploying the image we just published.  The -g flag specifies the "group" that the app will be deployed to and the value should be one of "blue" or "green".  The -c flag specifies the count, or how many instances of the app we'll be deploying.  We created the Swarm cluster with 2 nodes, so we specify 2 here, one for each node.
3. Our app should be up, time to test it.  The HAProxy front end was explicitly deployed to the manager node, so the first thing we need to do is find out what the IP address for that node is by typing `docker-machine ip manager` at the command line.  Once we've determined the IP address, we can access the app in a web browser using the URL http://[manager_ip]/test/id.  The id API method returns a JSON object that contains some interesting information that will help us validate that everything is working as expected.  First off, the "name", "version" and "group" keys should all correspond to the values we specified when we executed our deploy command.  In addition, there is a "uuid" key whose value is unique for the app instance the request was routed to.  If we refresh the page, we should see that value cycles between two distinct IDs.  We created 2 instances of our app and HAProxy is doing a round robin load balance between them, so this is what we'd expect!
4. Next, it's time to upgrade our app.  Blue / Green deployments are a strategy that involves bringing up a new version in parallel to the existing deployed app.  As we saw before, you specify a "group" of "blue" or "green" when performing a deployment.  We previously deployed to the blue group, which is the default active group when a cluster is initialized.  Now we'll deploy a new version to the green group.
    1. Build the new version with `./lm swarm publish -n app -v 1.0.1`.  **Note the version change!**
    2. Deploy two instances of the new app with `./lm swarm deploy -n app -v 1.0.1 -g green -c 2`
5. At this point, we've deployed to the green group, but we haven't activated it, so the blue group is still responding at the standard HTTP port 80.  Before we change that, we want to validate that our new instances deployed correctly.  To allow this, HAProxy is configured as a front end for the non-active group at port 8080.  Loading http://[manager_ip]:8080/test/id in a web browser should display a JSON object indicating that the app we are accessing is version 1.0.1.
6. Once we've validated that our new version is working correctly, it's time to make it active.  We can accomplish that be running `./lm swarm activate green` at the command line.  This will update the value of the app/active key in consul, which will trigger a configuration update and a reload of HAProxy.  Now http://[manager_ip]/test/id should report version 1.0.1, and access on port 8080 should report version 1.0.0.  The two versions have switched places.
7. With a new version properly deployed, it wouldn't make sense to leave the old version running and taking up resources.  We can remove it with `./lm swarm undeploy blue`.  This will cause all running instances in the blue group to be stopped and removed.  The blue group is now available for deployment of our next version!

If you forget which group is currently active, you can run `./lm swarm active` and the currently active group will be reported in your terminal.

### Other useful info

- You might want to run some docker commands against the swarm cluster.  Executing `eval "$(./lm swarm env)"` at the command line will set up the environment to allow that.  Once that's done, you can issue commands like `docker ps` and you'll see running containers across the whole cluster.
- Consul relies on a quorum of nodes to function properly.  In a production environment that consists of many servers that are consistently available, this isn't a problem.  When we're testing on a laptop though, having the machine sleep or restart can  cause the nodes to lose communication with each other and throw the consul cluster into chaos.  When a quorum is lost, the Consul cluster can't be repaired without manual intervention.  You can use the `docker logs consul` command to view the status of consul on each node, and if you see lots of messages about the nodes being unable to select a leader, the easiest thing is to destroy the whole cluster with `./lm swarm destroy` and then recreate it with `./lm swarm init`.
- On a laptop, be conscious of resource usage.  Each docker-machine VM will use about 1G of RAM.  Each will also only have about 1G of RAM available to processes running within it.  Deploying too many nodes (new nodes can be deployed with `./lm swarm create [nodename]` or deploying too many running containers to a single node will lead to problems.

### Next Steps

All of these concepts should transfer to a cluster regardless of whether it used physical servers or cloud resources.  Next steps could involve extending this process to include health checking, and automating the deployment process using a continuous deployment tool like Jenkins.



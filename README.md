# TRISTAN Cloud Connector

![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)
![GitHub issues](https://img.shields.io/github/issues/aicas/tristan-automotive-gateway)
![GitHub forks](https://img.shields.io/github/forks/aicas/tristan-automotive-gateway)
![GitHub stars](https://img.shields.io/github/stars/aicas/tristan-automotive-gateway)
![GitHub Workflow Status](https://img.shields.io/github/workflow/status/aicas/tristan-automotive-gateway/actions/workflows/badge.svg)
![Documentation Status](https://readthedocs.org/projects/tristan-automotive-gateway/badge/?version=latest)

Tristan Cloud Connector is an OSGi bundle developed
by [aicas](https://www.aicas.com) as part of the contributions to
the [EU-funded project TRISTAN](https://cordis.europa.eu/project/id/101095947).
The bundle connects to the Edge Data Gateway (**aicas EDG**) to manage
automotive data streams. This bundle is designed to run within an OSGi
container, such as **JamaicaAMS** and **Apache Felix**, but can also be packaged
as a fat JAR for standalone execution.

## About TRISTAN

TRISTAN (Together for RISC-V Technology and ApplicatioNs) is an EU-funded
initiative aimed at accelerating the adoption of the RISC-V chip architecture
across Europe. By fostering an open-source ecosystem, TRISTAN seeks to bolster
competitiveness and spur innovation in the design of integrated circuits. The
project is set to enhance the RISC-V architecture to make it a viable
alternative to existing commercial options, with a focus on improving
productivity, security, and transparency through a unified processor
architecture. TRISTAN's holistic approach encompasses advancements in electronic
design automation tools and the entire software stack, promising a significant
leap forward in open-source hardware and software utilization.

## Table of Contents

- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Building the Project](#building-the-project)
- [Running the Bundle in Apache Felix](#running-the-bundle-in-apache-felix)
- [Running the Fat JAR](#running-the-fat-jar)
- [Running the Bundle in JamaicaAMS](#running-the-bundle-in-jamaicaams)
- [Contributing](#contributing)
- [License](#license)
- [Copyright](#copyright)

## Project Structure

This project is structured as follows:

- **cloud-connector**: The module that builds the Cloud Connector OSGi bundle
  and fat JAR. An OSGi bundle connects to MQTT servers and publishes device
  telemetry data. This application facilitates connections to MQTT servers and
  publishes device telemetry data. Designed for versatility and ease of use, it
  supports multiple devices via a configurable JSON file and can be deployed
  both as a standalone Java application and as an OSGi bundle in an OSGi-based
  framework.

## Prerequisites

Before you can build and run this project, ensure you have the following
installed:

- **Java 8** or later (Java 8 is recommended for OSGi compatibility especially
  when you run bundles on JamaicaAMS).
- **Maven 3.9.6** or later.
- **JamaicaAMS** Realtime OSGi framework developed by aicas.
- **Apache Felix** OSGi
  framework ([Download Felix](https://felix.apache.org/documentation/downloads.html)).
- **aicas EDG** Realtime Data Visualization developed by aicas.

> **_NOTE:_**  
> Please contact aicas ([info@aicas.com](info@aicas.com)) for **JamaicaAMS** and
**EDG** to have full experiences of this project.

## Building the Project

To build the OSGi bundle and fat JAR, run the following Maven commands:

```bash
mvn clean package
```

This will generate two artifacts under `target/`:

- cloud-connector-1.0.0-SNAPSHOT.jar: The OSGi bundle.
- cloud-connector-1.0.0-SNAPSHOT-jar-with-dependencies.jar: The fat JAR with all
  dependencies.

## Running the Bundle in Apache Felix

### Step 1: Download and Install Apache Felix

Download [Apache Felix](https://felix.apache.org/documentation/downloads.html)
from the official website and extract it to a directory of your choice.

### Step 2: Start Felix

Run Apache Felix from the command line:

```bash
java -Dedg.device.token=<device-token> -jar bin/felix.jar
```

#### Configuring System Properties

You can set system properties in Felix that can be accessed in the OSGi bundle
using `System.getProperty()`. There are two ways to configure system properties:

##### Option 1: Command Line

Pass the properties directly when starting Felix:

```bash
java -Dedg.device.token=<device-token> -jar bin/felix.jar
```

##### Option 2: Felix config.properties

Edit Felix’s conf/config.properties file to set system properties:

```bash
edg.device.token=<device-token>
```

### Step 3: Install and Start the Bundle

Once Felix is running, use the Felix Gogo shell to install and start the OSGi
bundle.

```bash
g! install file:/path/to/cloud-connector-1.0.0-SNAPSHOT.jar 
g! start <bundle-id>
```

Replace <bundle-id> with the actual ID of the installed bundle (you can get this
from the `g! list` command).

### Step 4: Verify the Connection

The bundle will attempt to connect to the MQTT broker using the configuration in
config.json. You should see logs indicating a successful connection.

## Running the Fat JAR

Alternatively, you can run the fat JAR outside the OSGi container:

```bash
java -jar /path/to/cloud-connector-1.0.0-SNAPSHOT-jar-with-dependencies.jar --token=<device-token> 
```

You can get usage

```bash
java -jar /path/to/cloud-connector-1.0.0-SNAPSHOT-jar-with-dependencies.jar --help 
```

## Running the Bundle in JamaicaAMS

**JamaicaAMS** (Application Management System, AMS) is a modular and extensible
application framework, especially designed and tailored for Industrial IoT use
cases. It provides a powerful runtime environment for Java-based applications
and components, thereby supporting not only static but also highly dynamic and
distributed application scenarios. JamaicaAMS targets in particular at
heterogeneous embedded and mobile devices with sparse resources, providing
performance guarantees for their applications during runtime. The strength of
JamaicaAMS results from the combination of two solid open standards: OSGi , that
specifies a software architecture to create modular applications and services,
called bundles, and the Real-Time Specification for Java (RTSJ). Please contact
[aicas](info@aicas.com) to have an evaluation version of JamaicaAMS.

## Contributing

Contributions are welcome! Please feel free to submit a pull request or create
an issue for bugs, questions, or new features.

## License

This project is licensed under the GNU General Public License v3.0 - see the
LICENSE file for details. The GPLv3 is a free, copyleft license for software and
other kinds of works, ensuring the freedom to use, modify, and distribute the
software.

## Copyright

Copyright 2024, aicas GmbH; all rights reserved.

# TRISTAN Cloud Connector

   ```
   [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
   [![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)](https://conventionalcommits.org)
   [![GitHub issues](https://img.shields.io/github/issues/aicas/tristan-automotive-gateway)](https://github.com/aicas/tristan-automotive-gateway/issues)
   [![GitHub forks](https://img.shields.io/github/forks/aicas/tristan-automotive-gateway)](https://github.com/aicas/tristan-automotive-gateway/network)
   [![GitHub stars](https://img.shields.io/github/stars/aicas/tristan-automotive-gateway)](https://github.com/aicas/tristan-automotive-gateway/stargazers)
   [![GitHub Workflow Status](https://img.shields.io/github/workflow/status/aicas/tristan-automotive-gateway/actions/workflows/badge.svg)](https://github.com/aicas/tristan-automotive-gateway/actions/workflows/ci.yml)
   [![Documentation Status](https://readthedocs.org/projects/tristan-automotive-gateway/badge/?version=latest)](https://tristan.readthedocs.io/en/latest/?badge=latest)
   ```

CloudConnector is a Java application developed by aicas (www.aicas.com) as part
of the contributions to the EU-funded project
TRISTAN (https://cordis.europa.eu/project/id/101095947). It connects to MQTT
servers and publishes device telemetry data. This application facilitates
connections to MQTT servers and publishes device telemetry data. Designed for
versatility and ease of use, it supports multiple devices via a configurable
JSON file and can be deployed both as a standalone Java application and as an
OSGi bundle in an OSGi-based framework.

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

## Features

- Connects to MQTT servers using device-specific tokens.
- Publishes telemetry data from configurable trace files.
- Supports multiple devices through a single configuration file.
- Can be deployed as a bundle in OSGi-based frameworks.
- Integrates with GitHub Actions for CI/CD, including code quality checks and
  artifact publishing.

## Getting Started

### Prerequisites

- JDK 8
- Maven 3.6.0 or later

### Installation

1. Clone the repository:

   ```sh
   git clone https://github.com/aicas/CloudConnector.git
   cd CloudConnector
   ```

2. Build the project with Maven:

    ```sh
    mvn clean package
    ```

### Configuration

Modify the config.json file in the root directory to set up your device
configurations, including the MQTT server URI, device tokens, and trace files.

Example config.json:

   ```json
   {
     "edp.server.uri": "tcp://demo-jamaicaedg.aicas.com:1883",
     "devices": [
       {
         "name": "device1",
         "token": "token1",
         "trace": "automotive-trace.json",
         "delay": 500
       },
       {
         "name": "device2",
         "token": "token2",
         "trace": "forklift-trace.json",
         "delay": 1000
       }
     ]
   }
   ```

### Usage

#### As a Standalone Java Application:

Run the application using:

   ```sh
   java -jar target/cloud-connector-1.0.0-SNAPSHOT.jar
   ```

#### As an OSGi bundle:

To deploy CloudConnector as a bundle in an OSGi-based framework (e.g.,
JamaicaAMS, Apache Felix, Eclipse Equinox), ensure the JAR built is OSGi
compatible. Then, follow your OSGi container's instructions for adding a bundle,
typically involving placing the JAR in a `bundle` directory or installing it
through the container's command line interface.

JamaicaAMS (Application Management System, AMS) is a modular and extensible
application framework, especially designed and tailored for Industrial IoT use
cases. It provides a powerful runtime environment for Java-based applications
and components, thereby supporting not only static but also highly dynamic and
distributed application scenarios.  JamaicaAMS targets in particular at
heterogeneous embedded and mobile devices with sparse re- sources, providing
performance guarantees for their applications during runtime. The strength of
JamaicaAMS results from the combination of two solid open standards: OSGi , that
specifies a software architecture to create modular applications and services,
called bundles, and the Real-Time Specification for Java (RTSJ). Please contact
[aicas](info@aicas.com) to have an evaluation version of JamaicaAMS.


## GitHub Actions CI/CD

This project is configured with GitHub Actions for continuous integration and
deployment, including:

- Building and testing the Java application.
- Performing code quality checks with SonarCloud.
- Publishing artifacts to Maven Central.

## Contributing

Contributions are welcome! Please feel free to submit a pull request or create
an issue for bugs, questions, or new features.

## License

This project is licensed under the GNU General Public License v3.0 - see the
LICENSE file for details. The GPLv3 is a free, copyleft license for software and
other kinds of works, ensuring the freedom to use, modify, and distribute the
software.

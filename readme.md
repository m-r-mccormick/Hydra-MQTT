# Hydra-MQTT

| <img src="./docs/src/assets/hydra.png" alt="description" style="width: 300px"> | Hydra-MQTT is a free module for [Inductive Automation Ignition](https://inductiveautomation.com/) that facilitates the utilization of non-Sparkplug MQTT. |
|--------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|

| Branch                   | Actions                                                                                                                                                                                                                | Download                                                                                                                                                                                          | Documentation                                                                                                                 |
|:-------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------|
| Release (main)           | [![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/m-r-mccormick/Hydra-MQTT/build.yml?branch=main&label=main)](https://github.com/m-r-mccormick/Hydra-MQTT/releases/latest)      | [![GitHub Release](https://img.shields.io/github/v/release/m-r-mccormick/Hydra-MQTT?display_name=tag&label=release)](https://github.com/m-r-mccormick/Hydra-MQTT/releases/latest)                 | [![Netlify](https://img.shields.io/netlify/1c33c3b7-a57e-4cf6-8fc8-32b77d68c1c5)](https://hydra-mqtt.netlify.app/)            |
| Pre-Release (prerelease) | [![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/m-r-mccormick/Hydra-MQTT/build.yml?branch=prerelease&label=prerelease)](https://github.com/m-r-mccormick/Hydra-MQTT/releases) | [![GitHub Release](https://img.shields.io/github/v/release/m-r-mccormick/Hydra-MQTT?include_prereleases&display_name=tag&label=prerelease)](https://github.com/m-r-mccormick/Hydra-MQTT/releases) | [![Netlify](https://img.shields.io/netlify/83a20d24-3d72-4704-afe8-9f12417d746c)](https://hydra-mqtt-prerelease.netlify.app/) |


## Documentation

- [Release Documentation](https://hydra-mqtt.netlify.app/)
- [Pre-Release Documentation](https://hydra-mqtt-prerelease.netlify.app/)


## Build

On [Debian 12](https://www.debian.org/):
```bash
sudo apt-get update -q
sudo apt-get install -y -q git make default-jdk maven
git clone https://github.com/m-r-mccormick/Hydra-MQTT.git
cd Hydra-MQTT
make
```

When prompted, enter the `Certificate Signer Name` to be used for signing the module using a self-signed certificate.


## Academic Citations

If utilizing this module to support an academic publication, please cite [this](http://dx.doi.org/10.13140/RG.2.2.25803.60967) paper.

```bibtex
@article{mccormick-2024-real-time-manufacturing,
    author = "McCormick, M. R. and Wuest, Thorsten",
    title = "Real-Time Manufacturing Datasets: An Event Sourcing Approach",
    year = "2024",
    month = "07",
    doi = "10.13140/RG.2.2.25803.60967",
    url = "http://dx.doi.org/10.13140/RG.2.2.25803.60967",
}
```

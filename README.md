# axon-extension-dbscheduler

This project is an Axon framework extension that provides support of dbscheduler for deadlines and event scheduling.

## Motivation

Axon framework is a powerful framework of building event-driven systems. By default, it does not contain any other 
implementation of deadline / event scheduling than quartz and in memory one. This project simply adds support 
of dbscheduler as main scheduler to the framework. 

### Features

* db scheduler deadline manager
* spring autoconfiguration for spring boot enabled projects


### Work progress

Currently, project was tested only manually using simple axon example project.

Things that need to be done:

- event scheduler
- tests
- build tool
- more debug logs
- build.gradle version management
- artifact publication

## License

**axon-extension-dbscheduler** is published under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
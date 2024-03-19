Zeebe Simple Tasklist
=========================
## Install
### Docker
1. start docker image
    ```
    cd docker
    docker-compose --profile in-memory up
    ```
1. go to http://localhost:8081
1. login with `demo/demo`

### IntelliJ
1. start zeebe_broker container
1. Run `ZeebeSimpleTasklistApp.java`
1. Go to http://localhost:8081
1. Login with `demo/demo`

## Use
1. Open **Camunda Modeler**
2. Open `bpmn/simple-approve.bpmn` in Camunda Modeler
3. Click **Deploy Current Diagram** button (rocket icon on left bottom corner)
4. Click *Start current diagram* button 


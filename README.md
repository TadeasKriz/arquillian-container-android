arquillian-container-android
============================

arquillian-container-android extension is a logical continuation of Arquillan Drone and Arquillian Android extensions.

The major portion of the code is the same, but underlying concept is different. Arquillian container implements the 
deployable container SPI and the whole run of the container as such follows just the same pattern as the all other 
containers for Arquillian (managed ones). There are no extensions which are hooked to the execution process.

After completition, the user just codes deployment logic against well defined SPI, Arquillian Drone and Android will be 
merged together.

The main advantage of container approach is to be able to let construct the archive for the deployment on ShrinkWrap. 
The other advantage is to be able to create more then one Android device (physicall, virtual or both) and to test 
scenarios where more then one mobile device is involved, e.g. testing their communication. 

A lot of bugs is repaired, e.g. stopping of AVD device is done propperly, generation of AVD is done dynamically in 
the case there is no such AVD present in the system (and it is deleted afterwards). The code is more robust and reformatted. 
The internal logic is more concise.

The code is under very heavy development and it is not production ready at all. Deployment and enrichment of the archive 
(e.g. constructed by ShrinkWrap) is not done and the central part of the deployment process is missing. 

set schema 'sys_boot.sys_boot';

create jar firewater_plugin
library 'file:${FARRAGO_HOME}/plugin/firewater.jar'
options(0);

alter system add catalog jar firewater_plugin;

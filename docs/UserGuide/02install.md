
# Installation

Unpack zorka archive into application server home directory (ex. `/opt/tomcat-7.0.29`).
Zorka files will reside in `<server-home>/zorka` directory.  Unpacked directory contains
the following things:


* `zorka.jar` - agent jar file (with all dependencies included);

* `conf/*.bsh` - extension scripts;

* `log/*.log` - log files;

* `home.*` - sample home directories -  (containing predefined `zorka.properties+conf/*.bsh` for various application
servers);

* `zorka.properties` - main configuration file;


## Application server configuration

Zorka works as java agent, thus the following thing has to be added to java command line:

    -javaagent:/path/to/zorka/agent.jar=/path/to/zorka

Also, some specific things may have to be added for specific application servers. In all examples below `<server-home>`
means application server home (eg. `/opt/jboss-4.2`) and `<zorka-home>` means directory with unpacked zorka files
(eg. `/opt/jboss-4.2/zorka`).

Some application servers have been (preliminarily) tested and are described below but for other servers should work as
well as long as application server does not perform too many wizardry on platform mbean server or class loaders.



### Tomcat 6.x, 7.x

Only Sun JDK 6/5 is supported with Tomcat at the moment:

* copy `config/zabbix/tomcat.bsh` files to `<zorka-home>/conf` directory (overwrite existing files if necessary);

* edit `<server-home>/bin/catalina.conf` and add the following line at the end:


    CATALINA_OPTS="$CATALINA_OPTS -javaagent:<zorka-home>/agent.jar=<zorka-home>"

* adjust zorka.properties if necessary (log files, listen port number etc.);


### Jboss 4.x, 5.x, 6.x

Only Sun JDK 5 and 6 are supported:

* copy `config/zabbix/jboss.bsh` files to `<zorka-home>/conf` directory;

* edit `<server-home>/bin/run.conf` and add the following line at the end:


    JAVA_OPTS="$JAVA_OPTS -javaagent:<zorka-home>/agent.jar=<zorka-home>"

* adjust zorka.properties if necessary (log files, listen port number etc.);

* import `Template_Zorka_JBoss_*.xml` templates into Zabbix;



### JBoss 7.x

JBoss 7 can run in two modes: standalone and domain, so there are two different configurations for both modes.

Templates from `Template_Zorka_JBoss7_*.xml` files fit for various subsystems of JBoss 7.


#### Standalone mode

Following configuration steps have been tested in standalone mode but should work the same in both modes
(edit `domain.conf` instead of `standalone.conf`).

* copy `config/zabbix/scripts/jboss7.bsh` script to `<zorka-home>/conf` directory;

* make sure that `zorka.mbs.autoregister = no` is set in `zorka.properties` file;

* edit `<server-home>/bin/standalone.conf` and add the following line:


    JAVA_OPTS="$JAVA_OPTS -javaagent:<zorka-home>/agent.jar=<zorka-home>"


* add zorka bootstrap package to JBoss system modules - find and change the setting in `standalone.conf` file:


    if [ "x$JBOSS_MODULES_SYSTEM_PKGS" = "x" ]; then
       JBOSS_MODULES_SYSTEM_PKGS="org.jboss.byteman,com.jitlogic.zorka.core.spy"
    fi


#### Domain mode

In domain mode administrator needs to create separate zorka home directory for every server VM. Each agent instance has
to listen on separate port(s) and use separate log/trace files.

* copy `config/zabbix/scripts/jboss7.bsh` script to `<zorka-home>/conf` directory;

* make sure that `zorka.mbs.autoregister = no` is set in `zorka.properties` file;

* edit `<server-home>/bin/standalone.conf` and add the following line:

* add zorka bootstrap package to JBoss system modules - find and change the setting in `domain.conf` file:


    if [ "x$JBOSS_MODULES_SYSTEM_PKGS" = "x" ]; then
       JBOSS_MODULES_SYSTEM_PKGS="org.jboss.byteman,com.jitlogic.zorka.core.spy"
    fi

* add java agent option to default JVM configuration in `host.xml`:


    <jvms>
        <jvm name="default">
            <jvm-options>
                <option value="-server"/>
                <option value="-javaagent:<zorka-root>/zorka.jar=<zorka-root>/zorka"/>
            </jvm-options>
        </jvm>
    </jvms>


* add `zorka.home.dir` system property to each server JVM:


    <servers>
        <server name="server-one" group="main-server-group" auto-start="true">
            <system-properties>
                <property name="zorka.home.dir" value="<zorka-root>/server1"/>
            </system-properties>
        </server>
        <server name="server-two" group="main-server-group" auto-start="true">
            <system-properties>
                <property name="zorka.home.dir" value="<zorka-root>/server2"/>
            </system-properties>
            <socket-bindings port-offset="150"/>
        </server>
        <server name="server-three" group="other-server-group" auto-start="false">
            <socket-bindings port-offset="250"/>
        </server>
    </servers>


### Mule ESB

Zorka has been tested with Mule ESB 3.3.0 working on Sun JDK 6 on Linux. Mule ESB uses Tanuki Software wrapper
configured via `$MULE_CONF/conf/wrapper.conf` file. In order to configure zorka:

* unpack zorka to `$MULE_HOME`;

* edit `wrapper.conf` file and add java agent to java arguments:


    wrapper.java.additional.8=-javaagent:$MULE_HOME/zorka/agent.jar=$MULE_HOME/zorka
    wrapper.java.additional.8.stripquotes=TRUE

* make sure that `zorka.mbs.autoregister = yes` is set in `zorka.properties` file;

* import `Template_Zorka_Mule*.xml` templates into Zabbix;


### Apache ActiveMQ

Zorka has been tested with ActiveMQ 5.6.0 working on SunJDK6 on Linux. Wrapper has its configuration files in
`$MQ_HOME/bin/linux-*` directories - so you have to modify one of `wrapper.conf` files  - depending on what
architecture are you running for:

* edit wrapper.conf file and add java agent to java arguments (adjust option number):


    wrapper.java.additional.12=-javaagent:%ACTIVEMQ_HOME%/zorka/agent.jar=%ACTIVEMQ_HOME%/zorka

* make sure that `zorka.mbs.autoregister = yes` is set in `zorka.properties` file;

* import `Template_Zorka_ActiveMQ*.xml` templates into Zabbix;


## Zorka Viewer

Zorka viewer is useful for analyzing data produced by Zorka Tracer. Viewer is standalone executable jar with swing GUI.

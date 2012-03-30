
all: DungeonBuilder.jar

DungeonBuilder.jar: *.java plugin.yml CommandMap.xml
	javac -d . -Xlint:unchecked -cp "server/lib/craftbukkit-1.2.4-R1.0.jar:server/plugins/Permissions/Permissions.jar:server/lib/Heroes.jar:." -Xlint:deprecation *.java
	jar -cf DungeonBuilder.jar net plugin.yml com CommandMap.xml BlockPriority.xml

clean:
	rm -f DungeonBuilder.jar
	rm -rf net

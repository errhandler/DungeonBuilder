
all: DungeonBuilder.jar

DungeonBuilder.jar: *.java plugin.yml CommandMap.xml
	javac -d . -Xlint:unchecked -cp "server/lib/craftbukkit-1.1-R1.jar:server/plugins/Permissions/Permissions.jar:server/plugins/Heroes.jar:." -Xlint:deprecation *.java
	jar -cf DungeonBuilder.jar net plugin.yml com CommandMap.xml

clean:
	rm -f DungeonBuilder.jar
	rm -rf net

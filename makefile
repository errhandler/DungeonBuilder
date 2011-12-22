
all: DungeonBuilder.jar

DungeonBuilder.jar: *.java plugin.yml CommandMap.xml
	javac -d . -cp "server/lib/craftbukkit-1.0.1-R1.jar:server/plugins/Permissions/Permissions.jar:." -Xlint:deprecation *.java
	jar -cf DungeonBuilder.jar net plugin.yml com CommandMap.xml

clean:
	rm -f DungeonBuilder.jar
	rm -rf net
